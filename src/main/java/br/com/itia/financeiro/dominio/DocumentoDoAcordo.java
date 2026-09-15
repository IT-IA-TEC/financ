package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um documento que entrou num acordo, com o saldo que ele tinha na hora.
 *
 * Guardar o saldo da época importa: o valor do documento pode ser recalculado
 * depois, e a conta do acordo precisa continuar explicável.
 */
@Entity
@Table(name = "documento_do_acordo")
public class DocumentoDoAcordo {

    @Id
    private UUID id;

    @Column(name = "acordo_id", nullable = false)
    private UUID acordoId;

    @Column(name = "titulo_id", nullable = false)
    private UUID tituloId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo;

    protected DocumentoDoAcordo() {
        // exigido pelo JPA
    }

    public DocumentoDoAcordo(UUID acordoId, UUID tituloId, BigDecimal saldo) {
        this.id = UUID.randomUUID();
        this.acordoId = acordoId;
        this.tituloId = tituloId;
        this.saldo = saldo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAcordoId() {
        return acordoId;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
