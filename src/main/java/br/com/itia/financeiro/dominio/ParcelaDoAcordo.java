package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Uma parcela do acordo.
 *
 * A parcela é um título de verdade, para cair no contas a receber, na
 * conciliação e na cobrança como qualquer outro documento. Esta tabela só
 * guarda a ligação com o acordo e a ordem.
 */
@Entity
@Table(name = "parcela_do_acordo")
public class ParcelaDoAcordo {

    @Id
    private UUID id;

    @Column(name = "acordo_id", nullable = false)
    private UUID acordoId;

    @Column(name = "titulo_id", nullable = false)
    private UUID tituloId;

    @Column(nullable = false)
    private int ordem;

    protected ParcelaDoAcordo() {
        // exigido pelo JPA
    }

    public ParcelaDoAcordo(UUID acordoId, UUID tituloId, int ordem) {
        this.id = UUID.randomUUID();
        this.acordoId = acordoId;
        this.tituloId = tituloId;
        this.ordem = ordem;
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

    public int getOrdem() {
        return ordem;
    }
}
