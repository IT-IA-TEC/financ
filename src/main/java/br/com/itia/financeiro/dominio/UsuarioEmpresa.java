package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * O cracha: diz que esta pessoa cuida desta empresa, e com que alcada.
 * Sem uma linha aqui, a pessoa nao enxerga nada daquela empresa.
 */
@Entity
@Table(name = "usuario_empresa")
public class UsuarioEmpresa {

    @EmbeddedId
    private Chave chave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel = Papel.OPERADOR;

    protected UsuarioEmpresa() {
        // exigido pelo JPA
    }

    public UsuarioEmpresa(UUID usuarioId, UUID empresaId, Papel papel) {
        this.chave = new Chave(usuarioId, empresaId);
        this.papel = papel;
    }

    public UUID getUsuarioId() {
        return chave.usuarioId;
    }

    public UUID getEmpresaId() {
        return chave.empresaId;
    }

    public Papel getPapel() {
        return papel;
    }

    /** Alcada: quem pode fazer o que mexe em dinheiro. */
    public enum Papel {
        /** Lanca titulo, recebe pagamento, conversa com o cliente. */
        OPERADOR,
        /** Tudo do operador, mais cancelar titulo e dar baixa manual. */
        GESTOR,
        /** Tudo, mais fechar competencia e mexer na configuracao da empresa. */
        DIRETOR;

        public boolean podeCancelarTitulo() {
            return this != OPERADOR;
        }

        public boolean podeFecharCompetencia() {
            return this == DIRETOR;
        }
    }

    @Embeddable
    public static class Chave implements Serializable {

        @Column(name = "usuario_id")
        private UUID usuarioId;

        @Column(name = "empresa_id")
        private UUID empresaId;

        protected Chave() {
            // exigido pelo JPA
        }

        Chave(UUID usuarioId, UUID empresaId) {
            this.usuarioId = usuarioId;
            this.empresaId = empresaId;
        }

        @Override
        public boolean equals(Object outro) {
            if (this == outro) {
                return true;
            }
            if (!(outro instanceof Chave chave)) {
                return false;
            }
            return Objects.equals(usuarioId, chave.usuarioId)
                    && Objects.equals(empresaId, chave.empresaId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, empresaId);
        }
    }
}
