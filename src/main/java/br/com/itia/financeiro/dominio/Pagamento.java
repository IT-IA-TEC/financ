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

/** Dinheiro que entrou. Uma linha por entrada, nunca sobrescrita. */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titulo_id", nullable = false)
    private Titulo titulo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "pago_em", nullable = false)
    private LocalDate pagoEm;

    @Column(nullable = false)
    private String forma = "PIX";

    /** Identificador da transacao no banco. E ele que barra pagamento repetido. */
    @Column(name = "transacao_id")
    private String transacaoId;

    @Column(name = "conferido_por")
    private String conferidoPor;

    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Pagamento() {
        // exigido pelo JPA
    }

    public Pagamento(BigDecimal valor, LocalDate pagoEm, String forma,
                     String transacaoId, String conferidoPor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do pagamento tem que ser maior que zero.");
        }
        this.id = UUID.randomUUID();
        this.valor = valor;
        this.pagoEm = pagoEm == null ? LocalDate.now() : pagoEm;
        this.forma = forma == null ? "PIX" : forma;
        this.transacaoId = transacaoId;
        this.conferidoPor = conferidoPor;
    }

    void vincularA(Titulo titulo) {
        this.titulo = titulo;
        this.empresa = titulo.getEmpresa();
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Titulo getTitulo() {
        return titulo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getPagoEm() {
        return pagoEm;
    }

    public String getForma() {
        return forma;
    }

    public String getTransacaoId() {
        return transacaoId;
    }

    public String getConferidoPor() {
        return conferidoPor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
