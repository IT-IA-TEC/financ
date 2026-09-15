package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.config.Avisos;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Cobranca;
import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.ItemDaCobranca;
import br.com.itia.financeiro.dominio.ItemExecutado;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.OrigemDaCobranca;
import br.com.itia.financeiro.dominio.ServicoRealizado;
import br.com.itia.financeiro.dominio.SituacaoDaCobranca;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.TratamentoDoAtendimento;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.CobrancaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As cobranças de pacotes e de serviços.
 *
 * As regras que este serviço protege:
 *   1. Serviço incluído no pacote fica vinculado à contratação e não gera
 *      cobrança adicional.
 *   2. Gratuidade fica registrada, com valor final zero, sem virar valor a
 *      receber.
 *   3. Cobrança aprovada vira título no financeiro. O pagamento é lido do
 *      título, nunca digitado aqui.
 *   4. Desconto exige justificativa e fica com o nome de quem aprovou.
 */
@Service
public class Cobrancas {

    private static final DateTimeFormatter REFERENCIA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final CobrancaRepositorio cobrancas;
    private final ClienteRepositorio unidades;
    private final FinanceiroServico financeiro;
    private final IntegracaoServico integracoes;
    private final FechamentoDoMes fechamento;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public Cobrancas(CobrancaRepositorio cobrancas,
                     ClienteRepositorio unidades, FinanceiroServico financeiro,
                     IntegracaoServico integracoes, FechamentoDoMes fechamento,
                     ContextoEmpresa contexto, Avisos avisos) {
        this.cobrancas = cobrancas;
        this.unidades = unidades;
        this.financeiro = financeiro;
        this.integracoes = integracoes;
        this.fechamento = fechamento;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------------ lista

    public List<Cobranca> todas() {
        return cobrancas.findByEmpresaIdOrderByCriadoEmDesc(contexto.exigirEmpresaId());
    }

    public List<Cobranca> lista(String origem, String situacao, UUID pagadorId,
                                LocalDate de, LocalDate ate) {
        return todas().stream()
                .filter(c -> origem == null || origem.isBlank()
                        || c.getOrigem().name().equals(origem))
                .filter(c -> situacao == null || situacao.isBlank()
                        || c.getSituacao().name().equals(situacao))
                .filter(c -> pagadorId == null || c.getPagador().getId().equals(pagadorId))
                .filter(c -> de == null || (c.getVencimento() != null
                        && !c.getVencimento().isBefore(de)))
                .filter(c -> ate == null || (c.getVencimento() != null
                        && !c.getVencimento().isAfter(ate)))
                .toList();
    }

    public Cobranca cobranca(UUID id) {
        return cobrancas.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cobrança não encontrada nesta empresa."));
    }

    public List<Cobranca> daPessoa(UUID pagadorId) {
        return cobrancas.findByPagadorIdOrderByCriadoEmDesc(pagadorId);
    }

    /** Os números que a tela mostra em cima: cobrado, recebido e pendente. */
    public Resumo resumo(List<Cobranca> lista) {
        BigDecimal cobrado = BigDecimal.ZERO;
        BigDecimal recebido = BigDecimal.ZERO;
        BigDecimal pendente = BigDecimal.ZERO;
        BigDecimal gratuito = BigDecimal.ZERO;
        int aguardando = 0;

        for (Cobranca c : lista) {
            if (c.getSituacao() == SituacaoDaCobranca.CANCELADA) {
                continue;
            }
            if (c.getSituacao() == SituacaoDaCobranca.SEM_VALOR) {
                gratuito = gratuito.add(c.getValorOriginal());
                continue;
            }
            cobrado = cobrado.add(c.getValorFinal());
            if (c.estaPaga()) {
                recebido = recebido.add(c.getValorFinal());
            } else {
                pendente = pendente.add(c.getValorFinal());
            }
            if (c.getSituacao() == SituacaoDaCobranca.PENDENTE) {
                aguardando = aguardando + 1;
            }
        }
        return new Resumo(cobrado, recebido, pendente, gratuito, aguardando);
    }

    // ------------------------------------------------- vinda do atendimento

    /**
     * Acerta a cobrança de um serviço realizado.
     *
     * Chamado toda vez que o atendimento é gravado. Incluído no pacote não
     * gera cobrança; gratuito fica registrado sem valor; o resto vira cobrança
     * pendente de conferência.
     */
    @Transactional
    public void acertarPeloAtendimento(ServicoRealizado atendimento) {
        Optional<Cobranca> jaExiste = cobrancas.findByRealizadoId(atendimento.getId());

        if (atendimento.getTratamento() == TratamentoDoAtendimento.INCLUIDO_NO_PACOTE) {
            // Ja esta pago pelo pacote: a ligacao com a contratacao fica no
            // proprio atendimento, e cobranca nenhuma nasce daqui.
            jaExiste.filter(c -> c.getSituacao().aindaEditavel()).ifPresent(Cobranca::cancelar);
            return;
        }

        Cobranca cobranca = jaExiste.orElseGet(() -> nova(atendimento));
        if (!cobranca.getSituacao().aindaEditavel()) {
            // Ja foi aprovada ou enviada: ninguem mexe no valor por tras.
            return;
        }

        cobranca.ajustarDados(textoDo(atendimento), cobranca.getVencimento() == null
                        ? atendimento.getRealizadoEm().plusDays(10) : cobranca.getVencimento(),
                atendimento.getUnidade());
        cobranca.ajustarPeriodo(atendimento.getRealizadoEm(), atendimento.getRealizadoEm(),
                atendimento.getRealizadoEm().format(REFERENCIA));
        cobranca.vincularOrigem(null, atendimento);

        cobranca.limparItens();
        for (ItemExecutado executado : atendimento.getItens()) {
            cobranca.receber(new ItemDaCobranca(cobranca, executado.getItem().getNome(),
                    executado.getQuantidade(), executado.getItem().getValor(),
                    executado.getItem()));
        }

        if (atendimento.getTratamento() == TratamentoDoAtendimento.GRATUITO) {
            cobranca.ajustarValores(valorDe(atendimento), valorDe(atendimento),
                    atendimento.getObservacao());
            cobranca.marcarSemValor(atendimento.getObservacao());
        } else {
            cobranca.ajustarValores(valorDe(atendimento), atendimento.getDesconto(),
                    atendimento.getObservacao());
        }
        cobrancas.save(cobranca);
        avisos.avisar(cobranca.getEmpresa().getId(), "COBRANCA_ATUALIZADA");
    }

    private Cobranca nova(ServicoRealizado atendimento) {
        return new Cobranca(atendimento.getEmpresa(), OrigemDaCobranca.SERVICO,
                atendimento.getPagador(), textoDo(atendimento), contexto.autor());
    }

    private String textoDo(ServicoRealizado atendimento) {
        return atendimento.getServico().getNome() + " em "
                + atendimento.getRealizadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private BigDecimal valorDe(ServicoRealizado atendimento) {
        if (atendimento.getValorCobrado() != null) {
            return atendimento.getValorCobrado();
        }
        BigDecimal preco = atendimento.getServico().getTotalPadrao();
        return preco == null ? BigDecimal.ZERO : preco.multiply(atendimento.getQuantidade());
    }

    /** Quando o atendimento é cancelado, a cobrança pendente cai junto. */
    @Transactional
    public void cancelarDoAtendimento(UUID realizadoId) {
        cobrancas.findByRealizadoId(realizadoId)
                .filter(c -> c.getSituacao().aindaEditavel())
                .ifPresent(Cobranca::cancelar);
    }

    // ------------------------------------------------------------- aprovação

    @Transactional
    public void ajustarValor(UUID id, BigDecimal valorOriginal, BigDecimal desconto,
                             String justificativa, LocalDate vencimento) {
        Cobranca cobranca = cobranca(id);
        fechamento.exigirAberta(cobranca.getReferencia());
        if (!cobranca.getSituacao().aindaEditavel()) {
            throw new IllegalStateException(
                    "Esta cobrança já foi aprovada. Cancele e gere outra para mudar o valor.");
        }
        if (desconto != null && desconto.signum() > 0
                && (justificativa == null || justificativa.isBlank())) {
            throw new IllegalArgumentException("Desconto exige justificativa.");
        }
        cobranca.ajustarValores(valorOriginal, desconto, justificativa);
        cobranca.ajustarDados(cobranca.getDescricao(), vencimento, cobranca.getUnidade());
    }

    /** Aprova e lança o título no financeiro. */
    @Transactional
    public void aprovar(UUID id) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.GESTOR);
        Cobranca cobranca = cobranca(id);
        fechamento.exigirAberta(cobranca.getReferencia());
        if (cobranca.getSituacao() == SituacaoDaCobranca.SEM_VALOR) {
            throw new IllegalStateException(
                    "Gratuidade fica registrada, sem valor a receber. Não há o que aprovar.");
        }
        if (!cobranca.getSituacao().aindaEditavel()) {
            throw new IllegalStateException("Esta cobrança já foi aprovada.");
        }
        if (cobranca.getValorFinal().signum() <= 0) {
            throw new IllegalStateException("Cobrança sem valor. Confira antes de aprovar.");
        }

        ClienteEspelho unidade = unidadeParaOTitulo(cobranca);
        LocalDate vencimento = cobranca.getVencimento() == null
                ? LocalDate.now().plusDays(10) : cobranca.getVencimento();
        LocalDate competencia = cobranca.getPeriodoInicio() == null
                ? LocalDate.now().withDayOfMonth(1) : cobranca.getPeriodoInicio();

        Titulo titulo = financeiro.lancarTitulo(unidade.getId(), competencia,
                cobranca.getDescricao(), cobranca.getValorFinal(), vencimento);

        cobranca.aprovar(contexto.autor());
        cobranca.guardarTitulo(titulo);
        avisos.avisar(cobranca.getEmpresa().getId(), "COBRANCA_APROVADA");
    }

