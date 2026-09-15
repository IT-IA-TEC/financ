package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Quem ficou de fora do disparo, e por quê.
 *
 * Isso não é detalhe: a pergunta "por que fulano não recebeu" chega sempre, e
 * sem esta linha a resposta seria um chute.
 */
@Entity
@Table(name = "fora_do_lote")
public class ForaDoLote {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "titulo_id")
    private UUID tituloId;

    private String quem;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected ForaDoLote() {
        // exigido pelo JPA
    }

    public ForaDoLote(UUID empresaId, UUID loteId, UUID unidadeId, UUID tituloId,
                      String quem, String motivo) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.loteId = loteId;
        this.unidadeId = unidadeId;
        this.tituloId = tituloId;
        this.quem = quem;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public String getQuem() {
        return quem;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
