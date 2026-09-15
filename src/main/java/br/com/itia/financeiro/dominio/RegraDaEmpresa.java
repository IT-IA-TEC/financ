package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * As regras que cada empresa liga ou desliga.
 *
 * Tudo nasce desligado. Quem não mexer aqui continua com o sistema do jeito
 * que estava, e é por isso que ligar uma regra nunca muda o passado: ela vale
 * do momento em que foi ligada em diante.
 */
@Entity
@Table(name = "regra_da_empresa")
public class RegraDaEmpresa {

    @Id
    @Column(name = "empresa_id")
    private UUID empresaId;

    /** Cada cobrança com identificador próprio no PIX, para a baixa sozinha. */
    @Column(name = "pix_identificador", nullable = false)
    private boolean pixIdentificador = false;

    /** Pix Automático: o cliente autoriza uma vez e não é cobrado todo mês. */
    @Column(name = "pix_automatico", nullable = false)
    private boolean pixAutomatico = false;

    /** O agente diz que é robô quando perguntam. */
    @Column(name = "agente_se_identifica", nullable = false)
    private boolean agenteSeIdentifica = false;

    @Column(name = "cobrar_juros", nullable = false)
    private boolean cobrarJuros = false;

    @Column(name = "juros_ao_mes", nullable = false, precision = 7, scale = 4)
    private BigDecimal jurosAoMes = BigDecimal.ONE;

    @Column(name = "multa_por_atraso", nullable = false, precision = 7, scale = 4)
    private BigDecimal multaPorAtraso = new BigDecimal("2");

    @Column(name = "carencia_dias", nullable = false)
    private int carenciaDias = 0;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    protected RegraDaEmpresa() {
        // exigido pelo JPA
    }

    public RegraDaEmpresa(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public void ajustar(boolean pixIdentificador, boolean pixAutomatico,
                        boolean agenteSeIdentifica, boolean cobrarJuros, BigDecimal jurosAoMes,
                        BigDecimal multaPorAtraso, int carenciaDias, String quem) {
        this.pixIdentificador = pixIdentificador;
        this.pixAutomatico = pixAutomatico;
        this.agenteSeIdentifica = agenteSeIdentifica;
        this.cobrarJuros = cobrarJuros;
        this.jurosAoMes = jurosAoMes == null ? BigDecimal.ZERO : jurosAoMes;
        this.multaPorAtraso = multaPorAtraso == null ? BigDecimal.ZERO : multaPorAtraso;
        this.carenciaDias = Math.max(carenciaDias, 0);
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    /**
     * Quanto o atraso soma a um valor, nesta data.
     *
     * Multa é uma vez só, sobre o valor. Juros são pelos dias corridos, pela
     * taxa do mês dividida por 30. Dentro da carência, nada é somado.
     */
    public BigDecimal acrescimoDe(BigDecimal valor, LocalDate vencimento, LocalDate hoje) {
        if (!cobrarJuros || valor == null || vencimento == null || !vencimento.isBefore(hoje)) {
            return BigDecimal.ZERO;
        }
        long dias = ChronoUnit.DAYS.between(vencimento, hoje);
        if (dias <= carenciaDias) {
            return BigDecimal.ZERO;
        }
        BigDecimal multa = valor.multiply(multaPorAtraso)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal juros = valor.multiply(jurosAoMes)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .divide(new BigDecimal("30"), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
        return multa.add(juros);
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public boolean isPixIdentificador() {
        return pixIdentificador;
    }

    public boolean isPixAutomatico() {
        return pixAutomatico;
    }

    public boolean isAgenteSeIdentifica() {
        return agenteSeIdentifica;
    }

    public boolean isCobrarJuros() {
        return cobrarJuros;
    }

    public BigDecimal getJurosAoMes() {
        return jurosAoMes;
    }

    public BigDecimal getMultaPorAtraso() {
        return multaPorAtraso;
    }

    public int getCarenciaDias() {
        return carenciaDias;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }
}
