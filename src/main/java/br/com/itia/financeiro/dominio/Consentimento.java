package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O registro de que se pode falar com a pessoa por aquele canal, e com que base.
 *
 * Não é burocracia: é o que protege a empresa numa reclamação. Guarda também a
 * recusa e a revogação, porque provar que alguém pediu para não ser procurado é
 * tão importante quanto provar que aceitou.
 */
@Entity
@Table(name = "consentimento")
public class Consentimento {

    public static final java.util.List<String> CANAIS =
            java.util.List.of("WHATSAPP", "TELEFONE", "E-MAIL", "SMS", "CARTA");

    public static final java.util.List<String> BASES =
            java.util.List.of("EXECUCAO DE CONTRATO", "LEGITIMO INTERESSE",
                    "CONSENTIMENTO", "OBRIGACAO LEGAL");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id", nullable = false)
    private UUID pagadorId;

    @Column(name = "contato_id")
    private UUID contatoId;

    @Column(nullable = false)
    private String canal;

    @Column(name = "base_legal", nullable = false)
    private String baseLegal;

    /** ACEITO, RECUSADO ou REVOGADO. */
    @Column(nullable = false)
    private String situacao = "ACEITO";

    private String origem;

    private String observacao;

    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm = OffsetDateTime.now();

    @Column(name = "registrado_por")
    private String registradoPor;

    protected Consentimento() {
        // exigido pelo JPA
    }

    public Consentimento(UUID empresaId, UUID pagadorId, UUID contatoId, String canal,
                         String baseLegal, String situacao, String origem, String observacao,
                         String registradoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.contatoId = contatoId;
        this.canal = canal;
        this.baseLegal = baseLegal;
        this.situacao = situacao == null ? "ACEITO" : situacao;
        this.origem = origem;
        this.observacao = observacao;
        this.registradoPor = registradoPor;
    }

    public boolean permite() {
        return "ACEITO".equals(situacao);
    }

    public UUID getId() {
        return id;
    }

    public String getCanal() {
        return canal;
    }

    public String getBaseLegal() {
        return baseLegal;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getOrigem() {
        return origem;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getRegistradoEm() {
        return registradoEm;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }
}
