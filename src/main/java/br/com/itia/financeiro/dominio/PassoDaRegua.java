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
 * Um passo da régua de cobrança.
 *
 * A régua é a resposta para "quando a gente fala com quem deve". Cada passo
 * diz a distância até o vencimento e o texto que sai naquele dia. Nada aqui
 * escolhe data solta: é sempre relativo ao vencimento do documento, e por isso
 * a régua vale para qualquer cliente, sem ninguém ficar marcando agenda.
 */
@Entity
@Table(name = "passo_da_regua")
public class PassoDaRegua {

    public static final List<String> GATILHOS =
            List.of("ANTES_DE_VENCER", "NO_VENCIMENTO", "DEPOIS_DE_VENCER");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String gatilho = "DEPOIS_DE_VENCER";

    @Column(nullable = false)
    private int dias = 0;

    @Column(name = "modelo_id", nullable = false)
    private UUID modeloId;

    @Column(nullable = false)
    private String canal = "WHATSAPP";

    @Column(nullable = false)
    private int ordem = 1;

    @Column(name = "exige_confirmacao", nullable = false)
    private boolean exigeConfirmacao = true;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultima_rodada")
    private LocalDate ultimaRodada;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected PassoDaRegua() {
        // exigido pelo JPA
    }

    public PassoDaRegua(UUID empresaId, String nome, String gatilho, int dias, UUID modeloId,
                        String canal, int ordem, boolean exigeConfirmacao, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.modeloId = modeloId;
        this.criadoPor = criadoPor;
        ajustar(nome, gatilho, dias, modeloId, canal, ordem, exigeConfirmacao, true);
    }

    public void ajustar(String nome, String gatilho, int dias, UUID modeloId, String canal,
                        int ordem, boolean exigeConfirmacao, boolean ativo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O passo precisa de um nome.");
        }
        if (modeloId == null) {
            throw new IllegalArgumentException("Escolha o texto que este passo manda.");
        }
        if (dias < 0) {
            throw new IllegalArgumentException("Os dias contam sempre para frente, sem sinal.");
        }
        if ("NO_VENCIMENTO".equals(gatilho) && dias != 0) {
            throw new IllegalArgumentException(
                    "No vencimento é o próprio dia: deixe os dias em zero.");
        }
        this.nome = nome.trim();
        this.gatilho = gatilho == null ? "DEPOIS_DE_VENCER" : gatilho;
        this.dias = dias;
        this.modeloId = modeloId;
        this.canal = canal == null ? "WHATSAPP" : canal;
        this.ordem = ordem;
        this.exigeConfirmacao = exigeConfirmacao;
        this.ativo = ativo;
    }

    /**
     * O dia em que este passo fala sobre um documento que vence em tal data.
     */
    public LocalDate diaDeFalar(LocalDate vencimento) {
        return switch (gatilho) {
            case "ANTES_DE_VENCER" -> vencimento.minusDays(dias);
            case "NO_VENCIMENTO" -> vencimento;
            default -> vencimento.plusDays(dias);
        };
    }

    /** Se hoje é o dia deste passo para este vencimento. */
    public boolean valeHoje(LocalDate vencimento, LocalDate hoje) {
        return ativo && diaDeFalar(vencimento).isEqual(hoje);
    }

    public void anotarRodada(LocalDate quando) {
        this.ultimaRodada = quando;
    }

    public String getQuandoLegivel() {
        return switch (gatilho) {
            case "ANTES_DE_VENCER" -> dias + " dia(s) antes de vencer";
            case "NO_VENCIMENTO" -> "no dia do vencimento";
            default -> dias == 0 ? "no dia seguinte ao vencimento"
                    : dias + " dia(s) depois de vencer";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getNome() {
        return nome;
    }

    public String getGatilho() {
        return gatilho;
    }

    public int getDias() {
        return dias;
    }

    public UUID getModeloId() {
        return modeloId;
    }

    public String getCanal() {
        return canal;
    }

    public int getOrdem() {
        return ordem;
    }

    public boolean isExigeConfirmacao() {
        return exigeConfirmacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public LocalDate getUltimaRodada() {
        return ultimaRodada;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
