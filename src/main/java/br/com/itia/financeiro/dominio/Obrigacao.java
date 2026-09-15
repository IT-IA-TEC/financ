package br.com.itia.financeiro.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma conta a pagar.
 *
 * O que este desenho protege:
 *   1. Todo total é explicável: o valor sai da composição, e cada linha dela
 *      diz natureza, área e, quando existe, pessoa e bem.
 *   2. Os cinco controles andam separados. Aprovado, pago em parte e ainda
 *      não conciliado é uma situação possível, e um campo só não diz isso.
 *   3. Vencido é conta, não campo: sai do vencimento com saldo em aberto.
 *   4. Obrigação e pagamento são registros diferentes, e por isso um estorno
 *      não apaga a obrigação.
 */
@Entity
@Table(name = "obrigacao")
public class Obrigacao {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** Quando outra empresa do grupo paga por esta, a relação fica registrada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_pagadora_id")
    private Empresa empresaPagadora;

    @Column(nullable = false, updatable = false)
    private Long numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "favorecido_id", nullable = false)
    private Favorecido favorecido;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false)
    private TipoDeOperacao tipoOperacao = TipoDeOperacao.DESPESA;

    private LocalDate emissao;
    private LocalDate competencia;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(name = "pessoa_relacionada")
    private String pessoaRelacionada;

    /** O cliente ou projeto para quem o trabalho foi feito. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_id")
    private Pagador cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bem_id")
    private Bem bem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private ContaFinanceira conta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QualidadeDoCadastro qualidade = QualidadeDoCadastro.COMPLETO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Aprovacao aprovacao = Aprovacao.NAO_EXIGIDA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Execucao execucao = Execucao.NAO_ENCAMINHADA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Liquidacao liquidacao = Liquidacao.EM_ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Conciliacao conciliacao = Conciliacao.NAO_CONCILIADA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemDoRegistro origem = OrigemDoRegistro.MANUAL;

    @Column(name = "origem_referencia")
    private String origemReferencia;

    @Column(name = "grupo_parcelas")
    private UUID grupoParcelas;

    private Integer parcela;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Column(name = "recorrencia_id")
    private UUID recorrenciaId;

    private String solicitante;
    private String observacao;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Column(nullable = false)
    private boolean cancelada = false;

    @Column(name = "aprovada_por")
    private String aprovadaPor;

    @Column(name = "aprovada_em")
    private OffsetDateTime aprovadaEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @OneToMany(mappedBy = "obrigacao", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("ordem")
    private List<ItemDaObrigacao> itens = new ArrayList<>();

    @OneToMany(mappedBy = "obrigacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("pagoEm")
    private List<PagamentoDeObrigacao> pagamentos = new ArrayList<>();

    protected Obrigacao() {
        // exigido pelo JPA
    }

    public Obrigacao(Empresa empresa, Long numero, Favorecido favorecido, String descricao,
                     LocalDate vencimento, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.numero = numero;
        this.favorecido = favorecido;
        this.descricao = descricao;
        this.vencimento = vencimento;
        this.criadoPor = criadoPor;
    }

    public void ajustarDadosGerais(Favorecido favorecido, String descricao,
                                   TipoDeOperacao tipoOperacao, LocalDate emissao,
                                   LocalDate competencia, LocalDate vencimento, BigDecimal valor,
                                   ContaFinanceira conta, Empresa empresaPagadora) {
        this.favorecido = favorecido;
        this.descricao = descricao;
        this.tipoOperacao = tipoOperacao;
        this.emissao = emissao;
        this.competencia = competencia;
        this.vencimento = vencimento;
        this.valor = valor == null ? BigDecimal.ZERO : valor;
        this.conta = conta;
        this.empresaPagadora = empresaPagadora;
    }

    public void ajustarVinculos(String pessoaRelacionada, Pagador cliente, Bem bem,
                                String solicitante, String observacao) {
        this.pessoaRelacionada = pessoaRelacionada;
        this.cliente = cliente;
        this.bem = bem;
        this.solicitante = solicitante;
        this.observacao = observacao;
    }

    public void ajustarOrigem(OrigemDoRegistro origem, String referencia) {
        this.origem = origem;
        this.origemReferencia = referencia;
    }

    public void marcarComoParcela(UUID grupo, int parcela, int total) {
        this.grupoParcelas = grupo;
        this.parcela = parcela;
        this.totalParcelas = total;
    }

    public void veioDaRecorrencia(UUID recorrenciaId) {
        this.recorrenciaId = recorrenciaId;
        this.origem = OrigemDoRegistro.RECORRENCIA;
    }

    public void receber(ItemDaObrigacao item) {
        itens.add(item);
    }

    public void limparItens() {
        itens.clear();
    }

    public void receber(PagamentoDeObrigacao pagamento) {
        pagamentos.add(pagamento);
        recalcularLiquidacao();
    }

    public void ajustarQualidade(QualidadeDoCadastro qualidade) {
        this.qualidade = qualidade;
    }

    public void exigirAprovacao() {
        this.aprovacao = Aprovacao.PENDENTE;
    }

    public void aprovar(String quem) {
        this.aprovacao = Aprovacao.APROVADA;
        this.aprovadaPor = quem;
        this.aprovadaEm = OffsetDateTime.now();
    }

    public void rejeitar(String quem, String motivo) {
        this.aprovacao = Aprovacao.REJEITADA;
        this.aprovadaPor = quem;
        this.aprovadaEm = OffsetDateTime.now();
        this.observacao = motivo;
    }

    /**
     * Mudança em valor ou favorecido derruba a aprovação anterior.
     *
     * Quem aprovou aprovou aquele valor para aquela pessoa. Mudou, precisa
     * aprovar de novo.
     */
    public void invalidarAprovacao() {
        if (aprovacao == Aprovacao.APROVADA) {
            this.aprovacao = Aprovacao.PENDENTE;
            this.aprovadaPor = null;
            this.aprovadaEm = null;
        }
    }

    public void ajustarExecucao(Execucao execucao) {
        this.execucao = execucao;
    }

    public void ajustarConciliacao(Conciliacao conciliacao) {
        this.conciliacao = conciliacao;
    }

    public void cancelar(String motivo) {
        this.cancelada = true;
        this.motivoCancelamento = motivo;
    }

    /** Recalcula a liquidação pela soma dos pagamentos que valem. */
    public void recalcularLiquidacao() {
        BigDecimal pago = getTotalPago();
        if (pago.signum() <= 0) {
            this.liquidacao = Liquidacao.EM_ABERTO;
        } else if (pago.compareTo(valor) >= 0) {
            this.liquidacao = Liquidacao.LIQUIDADA;
        } else {
            this.liquidacao = Liquidacao.PARCIAL;
        }
    }

    public BigDecimal getTotalPago() {
        return pagamentos.stream()
                .filter(p -> !p.isEstornado())
                .map(PagamentoDeObrigacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSaldo() {
        BigDecimal saldo = valor.subtract(getTotalPago());
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    /** A soma da composição, para conferir se bate com o valor. */
    public BigDecimal getTotalDosItens() {
        return itens.stream()
                .map(ItemDaObrigacao::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean composicaoFecha() {
        return itens.isEmpty() || getTotalDosItens().compareTo(valor) == 0;
    }

    /** Vencido é conta, não campo que alguém atualiza na mão. */
    public boolean estaVencida(LocalDate hoje) {
        return !cancelada && getSaldo().signum() > 0 && vencimento.isBefore(hoje);
    }

    public long diasDeAtraso(LocalDate hoje) {
        if (!estaVencida(hoje)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(vencimento, hoje);
    }

    /** O que falta para esta conta poder ser paga com segurança. */
    public List<String> pendencias() {
        List<String> faltando = new ArrayList<>();
        if (qualidade == QualidadeDoCadastro.RASCUNHO) {
            faltando.add("rascunho");
        }
        if (itens.isEmpty()) {
            faltando.add("sem classificação");
        } else if (!composicaoFecha()) {
            faltando.add("composição não fecha");
        }
        if (aprovacao == Aprovacao.PENDENTE) {
            faltando.add("aguardando aprovação");
        }
        if (itens.stream().anyMatch(i -> i.getNatureza() == null)) {
            faltando.add("item sem natureza");
        }
        if (itens.stream().anyMatch(i -> i.getCentroDeCusto() == null)) {
            faltando.add("item sem centro de custo");
        }
        return faltando;
    }

    public boolean podeIrParaPagamento() {
        return !cancelada && aprovacao.liberaPagamento()
                && qualidade != QualidadeDoCadastro.RASCUNHO
                && getSaldo().signum() > 0;
    }

    public String getValorResumido() {
        return "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valor);
    }

    public String getEtiquetaParcela() {
        if (parcela == null || totalParcelas == null) {
            return null;
        }
        return parcela + " de " + totalParcelas;
    }

    /** Quem paga: a própria empresa, ou outra do grupo. */
    public boolean pagaPorOutraEmpresa() {
        return empresaPagadora != null && !empresaPagadora.getId().equals(empresa.getId());
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Empresa getEmpresaPagadora() {
        return empresaPagadora;
    }

    public Long getNumero() {
        return numero;
    }

    public Favorecido getFavorecido() {
        return favorecido;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoDeOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public LocalDate getEmissao() {
        return emissao;
    }

    public LocalDate getCompetencia() {
        return competencia;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getPessoaRelacionada() {
        return pessoaRelacionada;
    }

    public Pagador getCliente() {
        return cliente;
    }

    public Bem getBem() {
        return bem;
    }

    public ContaFinanceira getConta() {
        return conta;
    }

    public QualidadeDoCadastro getQualidade() {
        return qualidade;
    }

    public Aprovacao getAprovacao() {
        return aprovacao;
    }

    public Execucao getExecucao() {
        return execucao;
    }

    public Liquidacao getLiquidacao() {
        return liquidacao;
    }

    public Conciliacao getConciliacao() {
        return conciliacao;
    }

    public OrigemDoRegistro getOrigem() {
        return origem;
    }

    public String getOrigemReferencia() {
        return origemReferencia;
    }

    public UUID getGrupoParcelas() {
        return grupoParcelas;
    }

    public Integer getParcela() {
        return parcela;
    }

    public Integer getTotalParcelas() {
        return totalParcelas;
    }

    public UUID getRecorrenciaId() {
        return recorrenciaId;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    public String getAprovadaPor() {
        return aprovadaPor;
    }

    public OffsetDateTime getAprovadaEm() {
        return aprovadaEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public List<ItemDaObrigacao> getItens() {
        return itens;
    }

    public List<PagamentoDeObrigacao> getPagamentos() {
        return pagamentos;
    }
}
