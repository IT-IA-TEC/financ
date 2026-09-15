package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trilha de auditoria: quem mexeu, em que, quando e o que mudou.
 * Toda mudanca de dinheiro grava uma linha aqui. Nada e apagado.
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(nullable = false)
    private String acao;

    private String autor;

    @Column(columnDefinition = "jsonb")
    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    protected Evento() {
        // exigido pelo JPA
    }

    public Evento(UUID empresaId, String entidade, UUID entidadeId,
                  String acao, String autor, String detalhe) {
        this.empresaId = empresaId;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.acao = acao;
        this.autor = autor;
        this.detalhe = detalhe;
    }

    public Long getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEntidade() {
        return entidade;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public String getAcao() {
        return acao;
    }

    public String getAutor() {
        return autor;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
