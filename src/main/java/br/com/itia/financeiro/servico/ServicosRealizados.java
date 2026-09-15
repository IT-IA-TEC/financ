package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.config.Avisos;
import br.com.itia.financeiro.dominio.AbrangenciaDoPacote;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.ContratacaoDePacote;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.ItemDoPacote;
import br.com.itia.financeiro.dominio.ItemExecutado;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.ServicoRealizado;
import br.com.itia.financeiro.dominio.SituacaoDaContratacao;
import br.com.itia.financeiro.dominio.TratamentoDoAtendimento;
import br.com.itia.financeiro.dominio.TratamentoDoExcedente;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.ContratacaoDePacoteRepositorio;
import br.com.itia.financeiro.repositorio.DocumentoRepositorio;
import br.com.itia.financeiro.repositorio.ItemDeServicoRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import br.com.itia.financeiro.repositorio.ServicoRealizadoRepositorio;
import br.com.itia.financeiro.repositorio.ServicoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Os serviços realizados: o que a equipe fez para o cliente.
 *
 * A parte que interessa aqui é a conferência do pacote. Antes de gravar, o
 * sistema olha os pacotes que o cliente tem valendo na data, vê se aquele
 * serviço está incluído, conta quanto já foi usado na janela do limite e diz
 * se o atendimento entra no pacote ou vira cobrança.
 */
@Service
public class ServicosRealizados {

    private final ServicoRealizadoRepositorio realizados;
    private final ContratacaoDePacoteRepositorio contratacoes;
    private final ServicoRepositorio servicos;
    private final ItemDeServicoRepositorio itensDeServico;
    private final PagadorRepositorio pagadores;
    private final ClienteRepositorio unidades;
    private final DocumentoRepositorio documentos;
    private final Cobrancas cobrancas;
    private final ContextoEmpresa contexto;
    private final Avisos avisos;

