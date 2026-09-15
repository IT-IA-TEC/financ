package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Um pagamento feito contra uma conta a pagar.
 *
 * É um registro separado da obrigação. Uma conta de mil pode ser paga com
 * quatrocentos hoje e seiscentos depois, e o estorno de um pagamento não
 * apaga a obrigação nem o histórico.
 */
@Entity
@Table(name = "pagamento_obrigacao")
public class PagamentoDeObrigacao {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obrigacao_id", nullable = false)
    private Obrigacao obrigacao;

    @Column(name = "pago_em", nullable = false)
    private LocalDate pagoEm;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal juros = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal multa = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal retencao = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private ContaFinanceira conta;

    private String forma;

    @Column(name = "documento_id")
    private UUID documentoId;

    private String observacao;

    @Column(nullable = false)
    private boolean estornado = false;

    @Column(name = "motivo_estorno")
    private String motivoEstorno;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected PagamentoDeObrigacao() {
        // exigido pelo JPA
    }

    public PagamentoDeObrigacao(Obrigacao obrigacao, LocalDate pagoEm, BigDecimal valor,
                                ContaFinanceira conta, String forma, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = obrigacao.getEmpresa();
        this.obrigacao = obrigacao;
        this.pagoEm = pagoEm == null ? LocalDate.now() : pagoEm;
        this.valor = valor;
        this.conta = conta;
        this.forma = forma;
        this.criadoPor = criadoPor;
    }

    public void ajustarEncargos(BigDecimal juros, BigDecimal multa, BigDecimal desconto,
                                BigDecimal retencao, String observacao) {
        this.juros = juros == null ? BigDecimal.ZERO : juros;
        this.multa = multa == null ? BigDecimal.ZERO : multa;
        this.desconto = desconto == null ? BigDecimal.ZERO : desconto;
        this.retencao = retencao == null ? BigDecimal.ZERO : retencao;
        this.observacao = observacao;
    }

    public void guardarComprovante(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public void estornar(String motivo) {
        this.estornado = true;
        this.motivoEstorno = motivo;
    }

    /** O que saiu do banco de verdade: o valor com encargos e abatimentos. */
    public BigDecimal getSaidaDoBanco() {
        return valor.add(juros).add(multa).subtract(desconto).subtract(retencao);
    }

    public UUID getId() {
        return id;
    }

    public Obrigacao getObrigacao() {
        return obrigacao;
    }

    public LocalDate getPagoEm() {
        return pagoEm;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getJuros() {
        return juros;
    }

    public BigDecimal getMulta() {
        return multa;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getRetencao() {
        return retencao;
    }

    public ContaFinanceira getConta() {
        return conta;
    }

    public String getForma() {
        return forma;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isEstornado() {
        return estornado;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
