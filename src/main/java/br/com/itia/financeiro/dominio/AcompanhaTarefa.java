package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Quem acompanha uma tarefa sem ser quem faz nem quem pediu.
 *
 * E a aba "Faço parte": a pessoa quer saber o que acontece, mas nao e dela a
 * responsabilidade de fazer.
 */
@Entity
@Table(name = "acompanha_tarefa")
public class AcompanhaTarefa {

    @Id
    private UUID id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String quem;

    protected AcompanhaTarefa() {
        // exigido pelo JPA
    }

    public AcompanhaTarefa(UUID tarefaId, UUID empresaId, String quem) {
        this.id = UUID.randomUUID();
        this.tarefaId = tarefaId;
        this.empresaId = empresaId;
        this.quem = quem;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public String getQuem() {
        return quem;
    }
}
