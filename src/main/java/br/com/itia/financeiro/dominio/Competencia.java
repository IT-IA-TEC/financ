package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um mês de cobrança, aberto ou fechado.
 *
 * Fechado quer dizer que aquele mês não gera nem recalcula mais nada. Quem
 * precisar mexer reabre de propósito, e o nome de quem reabriu fica gravado.
 */
@Entity
@Table(name = "competencia")
public class Competencia {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** O mês em texto curto: 09/2026. */
    @Column(nullable = false, updatable = false)
    private String referencia;

    /** ABERTA ou FECHADA. */
    @Column(nullable = false)
    private String situacao = "ABERTA";

    @Column(name = "fechada_em")
    private OffsetDateTime fechadaEm;

    @Column(name = "fechada_por")
    private String fechadaPor;

    @Column(name = "reaberta_em")
    private OffsetDateTime reabertaEm;

    @Column(name = "reaberta_por")
    private String reabertaPor;

    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Competencia() {
        // exigido pelo JPA
    }

    public Competencia(Empresa empresa, String referencia) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.referencia = referencia;
    }

    public void fechar(String quem) {
        this.situacao = "FECHADA";
        this.fechadaEm = OffsetDateTime.now();
        this.fechadaPor = quem;
    }

    public void reabrir(String quem, String motivo) {
        this.situacao = "ABERTA";
        this.reabertaEm = OffsetDateTime.now();
        this.reabertaPor = quem;
        this.motivo = motivo;
    }

    public boolean estaFechada() {
        return "FECHADA".equals(situacao);
    }

    public UUID getId() {
        return id;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getSituacao() {
        return situacao;
    }

    public OffsetDateTime getFechadaEm() {
        return fechadaEm;
    }

    public String getFechadaPor() {
        return fechadaPor;
    }

    public OffsetDateTime getReabertaEm() {
        return reabertaEm;
    }

    public String getReabertaPor() {
        return reabertaPor;
    }

    public String getMotivo() {
        return motivo;
    }
}
