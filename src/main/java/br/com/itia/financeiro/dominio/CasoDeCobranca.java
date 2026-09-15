package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O caso de cobrança de um cliente.
 *
 * A faixa de atraso não mora aqui: ela é calculada do vencimento, e ninguém
 * arrasta caso de coluna na mão. O que fica guardado é o que o sistema não tem
 * como saber sozinho: se o caso está em acordo, contestado, parado esperando
 * alguém, ou já no jurídico.
 */
@Entity
@Table(name = "caso_de_cobranca")
public class CasoDeCobranca {

    public static final List<String> SITUACOES = List.of(
            "EM_COBRANCA", "PROMESSA", "EM_ACORDO", "CONTESTADO", "PARADO", "JURIDICO");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(nullable = false)
    private String situacao = "EM_COBRANCA";

    private String responsavel;

    @Column(name = "proxima_acao")
    private String proximaAcao;

    @Column(name = "proxima_data")
    private LocalDate proximaData;

    @Column(length = 1000)
    private String observacao;

    /** Até quando a cobrança automática fica parada para este cliente. */
    @Column(name = "pausada_ate")
    private LocalDate pausadaAte;

    @Column(name = "motivo_da_pausa")
    private String motivoDaPausa;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    protected CasoDeCobranca() {
        // exigido pelo JPA
    }

    public CasoDeCobranca(UUID empresaId, UUID unidadeId) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
    }

    public void ajustar(String situacao, String responsavel, String proximaAcao,
                        LocalDate proximaData, String observacao, String quem) {
        if (situacao != null && !SITUACOES.contains(situacao)) {
            throw new IllegalArgumentException("Situação desconhecida para o caso.");
        }
        this.situacao = situacao == null ? this.situacao : situacao;
        this.responsavel = responsavel;
        this.proximaAcao = proximaAcao;
        this.proximaData = proximaData;
        this.observacao = observacao;
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    /**
     * Se este caso pode receber cobrança automática.
     *
     * Quem já combinou pagamento ou contestou a conta não leva mensagem da
     * régua: é assim que se perde um cliente que estava resolvendo.
     */
    public boolean aceitaCobrancaAutomatica() {
        return aceitaCobrancaAutomatica(LocalDate.now());
    }

    public boolean aceitaCobrancaAutomatica(LocalDate hoje) {
        if (pausadaAte != null && !pausadaAte.isBefore(hoje)) {
            return false;
        }
        return !"EM_ACORDO".equals(situacao) && !"CONTESTADO".equals(situacao)
                && !"JURIDICO".equals(situacao);
    }

    /**
     * Para a cobrança automática até tal dia.
     *
     * A data de fim é obrigatória de propósito: régua parada para sempre é
     * dívida esquecida.
     */
    public void pausar(LocalDate ate, String motivo, String quem) {
        if (ate == null || ate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Diga até quando a régua fica parada, com uma data daqui para frente.");
        }
        this.pausadaAte = ate;
        this.motivoDaPausa = motivo;
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    public void voltarACobrar(String quem) {
        this.pausadaAte = null;
        this.motivoDaPausa = null;
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    public boolean pausada(LocalDate hoje) {
        return pausadaAte != null && !pausadaAte.isBefore(hoje);
    }

    public LocalDate getPausadaAte() {
        return pausadaAte;
    }

    public String getMotivoDaPausa() {
        return motivoDaPausa;
    }

    public boolean atrasado(LocalDate hoje) {
        return proximaData != null && proximaData.isBefore(hoje);
    }

    public String getSituacaoLegivel() {
        return switch (situacao) {
            case "PROMESSA" -> "promessa de pagamento";
            case "EM_ACORDO" -> "em acordo";
            case "CONTESTADO" -> "contestado";
            case "PARADO" -> "parado";
            case "JURIDICO" -> "jurídico";
            default -> "em cobrança";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getProximaAcao() {
        return proximaAcao;
    }

    public LocalDate getProximaData() {
        return proximaData;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }
}
