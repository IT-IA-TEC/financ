package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A contratação de um pacote por um cliente.
 *
 * O valor acordado fica guardado aqui, e não no pacote: cada cliente pode ter
 * fechado por um valor diferente, e mexer no preço do pacote não pode mudar o
 * que já foi combinado com quem contratou antes.
 */
@Entity
@Table(name = "pacote_contratacao")
public class ContratacaoDePacote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pacote_id", nullable = false)
    private Pacote pacote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagador_id", nullable = false)
    private Pagador pagador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private ClienteEspelho unidade;

    @Column(name = "valor_acordado", precision = 14, scale = 2)
    private BigDecimal valorAcordado;

    private LocalDate inicio;

    private LocalDate fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoDaContratacao situacao = SituacaoDaContratacao.ATIVA;

    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected ContratacaoDePacote() {
        // exigido pelo JPA
    }

    public ContratacaoDePacote(Empresa empresa, Pacote pacote, Pagador pagador, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.pacote = pacote;
        this.pagador = pagador;
        this.criadoPor = criadoPor;
    }

    public void ajustar(Pacote pacote, Pagador pagador, ClienteEspelho unidade,
                        BigDecimal valorAcordado, LocalDate inicio, LocalDate fim,
                        SituacaoDaContratacao situacao, String observacao) {
        this.pacote = pacote;
        this.pagador = pagador;
        this.unidade = unidade;
        this.valorAcordado = valorAcordado;
        this.inicio = inicio;
        this.fim = fim;
        this.situacao = situacao;
        this.observacao = observacao;
    }

    public void encerrar(LocalDate quando) {
        this.situacao = SituacaoDaContratacao.ENCERRADA;
        this.fim = quando;
    }

    /** O valor que vale para este cliente: o acordado, ou o do pacote. */
    public BigDecimal getValorEfetivo() {
        return valorAcordado != null ? valorAcordado : pacote.getValor();
    }

    public String getValorResumido() {
        BigDecimal valor = getValorEfetivo();
        if (valor == null) {
            return "valor não definido";
        }
        return "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valor);
    }

    /** Se a contratação está valendo na data informada. */
    public boolean estaVigenteEm(LocalDate dia) {
        if (situacao != SituacaoDaContratacao.ATIVA) {
            return false;
        }
        boolean jaComecou = inicio == null || !dia.isBefore(inicio);
        boolean aindaNaoAcabou = fim == null || !dia.isAfter(fim);
        return jaComecou && aindaNaoAcabou;
    }

    public String getVigenciaResumida() {
        if (inicio == null && fim == null) {
            return "sem vigência informada";
        }
        java.time.format.DateTimeFormatter dia =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String de = inicio == null ? "sem início" : inicio.format(dia);
        String ate = fim == null ? "sem prazo" : fim.format(dia);
        return de + " até " + ate;
    }

    public UUID getId() {
        return id;
    }

    public Pacote getPacote() {
        return pacote;
    }

    public Pagador getPagador() {
        return pagador;
    }

    public ClienteEspelho getUnidade() {
        return unidade;
    }

    public BigDecimal getValorAcordado() {
        return valorAcordado;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public SituacaoDaContratacao getSituacao() {
        return situacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
