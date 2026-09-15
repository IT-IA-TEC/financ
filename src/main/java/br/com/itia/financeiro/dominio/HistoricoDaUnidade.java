package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A memória das trocas de uma unidade: dono, telefone, nome, percentual.
 *
 * Identificar a unidade pelo código resolve metade do problema do cadastro.
 * A outra metade é saber quando ela mudou de mão, e por isso cada troca vira
 * uma linha aqui, com quem fez e por quê.
 */
@Entity
@Table(name = "historico_da_unidade")
public class HistoricoDaUnidade {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    /** DONO, TELEFONE, NOME, PERCENTUAL ou SITUACAO. */
    @Column(nullable = false)
    private String tipo;

    private String de;
    private String para;
    private String motivo;

    @Column(nullable = false)
    private OffsetDateTime quando = OffsetDateTime.now();

    private String quem;

    protected HistoricoDaUnidade() {
        // exigido pelo JPA
    }

    public HistoricoDaUnidade(UUID empresaId, UUID unidadeId, String tipo, String de,
                              String para, String motivo, String quem) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
        this.tipo = tipo;
        this.de = de;
        this.para = para;
        this.motivo = motivo;
        this.quem = quem;
    }

    /** A troca em uma linha, do jeito que a tela mostra. */
    public String getResumo() {
        String antes = de == null || de.isBlank() ? "vazio" : de;
        String depois = para == null || para.isBlank() ? "vazio" : para;
        return antes + " para " + depois;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDe() {
        return de;
    }

    public String getPara() {
        return para;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getQuando() {
        return quando;
    }

    public String getQuem() {
        return quem;
    }
}
