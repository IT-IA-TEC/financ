package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O registro de um atendimento do agente.
 *
 * Guarda a pergunta, a intenção entendida e a resposta. Sem isso ninguém
 * consegue conferir se o agente respondeu certo, e um agente que ninguém
 * confere é um risco solto falando com cliente.
 */
@Entity
@Table(name = "atendimento_do_agente")
public class AtendimentoDoAgente {

    public static final List<String> INTENCOES = List.of(
            "QUANTO_DEVO", "COMO_PAGO", "JA_PAGUEI", "VOU_PAGAR", "QUERO_PARCELAR",
            "RECLAMACAO", "NAO_ENTENDI");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "entrada_id")
    private UUID entradaId;

    @Column(name = "resposta_id")
    private UUID respostaId;

    @Column(length = 1000)
    private String pergunta;

    @Column(nullable = false)
    private String intencao;

    @Column(nullable = false)
    private boolean escalado = false;

    private String motivo;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    protected AtendimentoDoAgente() {
        // exigido pelo JPA
    }

    public AtendimentoDoAgente(UUID empresaId, UUID unidadeId, UUID entradaId, String pergunta,
                               String intencao) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
        this.entradaId = entradaId;
        this.pergunta = pergunta;
        this.intencao = intencao;
    }

    public void respondeu(UUID respostaId) {
        this.respostaId = respostaId;
    }

    public void escalou(String motivo) {
        this.escalado = true;
        this.motivo = motivo;
    }

    public String getIntencaoLegivel() {
        return switch (intencao) {
            case "QUANTO_DEVO" -> "quanto eu devo";
            case "COMO_PAGO" -> "como eu pago";
            case "JA_PAGUEI" -> "já paguei";
            case "VOU_PAGAR" -> "vou pagar tal dia";
            case "QUERO_PARCELAR" -> "quero parcelar";
            case "RECLAMACAO" -> "reclamação";
            default -> "não entendi";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public UUID getEntradaId() {
        return entradaId;
    }

    public UUID getRespostaId() {
        return respostaId;
    }

    public String getPergunta() {
        return pergunta;
    }

    public String getIntencao() {
        return intencao;
    }

    public boolean isEscalado() {
        return escalado;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
