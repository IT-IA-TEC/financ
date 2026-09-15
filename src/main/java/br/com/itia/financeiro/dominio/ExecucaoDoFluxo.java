package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O registro de que um fluxo agiu, ou de por que não agiu.
 *
 * Automação sem registro é caixa preta: ninguém descobre por que o cliente
 * recebeu, ou deixou de receber, uma mensagem.
 */
@Entity
@Table(name = "execucao_do_fluxo")
public class ExecucaoDoFluxo {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "fluxo_id", nullable = false)
    private UUID fluxoId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "titulo_id")
    private UUID tituloId;

    /** FEZ ou NAO_FEZ. */
    @Column(nullable = false)
    private String resultado;

    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    protected ExecucaoDoFluxo() {
        // exigido pelo JPA
    }

    public ExecucaoDoFluxo(UUID empresaId, UUID fluxoId, UUID unidadeId, UUID tituloId,
                           boolean fez, String detalhe) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.fluxoId = fluxoId;
        this.unidadeId = unidadeId;
        this.tituloId = tituloId;
        this.resultado = fez ? "FEZ" : "NAO_FEZ";
        this.detalhe = detalhe;
    }

    public boolean fez() {
        return "FEZ".equals(resultado);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFluxoId() {
        return fluxoId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public String getResultado() {
        return resultado;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