    public ServicosRealizados(ServicoRealizadoRepositorio realizados,
                              ContratacaoDePacoteRepositorio contratacoes,
                              ServicoRepositorio servicos,
                              ItemDeServicoRepositorio itensDeServico,
                              PagadorRepositorio pagadores, ClienteRepositorio unidades,
                              DocumentoRepositorio documentos, Cobrancas cobrancas,
                              ContextoEmpresa contexto, Avisos avisos) {
        this.realizados = realizados;
        this.contratacoes = contratacoes;
        this.servicos = servicos;
        this.itensDeServico = itensDeServico;
        this.pagadores = pagadores;
        this.unidades = unidades;
        this.documentos = documentos;
        this.cobrancas = cobrancas;
        this.contexto = contexto;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------------ lista

    public List<ServicoRealizado> todos() {
        return realizados.findByEmpresaIdOrderByRealizadoEmDescCriadoEmDesc(
                contexto.exigirEmpresaId());
    }

    /** A lista já filtrada pelos filtros da tela. */
    public List<ServicoRealizado> lista(UUID pagadorId, UUID servicoId, String tratamento,
                                        LocalDate de, LocalDate ate) {
        return todos().stream()
                .filter(r -> pagadorId == null || r.getPagador().getId().equals(pagadorId))
                .filter(r -> servicoId == null || r.getServico().getId().equals(servicoId))
                .filter(r -> tratamento == null || tratamento.isBlank()
                        || r.getTratamento().name().equals(tratamento))
                .filter(r -> de == null || !r.getRealizadoEm().isBefore(de))
                .filter(r -> ate == null || !r.getRealizadoEm().isAfter(ate))
                .toList();
    }

    public ServicoRealizado atendimento(UUID id) {
        return realizados.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Atendimento não encontrado nesta empresa."));
    }

    public List<Documento> comprovantesDe(UUID realizadoId) {
        return documentos.findByRealizadoIdOrderByAnexadoEmDesc(realizadoId);
    }

    public List<ServicoRealizado> atendimentosDaPessoa(UUID pagadorId) {
        return realizados.findByPagadorIdOrderByRealizadoEmDesc(pagadorId);
    }

    // -------------------------------------------------------------- registro

    @Transactional
    public ServicoRealizado registrar(UUID pagadorId, UUID unidadeId, UUID servicoId,
                                      LocalDate quando, BigDecimal quantidade, String responsavel,
                                      String tratamentoEscolhido, BigDecimal valorCobrado,
                                      BigDecimal desconto, String observacao,
                                      List<UUID> itensExecutados) {
        Pagador pagador = pagador(pagadorId);
        Servico servico = servico(servicoId);
        LocalDate dia = quando == null ? LocalDate.now() : quando;
        BigDecimal quantos = quantidade == null ? BigDecimal.ONE : quantidade;

        Cobertura cobertura = conferir(pagadorId, servicoId, dia, quantos, null);
        TratamentoDoAtendimento tratamento = decidir(tratamentoEscolhido, cobertura);

        ServicoRealizado novo = new ServicoRealizado(contexto.exigirEmpresa(), pagador, servico,
                contexto.autor());
        novo.ajustar(pagador, unidade(unidadeId), servico, cobertura.getContratacao(), dia,
                quantos, responsavel, tratamento,
                valorCobrado == null ? valorSugerido(servico, quantos) : valorCobrado,
                desconto, observacao);

        if (itensExecutados != null) {
            for (UUID itemId : itensExecutados) {
                ItemDeServico item = itensDeServico.findByIdAndServicoId(itemId, servicoId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Item não encontrado dentro do serviço escolhido."));
                // O item entra pelo atendimento, que se encarrega de gravar.
                novo.receber(new ItemExecutado(novo, item, BigDecimal.ONE, null));
            }
        }

        realizados.saveAndFlush(novo);
        // A cobranca nasce do atendimento: incluido no pacote nao gera nada,
        // gratuito fica registrado sem valor, o resto vira cobranca pendente.
        cobrancas.acertarPeloAtendimento(novo);
        avisos.avisar(novo.getEmpresa().getId(), "SERVICO_REALIZADO");
        return novo;
    }

    @Transactional
    public void salvar(UUID id, UUID pagadorId, UUID unidadeId, UUID servicoId, LocalDate quando,
                       BigDecimal quantidade, String responsavel, String tratamentoEscolhido,
                       BigDecimal valorCobrado, BigDecimal desconto, String observacao) {
        ServicoRealizado atendimento = atendimento(id);
        LocalDate dia = quando == null ? atendimento.getRealizadoEm() : quando;
        BigDecimal quantos = quantidade == null ? BigDecimal.ONE : quantidade;

        Cobertura cobertura = conferir(pagadorId, servicoId, dia, quantos, id);
        TratamentoDoAtendimento tratamento = decidir(tratamentoEscolhido, cobertura);

        atendimento.ajustar(pagador(pagadorId), unidade(unidadeId), servico(servicoId),
                cobertura.getContratacao(), dia, quantos, responsavel, tratamento,
                valorCobrado, desconto, observacao);
        cobrancas.acertarPeloAtendimento(atendimento);
    }

    @Transactional
    public void cancelar(UUID id) {
        atendimento(id).cancelar();
        cobrancas.cancelarDoAtendimento(id);
    }

    @Transactional
    public void reabrir(UUID id) {
        ServicoRealizado atendimento = atendimento(id);
        atendimento.reabrir();
        cobrancas.acertarPeloAtendimento(atendimento);
    }

    @Transactional
    public void adicionarItem(UUID id, UUID itemId, BigDecimal quantidade, String observacao) {
        ServicoRealizado atendimento = atendimento(id);
        ItemDeServico item = itensDeServico
                .findByIdAndServicoId(itemId, atendimento.getServico().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item não encontrado dentro do serviço deste atendimento."));
        atendimento.receber(new ItemExecutado(atendimento, item, quantidade, observacao));
        realizados.saveAndFlush(atendimento);
        cobrancas.acertarPeloAtendimento(atendimento);
    }

    /** Liga um arquivo já anexado a este atendimento. */
    @Transactional
    public void guardarComprovante(UUID realizadoId, Documento documento) {
        atendimento(realizadoId);
        documento.vincularAoAtendimento(realizadoId);
        documentos.save(documento);
    }

    // ------------------------------------------------------ conferir o pacote

    /**
     * Olha os pacotes do cliente e diz se este atendimento entra no pacote.
     *
     * Nada é gravado aqui. A tela usa este mesmo cálculo para avisar antes,
     * e o registro usa de novo na hora de salvar.
     */
    public Cobertura conferir(UUID pagadorId, UUID servicoId, LocalDate dia,
                              BigDecimal quantidade, UUID ignorarAtendimento) {
        LocalDate quando = dia == null ? LocalDate.now() : dia;
        BigDecimal quantos = quantidade == null ? BigDecimal.ONE : quantidade;

        for (ContratacaoDePacote contratacao : contratacoes
                .findByPagadorIdOrderByCriadoEmDesc(pagadorId)) {
            if (contratacao.getSituacao() != SituacaoDaContratacao.ATIVA
                    || !contratacao.estaVigenteEm(quando)) {
                continue;
            }
            for (ItemDoPacote linha : contratacao.getPacote().getComposicao()) {
                if (!linha.getServico().getId().equals(servicoId)) {
                    continue;
                }
                if (linha.getAbrangencia() == AbrangenciaDoPacote.ITEM_ESCOLHIDO) {
                    // O pacote cobre so um item deste servico, e nao o servico
                    // inteiro. Quem decide nesse caso e a pessoa, na tela.
                    return Cobertura.parcial(contratacao, linha);
                }
                if (linha.isIlimitado()) {
                    return Cobertura.ilimitado(contratacao, linha);
                }
                BigDecimal incluido = linha.getQuantidadeIncluida();
                if (incluido == null) {
                    return Cobertura.semLimiteDefinido(contratacao, linha);
                }
                Janela janela = janelaDoLimite(contratacao, linha, quando);
                BigDecimal usado = quantoJaFoiUsado(pagadorId, servicoId, janela,
                        ignorarAtendimento);
                BigDecimal saldo = incluido.subtract(usado);
                return Cobertura.comLimite(contratacao, linha, incluido, usado, saldo,
                        quantos, janela);
            }
        }
        return Cobertura.semPacote();
    }

    private BigDecimal quantoJaFoiUsado(UUID pagadorId, UUID servicoId, Janela janela,
                                        UUID ignorar) {
        return realizados
                .findByPagadorIdAndServicoIdAndRealizadoEmBetween(pagadorId, servicoId,
                        janela.de(), janela.ate())
                .stream()
                .filter(r -> ignorar == null || !r.getId().equals(ignorar))
                .filter(ServicoRealizado::consomeLimite)
                .map(ServicoRealizado::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** De quando até quando vale a quantidade incluída, para esta data. */
    private Janela janelaDoLimite(ContratacaoDePacote contratacao, ItemDoPacote linha,
                                  LocalDate dia) {
        return switch (linha.getPeriodoLimite()) {
            case POR_MES -> new Janela(dia.withDayOfMonth(1),
                    dia.withDayOfMonth(dia.lengthOfMonth()));
            case POR_ANO -> new Janela(dia.withDayOfYear(1),
                    dia.withDayOfYear(dia.lengthOfYear()));
            case TOTAL_DA_VIGENCIA -> new Janela(
                    contratacao.getInicio() == null ? dia.minusYears(50) : contratacao.getInicio(),
                    contratacao.getFim() == null ? dia.plusYears(50) : contratacao.getFim());
            case POR_PERIODO_DO_PACOTE -> periodoDoPacote(contratacao, dia);
        };
    }

    /** A janela que acompanha a periodicidade do pacote, contada do início. */
    private Janela periodoDoPacote(ContratacaoDePacote contratacao, LocalDate dia) {
        int meses = mesesDe(contratacao.getPacote().getPeriodicidade());
        LocalDate inicio = contratacao.getInicio() == null
                ? dia.withDayOfMonth(1) : contratacao.getInicio();
        if (meses == 0 || dia.isBefore(inicio)) {
            // Pacote avulso ou com intervalo proprio: conta a vigencia inteira.
            return new Janela(inicio, contratacao.getFim() == null
                    ? dia.plusYears(50) : contratacao.getFim());
        }
        long periodosPassados = ChronoUnit.MONTHS.between(inicio, dia) / meses;
        LocalDate de = inicio.plusMonths(periodosPassados * meses);
        return new Janela(de, de.plusMonths(meses).minusDays(1));
    }

    private int mesesDe(Periodicidade periodicidade) {
        return switch (periodicidade) {
            case MENSAL -> 1;
            case TRIMESTRAL -> 3;
            case SEMESTRAL -> 6;
            case ANUAL -> 12;
            case AVULSA, OUTRA -> 0;
        };
    }

    /** O que a pessoa escolheu na tela, ou o que o pacote manda. */
    private TratamentoDoAtendimento decidir(String escolhido, Cobertura cobertura) {
        if (escolhido != null && !escolhido.isBlank() && !"PELO_PACOTE".equals(escolhido)) {
            return TratamentoDoAtendimento.valueOf(escolhido);
        }
        return cobertura.getTratamentoSugerido();
    }

    private BigDecimal valorSugerido(Servico servico, BigDecimal quantidade) {
        BigDecimal preco = servico.getTotalPadrao();
        return preco == null ? null : preco.multiply(quantidade);
    }

    // ------------------------------------------------------------------ apoio

    public List<Servico> servicosDisponiveis() {
        return servicos.findByEmpresaIdOrderByCodigo(contexto.exigirEmpresaId()).stream()
                .filter(Servico::isAtivo)
                .toList();
    }

    public List<Pagador> clientes() {
        return pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<ClienteEspelho> unidadesDaEmpresa() {
        return unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId());
    }

    private Servico servico(UUID id) {
        return servicos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));
    }

    private Pagador pagador(UUID id) {
        return pagadores.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    private ClienteEspelho unidade(UUID id) {
        if (id == null) {
            return null;
        }
        return unidades.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada."));
    }

    /** A janela de tempo em que a quantidade incluída vale. */
    public record Janela(LocalDate de, LocalDate ate) {
    }

    /**
     * A resposta da conferência: o que o pacote do cliente cobre, e o que
     * sobra para cobrar.
     */
    public static class Cobertura {

        private ContratacaoDePacote contratacao;
        private ItemDoPacote linha;
        private boolean temPacote;
        private boolean ilimitado;
        private BigDecimal incluido;
        private BigDecimal jaUsado;
        private BigDecimal saldo;
        private Janela janela;
        private TratamentoDoAtendimento tratamentoSugerido =
                TratamentoDoAtendimento.COBRADO_A_PARTE;
        private String explicacao = "Este cliente não tem pacote que cubra este serviço.";
        private boolean precisaDeAvaliacao;

        static Cobertura semPacote() {
            return new Cobertura();
        }

        static Cobertura ilimitado(ContratacaoDePacote contratacao, ItemDoPacote linha) {
            Cobertura c = base(contratacao, linha);
            c.ilimitado = true;
            c.tratamentoSugerido = TratamentoDoAtendimento.INCLUIDO_NO_PACOTE;
            c.explicacao = "Incluído no pacote " + contratacao.getPacote().getNome()
                    + ", com utilização ilimitada.";
            return c;
        }

        static Cobertura semLimiteDefinido(ContratacaoDePacote contratacao, ItemDoPacote linha) {
            Cobertura c = base(contratacao, linha);
            c.tratamentoSugerido = TratamentoDoAtendimento.INCLUIDO_NO_PACOTE;
            c.explicacao = "O pacote " + contratacao.getPacote().getNome()
                    + " inclui este serviço, mas ninguém definiu a quantidade."
                    + " Confira o pacote.";
            return c;
        }

        static Cobertura parcial(ContratacaoDePacote contratacao, ItemDoPacote linha) {
            Cobertura c = base(contratacao, linha);
            c.tratamentoSugerido = TratamentoDoAtendimento.COBRADO_A_PARTE;
            c.explicacao = "O pacote " + contratacao.getPacote().getNome()
                    + " cobre apenas o item " + (linha.getItem() == null ? "escolhido"
                    : linha.getItem().getNome()) + " deste serviço, e não o serviço inteiro.";
            return c;
        }

        static Cobertura comLimite(ContratacaoDePacote contratacao, ItemDoPacote linha,
                                   BigDecimal incluido, BigDecimal usado, BigDecimal saldo,
                                   BigDecimal pedido, Janela janela) {
            Cobertura c = base(contratacao, linha);
            c.incluido = incluido;
            c.jaUsado = usado;
            c.saldo = saldo;
            c.janela = janela;

            boolean cabe = saldo.compareTo(pedido) >= 0;
            if (cabe) {
                c.tratamentoSugerido = TratamentoDoAtendimento.INCLUIDO_NO_PACOTE;
                c.explicacao = "Incluído no pacote " + contratacao.getPacote().getNome()
                        + ". Usado " + texto(usado) + " de " + texto(incluido)
                        + " no período, sobra " + texto(saldo) + ".";
            } else {
                c.tratamentoSugerido = TratamentoDoAtendimento.COBRADO_A_PARTE;
                c.precisaDeAvaliacao =
                        linha.getTratamentoExcedente() == TratamentoDoExcedente.EXIGE_AVALIACAO;
                c.explicacao = "Passou do limite do pacote "
                        + contratacao.getPacote().getNome() + ". O cliente tinha "
                        + texto(incluido) + " no período e já usou " + texto(usado) + ". "
                        + (c.precisaDeAvaliacao
                        ? "O pacote exige avaliação antes de cobrar."
                        : "O excedente é cobrado pelo preço do catálogo.");
            }
            return c;
        }

        private static Cobertura base(ContratacaoDePacote contratacao, ItemDoPacote linha) {
            Cobertura c = new Cobertura();
            c.contratacao = contratacao;
            c.linha = linha;
            c.temPacote = true;
            return c;
        }

        private static String texto(BigDecimal numero) {
            return numero == null ? "0" : numero.stripTrailingZeros().toPlainString();
        }

        public ContratacaoDePacote getContratacao() {
            return contratacao;
        }

        public ItemDoPacote getLinha() {
            return linha;
        }

        public boolean isTemPacote() {
            return temPacote;
        }

        public boolean isIlimitado() {
            return ilimitado;
        }

        public BigDecimal getIncluido() {
            return incluido;
        }

        public BigDecimal getJaUsado() {
            return jaUsado;
        }

        public BigDecimal getSaldo() {
            return saldo;
        }

        public Janela getJanela() {
            return janela;
        }

        public TratamentoDoAtendimento getTratamentoSugerido() {
            return tratamentoSugerido;
        }

        public String getTratamentoSugeridoNome() {
            return tratamentoSugerido.name();
        }

        public String getExplicacao() {
            return explicacao;
        }

        public boolean isPrecisaDeAvaliacao() {
            return precisaDeAvaliacao;
        }

        public String getNomeDoPacote() {
            return contratacao == null ? null : contratacao.getPacote().getNome();
        }
    }

    /** Usado só para montar listas em memória sem repetir código. */
    static List<UUID> vazioQuandoNulo(List<UUID> lista) {
        return lista == null ? new ArrayList<>() : lista;
    }
}
