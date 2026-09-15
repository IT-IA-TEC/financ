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

/** Liga uma etiqueta a alguma coisa: um contato, uma pessoa, uma unidade. */
@Entity
@Table(name = "etiqueta_vinculo")
public class VinculoDeEtiqueta {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etiqueta_id", nullable = false)
    private Etiqueta etiqueta;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected VinculoDeEtiqueta() {
        // exigido pelo JPA
    }

    public VinculoDeEtiqueta(UUID empresaId, Etiqueta etiqueta, String entidade,
                             UUID entidadeId, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.etiqueta = etiqueta;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.criadoPor = criadoPor;
    }

    public UUID getId() {
        return id;
    }

    public Etiqueta getEtiqueta() {
        return etiqueta;
    }

    public String getEntidade() {
        return entidade;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }
}