    @Transactional
    public void cancelar(UUID id) {
        cobranca(id).cancelar();
    }

    /**
     * Marca como paga o que o título já quitou.
     *
     * A situação do pagamento mora no título. Aqui só copiamos o resultado
     * para a lista de cobranças não precisar abrir cada título.
     */
    @Transactional
    public void conferirPagamentos() {
        for (Cobranca cobranca : todas()) {
            if (cobranca.estaPaga() && cobranca.getSituacao() != SituacaoDaCobranca.PAGA) {
                cobranca.marcarPaga();
                cobrancas.save(cobranca);
            }
        }
    }

    // ------------------------------------------------------- envio ao Conexa

    /** A integração de sistema de cobrança configurada e ligada, se houver. */
    public Optional<Integracao> sistemaDeCobranca() {
        return integracoes.daEmpresa().stream()
                .filter(i -> i.getProvedor() == Conector.SISTEMA_DE_COBRANCA)
                .filter(Integracao::isAtiva)
                .findFirst();
    }

    @Transactional
    public void enviar(UUID id) {
        Cobranca cobranca = cobranca(id);
        if (cobranca.getSituacao() != SituacaoDaCobranca.APROVADA) {
            throw new IllegalStateException("Aprove a cobrança antes de enviar.");
        }
        Integracao integracao = sistemaDeCobranca().orElseThrow(() -> new IllegalStateException(
                "Nenhum sistema de cobrança ligado. Configure em Integrações, com o tipo"
                        + " Sistema de cobrança, e crie a operação de criar cobrança."));

        OperacaoIntegracao operacao = integracoes.operacoesDe(integracao.getId()).stream()
                .filter(o -> o.getNome() != null
                        && o.getNome().toLowerCase().contains("cobran"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A integração não tem operação de cobrança. Crie uma operação com"
                                + " a palavra cobrança no nome."));

        try {
            String resposta = integracoes.executar(integracao.getId(), operacao.getId(),
                    corpoDe(cobranca));
            cobranca.marcarEnviada(integracao.getId(), identificadorDe(resposta));
        } catch (RuntimeException falha) {
            cobranca.anotarErro(falha.getMessage());
            throw new IllegalStateException("Não consegui enviar: " + falha.getMessage());
        }
    }

