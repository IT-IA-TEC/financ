package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.AnaliseDeCredito;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Consentimento;
import br.com.itia.financeiro.dominio.Contato;
import br.com.itia.financeiro.dominio.Contrato;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Endereco;
import br.com.itia.financeiro.dominio.Etiqueta;
import br.com.itia.financeiro.dominio.Fonte;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.PreferenciaDeCobranca;
import br.com.itia.financeiro.dominio.Restricao;
import br.com.itia.financeiro.dominio.ServicoDoContrato;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.dominio.VinculoDeEtiqueta;
import br.com.itia.financeiro.repositorio.AnaliseDeCreditoRepositorio;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.ConsentimentoRepositorio;
import br.com.itia.financeiro.repositorio.ContatoRepositorio;
import br.com.itia.financeiro.repositorio.ContratoRepositorio;
import br.com.itia.financeiro.repositorio.DocumentoRepositorio;
import br.com.itia.financeiro.repositorio.EnderecoRepositorio;
import br.com.itia.financeiro.repositorio.EtiquetaRepositorio;
import br.com.itia.financeiro.repositorio.InteracaoRepositorio;
import br.com.itia.financeiro.repositorio.PreferenciaDeCobrancaRepositorio;
import br.com.itia.financeiro.repositorio.RestricaoRepositorio;
import br.com.itia.financeiro.repositorio.ServicoDoContratoRepositorio;
import br.com.itia.financeiro.repositorio.ServicoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import br.com.itia.financeiro.repositorio.VinculoDeEtiquetaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Monta o perfil do cliente.
 *
 * Reúne o que está espalhado em várias tabelas e calcula na hora o que é
 * derivado: posição em aberto, aging, atraso médio, comportamento de pagamento
 * e limite disponível. Nada calculado é gravado, para não envelhecer em
 * silêncio quando o dado de origem mudar.
 */
@Service
public class PerfilServico {

    private final ClienteRepositorio unidades;
    private final TituloRepositorio titulos;
    private final ContatoRepositorio contatos;
    private final EnderecoRepositorio enderecos;
    private final PreferenciaDeCobrancaRepositorio preferencias;
    private final ContratoRepositorio contratos;
    private final ServicoDoContratoRepositorio servicosDoContrato;
    private final ServicoRepositorio servicos;
    private final AnaliseDeCreditoRepositorio creditos;
    private final RestricaoRepositorio restricoes;
    private final InteracaoRepositorio interacoes;
    private final DocumentoRepositorio documentos;
    private final ConsentimentoRepositorio consentimentos;
    private final EtiquetaRepositorio etiquetas;
    private final VinculoDeEtiquetaRepositorio vinculos;
    private final CarteiraServico carteira;
    private final ContextoEmpresa contexto;

