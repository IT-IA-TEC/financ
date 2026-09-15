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
 * O molde de uma conta que se repete: aluguel, assinatura, internet.
 *
 * Recorrência é diferente de parcelamento. Parcelamento é uma contratação
 * dividida; recorrência não tem fim previsto. Quando o valor muda todo mês, a
 * conta nasce como previsão e pede conferência, para ninguém confundir
 * previsão com cobrança confirmada.
 */
@Entity
@Table(name = "recorrencia")
public class Recorrencia {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "favorecido_id", nullable = false)
    private Favorecido favorecido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natureza_id")
    private Natureza natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroDeCusto centroDeCusto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false)
    private TipoDeOperacao tipoOperacao = TipoDeOperacao.DESPESA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Periodicidade periodicidade = Periodicidade.MENSAL;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento = 10;

    @Column(name = "valor_previsto", precision = 14, scale = 2)
    private BigDecimal valorPrevisto;

    @Column(name = "valor_variavel", nullable = false)
    private boolean valorVariavel = false;

    private LocalDate inicio;
    private LocalDate fim;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Recorrencia() {
        // exigido pelo JPA
    }

    public Recorrencia(Empresa empresa, String descricao, Favorecido favorecido,
                       String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.descricao = descricao;
        this.favorecido = favorecido;
        this.criadoPor = criadoPor;
    }

    public void ajustar(String descricao, Favorecido favorecido, Natureza natureza,
                        CentroDeCusto centroDeCusto, TipoDeOperacao tipoOperacao,
                        Periodicidade periodicidade, int diaVencimento, BigDecimal valorPrevisto,
                        boolean valorVariavel, LocalDate inicio, LocalDate fim, boolean ativa) {
        this.descricao = descricao;
        this.favorecido = favorecido;
        this.natureza = natureza;
        this.centroDeCusto = centroDeCusto;
        this.tipoOperacao = tipoOperacao;
        this.periodicidade = periodicidade;
        this.diaVencimento = diaVencimento;
        this.valorPrevisto = valorPrevisto;
        this.valorVariavel = valorVariavel;
        this.inicio = inicio;
        this.fim = fim;
        this.ativa = ativa;
    }

    /** De quantos em quantos meses esta conta nasce. */
    public int mesesDoIntervalo() {
        return switch (periodicidade) {
            case MENSAL -> 1;
            case TRIMESTRAL -> 3;
            case SEMESTRAL -> 6;
            case ANUAL -> 12;
            case AVULSA, OUTRA -> 0;
        };
    }

    public boolean valeNoMes(LocalDate primeiroDia) {
        if (!ativa) {
            return false;
        }
        boolean jaComecou = inicio == null || !inicio.isAfter(primeiroDia.withDayOfMonth(
                primeiroDia.lengthOfMonth()));
        boolean aindaNaoAcabou = fim == null || !fim.isBefore(primeiroDia);
        if (!jaComecou || !aindaNaoAcabou) {
            return false;
        }
        int meses = mesesDoIntervalo();
        if (meses <= 1) {
            return meses == 1;
        }
        LocalDate comeco = inicio == null ? primeiroDia : inicio;
        long distancia = java.time.temporal.ChronoUnit.MONTHS.between(
                java.time.YearMonth.from(comeco), java.time.YearMonth.from(primeiroDia));
        return distancia >= 0 && distancia % meses == 0;
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getDescricao() {
        return descricao;
    }

    public Favorecido getFavorecido() {
        return favorecido;
    }

    public Natureza getNatureza() {
        return natureza;
    }

    public CentroDeCusto getCentroDeCusto() {
        return centroDeCusto;
    }

    public TipoDeOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public boolean isValorVariavel() {
        return valorVariavel;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