    private String corpoDe(Cobranca cobranca) {
        return "{\"cliente\": \"" + escapar(cobranca.getPagador().getNome()) + "\","
                + "\"documento\": \"" + escapar(cobranca.getPagador().getCpf()) + "\","
                + "\"descricao\": \"" + escapar(cobranca.getDescricao()) + "\","
                + "\"valor\": " + cobranca.getValorFinal() + ","
                + "\"vencimento\": \"" + cobranca.getVencimento() + "\","
                + "\"referencia\": \"" + escapar(cobranca.getReferencia()) + "\","
                + "\"titulo\": \"" + (cobranca.getTitulo() == null ? ""
                : cobranca.getTitulo().getIdentificadorPix()) + "\"}";
    }

    /** Pega o id que o outro sistema devolveu, quando ele devolve um. */
    private String identificadorDe(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            return null;
        }
        java.util.regex.Matcher achou = java.util.regex.Pattern
                .compile("\"(id|identificador|charge_id)\"\\s*:\\s*\"?([^\",}]+)")
                .matcher(resposta);
        if (achou.find()) {
            return achou.group(2).trim();
        }
        return resposta.length() > 180 ? resposta.substring(0, 180) : resposta;
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\"", "'");
    }

    // ------------------------------------------------------------------ apoio

    /** O título é lançado para uma unidade. Sem unidade, não há título. */
    private ClienteEspelho unidadeParaOTitulo(Cobranca cobranca) {
        if (cobranca.getUnidade() != null) {
            return cobranca.getUnidade();
        }
        return unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId())
                .stream()
                .filter(u -> u.getPagador() != null
                        && u.getPagador().getId().equals(cobranca.getPagador().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Este cliente não tem unidade cadastrada, e o título é lançado para"
                                + " uma unidade. Cadastre a unidade antes de aprovar."));
    }

    /** Os totais que a tela mostra em cima da lista. */
    public record Resumo(BigDecimal cobrado, BigDecimal recebido, BigDecimal pendente,
                         BigDecimal gratuito, int aguardando) {
    }
}