    public PerfilServico(ClienteRepositorio unidades, TituloRepositorio titulos,
                         ContatoRepositorio contatos, EnderecoRepositorio enderecos,
                         PreferenciaDeCobrancaRepositorio preferencias,
                         ContratoRepositorio contratos,
                         ServicoDoContratoRepositorio servicosDoContrato,
                         ServicoRepositorio servicos,
                         AnaliseDeCreditoRepositorio creditos, RestricaoRepositorio restricoes,
                         InteracaoRepositorio interacoes, DocumentoRepositorio documentos,
                         ConsentimentoRepositorio consentimentos, EtiquetaRepositorio etiquetas,
                         VinculoDeEtiquetaRepositorio vinculos, CarteiraServico carteira,
                         ContextoEmpresa contexto) {
        this.unidades = unidades;
        this.titulos = titulos;
        this.contatos = contatos;
        this.enderecos = enderecos;
        this.preferencias = preferencias;
        this.contratos = contratos;
        this.servicosDoContrato = servicosDoContrato;
        this.servicos = servicos;
        this.creditos = creditos;
        this.restricoes = restricoes;
        this.interacoes = interacoes;
        this.documentos = documentos;
        this.consentimentos = consentimentos;
        this.etiquetas = etiquetas;
        this.vinculos = vinculos;
        this.carteira = carteira;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------ o resumo

    /** Os números da pessoa, somando todas as unidades dela. */
    public Posicao posicaoDa(UUID pagadorId) {
        LocalDate hoje = LocalDate.now();
        List<Titulo> todos = new ArrayList<>();
        for (ClienteEspelho u : unidadesDe(pagadorId)) {
            todos.addAll(titulos.findByClienteIdOrderByVencimentoDesc(u.getId()));
        }
        return new Posicao(todos, hoje);
    }

    public List<ClienteEspelho> unidadesDe(UUID pagadorId) {
        return unidades.findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(contexto.exigirEmpresaId())
                .stream()
                .filter(u -> u.getPagador() != null && u.getPagador().getId().equals(pagadorId))
                .toList();
    }

    public ClienteEspelho unidade(UUID unidadeId) {
        return unidades.findByIdAndEmpresaId(unidadeId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unidade não encontrada nesta empresa."));
    }

    // ----------------------------------------------------------- contatos

    public List<Contato> contatosDe(UUID pagadorId) {
        List<Contato> lista = contatos.findByPagadorIdOrderByPrioridade(pagadorId);
        lista.forEach(c -> c.receberPapeis(etiquetasDe("CONTATO", c.getId())));
        return lista;
    }

    public List<Contato> contatosDaUnidade(UUID unidadeId) {
        List<Contato> lista = contatos.findByUnidadeIdOrderByPrioridade(unidadeId);
        lista.forEach(c -> c.receberPapeis(etiquetasDe("CONTATO", c.getId())));
        return lista;
    }

    @Transactional
    public Contato salvarContato(UUID contatoId, UUID pagadorId, UUID unidadeId, String nome,
                                 String telefone, String whatsapp, String email, int prioridade,
                                 boolean aceitaCobranca, String melhorHorario, String observacao,
                                 boolean ativo, List<UUID> papeis) {
        UUID empresaId = contexto.exigirEmpresaId();
        Contato contato = contatoId == null
                ? new Contato(empresaId, pagadorId, unidadeId, nome)
                : contatos.findByIdAndEmpresaId(contatoId, empresaId)
                        .orElseThrow(() -> new IllegalArgumentException("Contato não encontrado."));
        contato.ajustar(nome, telefone, whatsapp, email, prioridade, aceitaCobranca,
                melhorHorario, observacao, ativo);
        contatos.save(contato);
        trocarEtiquetas("CONTATO", contato.getId(), papeis);
        return contato;
    }

    // ----------------------------------------------------------- etiquetas

    public List<Etiqueta> etiquetasDe(String entidade, UUID entidadeId) {
        return vinculos.findByEntidadeAndEntidadeId(entidade, entidadeId).stream()
                .map(VinculoDeEtiqueta::getEtiqueta)
                .sorted(Comparator.comparing(Etiqueta::getNome))
                .toList();
    }

    @Transactional
    public void trocarEtiquetas(String entidade, UUID entidadeId, List<UUID> etiquetaIds) {
        UUID empresaId = contexto.exigirEmpresaId();
        vinculos.deleteByEntidadeAndEntidadeId(entidade, entidadeId);
        if (etiquetaIds == null) {
            return;
        }
        for (UUID etiquetaId : etiquetaIds) {
            etiquetas.findByIdAndEmpresaId(etiquetaId, empresaId).ifPresent(e ->
                    vinculos.save(new VinculoDeEtiqueta(empresaId, e, entidade, entidadeId,
                            contexto.autor())));
        }
    }

    // ---------------------------------------------------------- enderecos

    public List<Endereco> enderecosDe(UUID unidadeId) {
        return enderecos.findByUnidadeIdOrderByTipo(unidadeId);
    }

    @Transactional
    public void salvarEndereco(UUID enderecoId, UUID unidadeId, String tipo, String cep,
                               String logradouro, String numero, String complemento,
                               String bairro, String cidade, String uf, String pais,
                               Fonte fonte) {
        UUID empresaId = contexto.exigirEmpresaId();
        Endereco endereco = enderecoId == null
                ? new Endereco(empresaId, unidadeId, tipo)
                : enderecos.findByIdAndEmpresaId(enderecoId, empresaId)
                        .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado."));
        endereco.ajustar(tipo, cep, logradouro, numero, complemento, bairro, cidade, uf, pais, fonte);
        enderecos.save(endereco);
    }

    // ----------------------------------------------------------- cobranca

    public PreferenciaDeCobranca cobrancaDe(UUID unidadeId) {
        return preferencias.findById(unidadeId)
                .orElseGet(() -> new PreferenciaDeCobranca(unidadeId, contexto.exigirEmpresaId()));
    }

    @Transactional
    public void salvarCobranca(UUID unidadeId, String forma, String chavePix, String banco,
                               String agencia, String conta, String titular, Integer diaVencimento,
                               String periodicidade, String emailCobranca, boolean debitoRecorrente,
                               BigDecimal juros, BigDecimal multa, BigDecimal desconto,
                               Integer carencia, Integer protestarApos, String instrucoes,
                               Fonte fonte) {
        PreferenciaDeCobranca preferencia = preferencias.findById(unidadeId)
                .orElseGet(() -> new PreferenciaDeCobranca(unidadeId, contexto.exigirEmpresaId()));
        preferencia.ajustar(forma, chavePix, banco, agencia, conta, titular, diaVencimento,
                periodicidade, emailCobranca, debitoRecorrente, juros, multa, desconto,
                carencia, protestarApos, instrucoes, fonte);
        preferencias.save(preferencia);
    }

    // ---------------------------------------------------------- contratos

    public List<Contrato> contratosDe(UUID pagadorId) {
        List<Contrato> lista = contratos.findByPagadorIdOrderByNumero(pagadorId);
        for (Contrato c : lista) {
            List<ServicoDoContrato> itens = servicosDoContrato.findByContratoId(c.getId());
            itens.forEach(i -> {
                if (i.getServicoId() != null) {
                    servicos.findById(i.getServicoId())
                            .ifPresent(s -> i.receberNomeDoServico(s.getNome()));
                }
            });
            c.receberServicos(itens);
            if (c.getUnidadeId() != null) {
                unidades.findById(c.getUnidadeId())
                        .ifPresent(u -> c.receberNomeDaUnidade(u.getRazaoSocial()));
            }
        }
        return lista;
    }

    @Transactional
    public Contrato salvarContrato(UUID contratoId, UUID pagadorId, UUID unidadeId, String numero,
                                   String descricao, LocalDate inicio, LocalDate fim,
                                   BigDecimal valor, String indice, Integer mesReajuste,
                                   Integer diaVencimento, String periodicidade,
                                   String responsavel, String situacao, String observacao) {
        UUID empresaId = contexto.exigirEmpresaId();
        Contrato contrato = contratoId == null
                ? new Contrato(empresaId, pagadorId, unidadeId,
                        numero == null || numero.isBlank() ? proximoNumeroDeContrato() : numero,
                        contexto.autor())
                : contratos.findByIdAndEmpresaId(contratoId, empresaId)
                        .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        contrato.ajustar(unidadeId, descricao, inicio, fim, valor, indice, mesReajuste,
                diaVencimento, periodicidade, responsavel, situacao, observacao);
        return contratos.save(contrato);
    }

    @Transactional
    public void adicionarServicoAoContrato(UUID contratoId, UUID servicoId, String descricao,
                                           BigDecimal quantidade, BigDecimal valor) {
        contratos.findByIdAndEmpresaId(contratoId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        servicosDoContrato.save(new ServicoDoContrato(contratoId, servicoId, descricao,
                quantidade, valor));
    }

    private String proximoNumeroDeContrato() {
        long quantos = contratos.quantos(contexto.exigirEmpresaId());
        return "C" + String.format("%04d", quantos + 1);
    }

    // ------------------------------------------------------------- credito

    public AnaliseDeCredito creditoDe(UUID pagadorId) {
        return creditos.findById(pagadorId)
                .orElseGet(() -> new AnaliseDeCredito(pagadorId, contexto.exigirEmpresaId()));
    }

    @Transactional
    public void salvarCredito(UUID pagadorId, String classificacao, BigDecimal limite,
                              boolean bloqueado, String motivo, String observacao) {
        AnaliseDeCredito analise = creditos.findById(pagadorId)
                .orElseGet(() -> new AnaliseDeCredito(pagadorId, contexto.exigirEmpresaId()));
        analise.ajustar(classificacao, limite, bloqueado, motivo, observacao, contexto.autor());
        creditos.save(analise);
    }

    public List<Restricao> restricoesDe(UUID pagadorId) {
        return restricoes.findByPagadorIdOrderByDataDesc(pagadorId);
    }

    @Transactional
    public void registrarRestricao(UUID pagadorId, String tipo, String origem, BigDecimal valor,
                                   LocalDate data, String observacao) {
        restricoes.save(new Restricao(contexto.exigirEmpresaId(), pagadorId, tipo, origem,
                valor, data, observacao));
    }

    // -------------------------------------------------------- linha do tempo

    public List<Interacao> historicoDe(UUID pagadorId) {
        return interacoes.findByPagadorIdOrderByOcorridoEmDesc(pagadorId);
    }

    public List<Interacao> historicoDaUnidade(UUID unidadeId) {
        return interacoes.findByUnidadeIdOrderByOcorridoEmDesc(unidadeId);
    }

    @Transactional
    public void registrarInteracao(UUID pagadorId, UUID unidadeId, String tipo, String descricao,
                                   BigDecimal valor, LocalDate dataPrometida, String canal) {
        interacoes.save(new Interacao(contexto.exigirEmpresaId(), pagadorId, unidadeId, tipo,
                descricao, valor, dataPrometida, canal, contexto.autor()));
    }

    @Transactional
    public void baixarPromessa(UUID interacaoId, boolean cumprida) {
        Interacao promessa = interacoes.findByIdAndEmpresaId(interacaoId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado."));
        if (cumprida) {
            promessa.marcarCumprida();
        } else {
            promessa.marcarQuebrada();
        }
    }

    // ---------------------------------------------------------- documentos

    public List<Documento> documentosDe(UUID pagadorId) {
        return documentos.findByPagadorIdOrderByAnexadoEmDesc(pagadorId);
    }

    public List<Documento> documentosDaUnidade(UUID unidadeId) {
        return documentos.findByUnidadeIdOrderByAnexadoEmDesc(unidadeId);
    }

    // -------------------------------------------------------- conformidade

    public List<Consentimento> consentimentosDe(UUID pagadorId) {
        return consentimentos.findByPagadorIdOrderByRegistradoEmDesc(pagadorId);
    }

    @Transactional
    public void registrarConsentimento(UUID pagadorId, UUID contatoId, String canal,
                                       String baseLegal, String situacao, String origem,
                                       String observacao) {
        consentimentos.save(new Consentimento(contexto.exigirEmpresaId(), pagadorId, contatoId,
                canal, baseLegal, situacao, origem, observacao, contexto.autor()));
    }

    // -------------------------------------------------------------- pessoa

    public Pagador pessoa(UUID pagadorId) {
        return carteira.pagador(pagadorId);
    }

    @Transactional
    public void salvarIdentificacao(UUID pagadorId, String nome, String nomeSocial, String cpf,
                                    LocalDate dataNascimento, LocalDate clienteDesde,
                                    String situacao, String whatsapp, String telefone,
                                    String email, String observacao) {
        Pagador pessoa = carteira.pagador(pagadorId);
        pessoa.atualizarIdentificacao(nome, nomeSocial, cpf, dataNascimento, clienteDesde, situacao);
        pessoa.atualizar(nome, cpf, whatsapp, telefone, email, observacao);
    }

    // ----------------------------------------------------- o calculo do risco

    /**
     * A posição financeira da pessoa, calculada dos títulos dela.
     *
     * Aging em quatro faixas, atraso médio do que já foi pago, e o
     * comportamento resumido numa palavra. Nada disso fica gravado.
     */
    public static final class Posicao {

        private final List<Titulo> titulos;
        private final LocalDate hoje;

        Posicao(List<Titulo> titulos, LocalDate hoje) {
            this.titulos = titulos;
            this.hoje = hoje;
        }

        private List<Titulo> abertos() {
            return titulos.stream()
                    .filter(t -> t.getSituacao() == SituacaoTitulo.ABERTO
                            || t.getSituacao() == SituacaoTitulo.PARCIAL)
                    .toList();
        }

        public BigDecimal getEmAberto() {
            return somar(abertos().stream().map(Titulo::getSaldo).toList());
        }

        public BigDecimal getVencido() {
            return somar(abertos().stream().filter(t -> t.estaVencido(hoje))
                    .map(Titulo::getSaldo).toList());
        }

        public BigDecimal getAVencer() {
            return somar(abertos().stream().filter(t -> !t.estaVencido(hoje))
                    .map(Titulo::getSaldo).toList());
        }

        /** Faixas de atraso: ate 30, 31 a 60, 61 a 90 e mais de 90 dias. */
        public BigDecimal faixa(int de, int ate) {
            return somar(abertos().stream()
                    .filter(t -> t.estaVencido(hoje))
                    .filter(t -> {
                        long dias = t.diasDeAtraso(hoje);
                        return dias >= de && (ate == 0 || dias <= ate);
                    })
                    .map(Titulo::getSaldo).toList());
        }

        public BigDecimal getAte30() {
            return faixa(1, 30);
        }

        public BigDecimal getDe31a60() {
            return faixa(31, 60);
        }

        public BigDecimal getDe61a90() {
            return faixa(61, 90);
        }

        public BigDecimal getAcimaDe90() {
            return faixa(91, 0);
        }

        public long getMaiorAtraso() {
            return abertos().stream().mapToLong(t -> t.diasDeAtraso(hoje)).max().orElse(0);
        }

        public BigDecimal getTotalFaturado() {
            return somar(titulos.stream().filter(Titulo::contaNoTotal)
                    .map(Titulo::getValor).toList());
        }

        public BigDecimal getTotalRecebido() {
            return somar(titulos.stream().map(Titulo::getTotalPago).toList());
        }

        public BigDecimal getCredito() {
            return somar(titulos.stream().map(Titulo::getCredito).toList());
        }

        public int getQuantidadeDeTitulos() {
            return titulos.size();
        }

        public BigDecimal getTicketMedio() {
            List<Titulo> validos = titulos.stream()
                    .filter(Titulo::contaNoTotal).toList();
            if (validos.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return getTotalFaturado().divide(BigDecimal.valueOf(validos.size()), 2,
                    RoundingMode.HALF_UP);
        }

        public LocalDate getProximoVencimento() {
            return abertos().stream().map(Titulo::getVencimento)
                    .filter(v -> !v.isBefore(hoje)).min(Comparator.naturalOrder()).orElse(null);
        }

        public LocalDate getUltimoRecebimento() {
            return titulos.stream().flatMap(t -> t.getPagamentos().stream())
                    .map(Pagamento::getPagoEm).max(Comparator.naturalOrder()).orElse(null);
        }

        /** Quantos dias, em média, o cliente paga depois do vencimento. */
        public long getAtrasoMedio() {
            List<Long> atrasos = new ArrayList<>();
            for (Titulo t : titulos) {
                for (Pagamento p : t.getPagamentos()) {
                    atrasos.add(ChronoUnit.DAYS.between(t.getVencimento(), p.getPagoEm()));
                }
            }
            if (atrasos.isEmpty()) {
                return 0;
            }
            return Math.round(atrasos.stream().mapToLong(Long::longValue).average().orElse(0));
        }

        public int getTitulosPagos() {
            return (int) titulos.stream().filter(t -> t.getSituacao() == SituacaoTitulo.PAGO).count();
        }

        /** O comportamento de pagamento resumido numa palavra. */
        public String getComportamento() {
            if (titulos.isEmpty()) {
                return "sem histórico";
            }
            long atrasoMedio = getAtrasoMedio();
            if (getMaiorAtraso() > 90) {
                return "crítico";
            }
            if (atrasoMedio <= 0) {
                return "paga em dia";
            }
            if (atrasoMedio <= 10) {
                return "paga com pequeno atraso";
            }
            if (atrasoMedio <= 30) {
                return "paga com atraso";
            }
            return "atraso alto";
        }

        private static BigDecimal somar(List<BigDecimal> valores) {
            return valores.stream().filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /** Quanto do limite de crédito já está usado. */
    public BigDecimal limiteDisponivel(UUID pagadorId) {
        AnaliseDeCredito analise = creditoDe(pagadorId);
        if (analise.getLimiteCredito() == null) {
            return null;
        }
        BigDecimal usado = posicaoDa(pagadorId).getEmAberto();
        return analise.getLimiteCredito().subtract(usado);
    }

    public Optional<Documento> documento(UUID documentoId) {
        return documentos.findByIdAndEmpresaId(documentoId, contexto.exigirEmpresaId());
    }
}
