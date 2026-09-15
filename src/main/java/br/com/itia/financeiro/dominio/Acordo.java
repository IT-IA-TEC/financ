package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um acordo de dívida.
 *
 * O acordo não apaga o que era devido: os documentos originais continuam
 * guardados e saem da cobrança porque quem passa a ser cobrado são as
 * parcelas. É essa separação que permite responder, meses depois, "de onde
 * veio esse acordo e quanto de desconto foi dado".
 */
@Entity
@Table(name = "acordo")
public class Acordo {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "valor_original", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorOriginal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal acrescimo = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_combinado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorCombinado;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal entrada = BigDecimal.ZERO;

    @Column(nullable = false)
    private int parcelas = 1;

    @Column(name = "primeiro_vencimento", nullable = false)
    private LocalDate primeiroVencimento;

    /** ATIVO, CUMPRIDO, QUEBRADO ou CANCELADO. */
    @Column(nullable = false)
    private String situacao = "ATIVO";

    @Column(name = "motivo_desconto")
    private String motivoDesconto;

    @Column(name = "autorizado_por")
    private String autorizadoPor;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "quebrado_em")
    private OffsetDateTime quebradoEm;

    @Column(name = "motivo_quebra")
    private String motivoQuebra;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Acordo() {
        // exigido pelo JPA
    }

    public Acordo(UUID empresaId, UUID unidadeId, Integer numero, BigDecimal valorOriginal,
                  BigDecimal acrescimo, BigDecimal desconto, BigDecimal entrada, int parcelas,
                  LocalDate primeiroVencimento, String motivoDesconto, String autorizadoPor,
                  String observacao, String criadoPor) {
        if (valorOriginal == null || valorOriginal.signum() <= 0) {
            throw new IllegalArgumentException("Escolha os documentos que entram no acordo.");
        }
        if (parcelas < 1) {
            throw new IllegalArgumentException("O acordo tem pelo menos uma parcela.");
        }
        if (primeiroVencimento == null) {
            throw new IllegalArgumentException("Diga quando vence a primeira parcela.");
        }
        BigDecimal maisJuros = acrescimo == null ? BigDecimal.ZERO : acrescimo;
        BigDecimal menosDesconto = desconto == null ? BigDecimal.ZERO : desconto;
        BigDecimal combinado = valorOriginal.add(maisJuros).subtract(menosDesconto);
        if (combinado.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O desconto não pode zerar o acordo. Confira os valores.");
        }
        BigDecimal deEntrada = entrada == null ? BigDecimal.ZERO : entrada;
        if (deEntrada.compareTo(combinado) > 0) {
            throw new IllegalArgumentException(
                    "A entrada não pode ser maior que o valor combinado.");
        }
        if (menosDesconto.signum() > 0
                && (motivoDesconto == null || motivoDesconto.isBlank())) {
            throw new IllegalArgumentException(
                    "Desconto precisa de motivo escrito. Desconto sem motivo é dinheiro sumido.");
        }
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
        this.numero = numero;
        this.valorOriginal = valorOriginal;
        this.acrescimo = maisJuros;
        this.desconto = menosDesconto;
        this.valorCombinado = combinado;
        this.entrada = deEntrada;
        this.parcelas = parcelas;
        this.primeiroVencimento = primeiroVencimento;
        this.motivoDesconto = motivoDesconto;
        this.autorizadoPor = autorizadoPor;
        this.observacao = observacao;
        this.criadoPor = criadoPor;
    }

    /**
     * Quanto vale cada parcela, com a sobra dos centavos na última.
     *
     * A soma volta fechando com o valor combinado, sempre. Centavo perdido em
     * divisão é o tipo de erro que só aparece no fim do ano.
     */
    public List<BigDecimal> valoresDasParcelas() {
        BigDecimal aParcelar = valorCombinado.subtract(entrada);
        BigDecimal cada = aParcelar.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.DOWN);
        List<BigDecimal> valores = new ArrayList<>();
        BigDecimal somado = BigDecimal.ZERO;
        for (int i = 1; i < parcelas; i++) {
            valores.add(cada);
            somado = somado.add(cada);
        }
        valores.add(aParcelar.subtract(somado));
        return valores;
    }

    public void marcarCumprido() {
        this.situacao = "CUMPRIDO";
    }

    public void quebrar(String motivo) {
        if (!"ATIVO".equals(situacao)) {
            throw new IllegalStateException("Só acordo ativo pode ser quebrado.");
        }
        this.situacao = "QUEBRADO";
        this.quebradoEm = OffsetDateTime.now();
        this.motivoQuebra = motivo == null || motivo.isBlank()
                ? "não cumprido" : motivo;
    }

    public void cancelar() {
        if ("CUMPRIDO".equals(situacao)) {
            throw new IllegalStateException("Acordo já cumprido não é cancelado.");
        }
        this.situacao = "CANCELADO";
    }

    public boolean estaAtivo() {
        return "ATIVO".equals(situacao);
    }

    public String getSituacaoLegivel() {
        return switch (situacao) {
            case "CUMPRIDO" -> "cumprido";
            case "QUEBRADO" -> "quebrado";
            case "CANCELADO" -> "cancelado";
            default -> "ativo";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public Integer getNumero() {
        return numero;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public BigDecimal getAcrescimo() {
        return acrescimo;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getValorCombinado() {
        return valorCombinado;
    }

    public BigDecimal getEntrada() {
        return entrada;
    }

    public int getParcelas() {
        return parcelas;
    }

    public LocalDate getPrimeiroVencimento() {
        return primeiroVencimento;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getMotivoDesconto() {
        return motivoDesconto;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getQuebradoEm() {
        return quebradoEm;
    }

    public String getMotivoQuebra() {
        return motivoQuebra;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
