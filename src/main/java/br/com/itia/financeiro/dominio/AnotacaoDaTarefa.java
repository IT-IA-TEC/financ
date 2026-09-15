package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do fio da tarefa.
 *
 * Comentário, dúvida, cobrança ou recado do próprio sistema. É aqui que fica
 * a conversa sobre a tarefa, para quem pegar depois entender o que houve.
 */
@Entity
@Table(name = "anotacao_da_tarefa")
public class AnotacaoDaTarefa {

    @Id
    private UUID id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 4000)
    private String texto;

    /** COMENTARIO, DUVIDA, COBRANCA ou SISTEMA. */
    @Column(nullable = false)
    private String tipo = "COMENTARIO";

    private String autor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected AnotacaoDaTarefa() {
        // exigido pelo JPA
    }

    public AnotacaoDaTarefa(UUID tarefaId, UUID empresaId, String texto, String tipo,
                            String autor) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escreva alguma coisa antes de mandar.");
        }
        this.id = UUID.randomUUID();
        this.tarefaId = tarefaId;
        this.empresaId = empresaId;
        this.texto = texto.trim();
        this.tipo = tipo == null ? "COMENTARIO" : tipo;
        this.autor = autor;
    }

    public boolean doSistema() {
        return "SISTEMA".equals(tipo);
    }

    public String getTipoLegivel() {
        return switch (tipo) {
            case "DUVIDA" -> "dúvida";
            case "COBRANCA" -> "cobrança";
            case "SISTEMA" -> "registro";
            default -> "comentário";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public String getTexto() {
        return texto;
    }

    public String getTipo() {
        return tipo;
    }

    public String getAutor() {
        return autor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
