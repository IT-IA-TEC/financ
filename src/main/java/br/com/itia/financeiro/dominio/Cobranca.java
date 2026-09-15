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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma cobrança de pacote ou de serviço.
 *
 * Toda cobrança aponta para a origem: a contratação do pacote ou o serviço
 * realizado que a gerou. O valor a receber de verdade é o título; a cobrança
 * aprovada gera o título e guarda o número dele, e o pagamento é lido de lá.
 */
@Entity
@Table(name = "cobranca")
public class Cobranca {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemDaCobranca origem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagador_id", nullable = false)
    private Pagador pagador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private ClienteEspelho unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratacao_id")
    private ContratacaoDePacote contratacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realizado_id")
    private ServicoRealizado realizado;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    /** O período em texto curto, como 09/2026. */
    private String referencia;

    @Column(name = "valor_original", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorOriginal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_final", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFinal = BigDecimal.ZERO;

    private String justificativa;

    @Column(name = "aprovada_por")
    private String aprovadaPor;

    @Column(name = "aprovada_em")
    private OffsetDateTime aprovadaEm;

    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoDaCobranca situacao = SituacaoDaCobranca.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    @Column(name = "integracao_id")
    private UUID integracaoId;

    @Column(name = "referencia_externa")
    private String referenciaExterna;

    @Column(name = "enviada_em")
    private OffsetDateTime enviadaEm;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemDaCobranca> itens = new ArrayList<>();

    protected Cobranca() {
        // exigido pelo JPA
    }

    public Cobranca(Empresa empresa, OrigemDaCobranca origem, Pagador pagador, String descricao,
                    String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.origem = origem;
        this.pagador = pagador;
        this.descricao = descricao;
        this.criadoPor = criadoPor;
    }

    public void ajustarValores(BigDecimal valorOriginal, BigDecimal desconto,
                               String justificativa) {
        this.valorOriginal = valorOriginal == null ? BigDecimal.ZERO : valorOriginal;
        this.desconto = desconto == null ? BigDecimal.ZERO : desconto;
        BigDecimal total = this.valorOriginal.subtract(this.desconto);
        // Desconto maior que o valor nunca vira credito por distracao.
        this.valorFinal = total.signum() < 0 ? BigDecimal.ZERO : total;
        this.justificativa = justificativa;
    }

    public void ajustarPeriodo(LocalDate inicio, LocalDate fim, String referencia) {
        this.periodoInicio = inicio;
        this.periodoFim = fim;
        this.referencia = referencia;
    }

    public void ajustarDados(String descricao, LocalDate vencimento, ClienteEspelho unidade) {
        this.descricao = descricao;
        this.vencimento = vencimento;
        this.unidade = unidade;
    }

    public void vincularOrigem(ContratacaoDePacote contratacao, ServicoRealizado realizado) {
        this.contratacao = contratacao;
        this.realizado = realizado;
    }

    public void receber(ItemDaCobranca item) {
        itens.add(item);
    }

    public void limparItens() {
        itens.clear();
    }

    /** Gratuidade: fica registrada, sem virar valor a receber. */
    public void marcarSemValor(String motivo) {
        this.situacao = SituacaoDaCobranca.SEM_VALOR;
        this.valorFinal = BigDecimal.ZERO;
        this.justificativa = motivo;
    }

    public void aprovar(String quem) {
        this.situacao = SituacaoDaCobranca.APROVADA;
        this.aprovadaPor = quem;
        this.aprovadaEm = OffsetDateTime.now();
    }

    /** O titulo quitou: a lista de cobrancas passa a mostrar pago. */
    public void marcarPaga() {
        this.situacao = SituacaoDaCobranca.PAGA;
    }

    public void guardarTitulo(Titulo titulo) {
        this.titulo = titulo;
    }

    public void marcarEnviada(UUID integracaoId, String referenciaExterna) {
        this.situacao = SituacaoDaCobranca.ENVIADA;
        this.integracaoId = integracaoId;
        this.referenciaExterna = referenciaExterna;
        this.enviadaEm = OffsetDateTime.now();
        this.ultimoErro = null;
    }

    public void anotarErro(String erro) {
        this.ultimoErro = erro;
    }

    public void cancelar() {
        this.situacao = SituacaoDaCobranca.CANCELADA;
    }

    /** A situação do pagamento vem do título, nunca é digitada aqui. */
    public String getSituacaoDoPagamento() {
        if (titulo == null) {
            return situacao == SituacaoDaCobranca.SEM_VALOR ? "sem valor a receber"
                    : "ainda sem título";
        }
        return switch (titulo.getSituacao()) {
            case PAGO -> "pago";
            case PARCIAL -> "pago em parte";
            case CANCELADO -> "título cancelado";
            case EM_ACORDO -> "dentro de um acordo";
            case ABERTO -> titulo.getVencimento() != null
                    && titulo.getVencimento().isBefore(LocalDate.now())
                    ? "vencido" : "em aberto";
        };
    }

    public boolean estaPaga() {
        return titulo != null && titulo.getSituacao() == SituacaoTitulo.PAGO;
    }

    public String getValorResumido() {
        return "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valorFinal);
    }

    public String getPeriodoResumido() {
        if (referencia != null && !referencia.isBlank()) {
            return referencia;
        }
        if (periodoInicio == null) {
            return "sem período";
        }
        java.time.format.DateTimeFormatter dia =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return periodoInicio.format(dia) + " a "
                + (periodoFim == null ? "em aberto" : periodoFim.format(dia));
    }

    /** O que originou esta cobrança, em uma linha. */
    public String getOrigemResumida() {
        if (contratacao != null) {
            return "Pacote " + contratacao.getPacote().getNome();
        }
        if (realizado != null) {
            return realizado.getServico().getNome() + " em "
                    + realizado.getRealizadoEm().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "sem origem";
    }

    public boolean temDesconto() {
        return desconto != null && desconto.signum() > 0;
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public OrigemDaCobranca getOrigem() {
        return origem;
    }

    public Pagador getPagador() {
        return pagador;
    }

    public ClienteEspelho getUnidade() {
        return unidade;
    }

    public ContratacaoDePacote getContratacao() {
        return contratacao;
    }

    public ServicoRealizado getRealizado() {
        return realizado;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public String getReferencia() {
        return referencia;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public String getAprovadaPor() {
        return aprovadaPor;
    }

    public OffsetDateTime getAprovadaEm() {
        return aprovadaEm;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public SituacaoDaCobranca getSituacao() {
        return situacao;
    }

    public Titulo getTitulo() {
        return titulo;
    }

    public UUID getIntegracaoId() {
        return integracaoId;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public OffsetDateTime getEnviadaEm() {
        return enviadaEm;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public List<ItemDaCobranca> getItens() {
        return itens;
    }
}
