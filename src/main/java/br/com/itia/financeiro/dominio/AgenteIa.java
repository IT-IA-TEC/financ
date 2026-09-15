package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O modelo de inteligência ligado a esta empresa.
 *
 * Nasce desligado e no modo mais fraco: só sugere, não fala com ninguém. Subir
 * o modo é uma decisão consciente de quem cuida da empresa, e o teto de valor
 * e o teto de mensagens continuam valendo em qualquer modo.
 */
@Entity
@Table(name = "agente_ia")
public class AgenteIa {

    public static final List<String> MODOS =
            List.of("SO_SUGERE", "RESPONDE_COM_REVISAO", "RESPONDE_SOZINHO");

    @Id
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "integracao_id")
    private UUID integracaoId;

    private String modelo;

    @Column(nullable = false)
    private boolean ativo = false;

    @Column(nullable = false)
    private String modo = "SO_SUGERE";

    @Column(length = 4000)
    private String instrucao;

    @Column(name = "teto_valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal tetoValor = BigDecimal.ZERO;

    @Column(name = "teto_mensagens_dia", nullable = false)
    private int tetoMensagensDia = 1;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    protected AgenteIa() {
        // exigido pelo JPA
    }

    public AgenteIa(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public void ajustar(UUID integracaoId, String modelo, boolean ativo, String modo,
                        String instrucao, BigDecimal tetoValor, int tetoMensagensDia,
                        String quem) {
        if (modo != null && !MODOS.contains(modo)) {
            throw new IllegalArgumentException("Modo desconhecido para o modelo.");
        }
        if (ativo && integracaoId == null) {
            throw new IllegalStateException(
                    "Escolha a integração do modelo antes de ligar.");
        }
        if (tetoMensagensDia < 0 || tetoMensagensDia > 20) {
            throw new IllegalArgumentException(
                    "O teto de mensagens por dia vai de zero a vinte.");
        }
        this.integracaoId = integracaoId;
        this.modelo = modelo;
        this.ativo = ativo;
        this.modo = modo == null ? "SO_SUGERE" : modo;
        this.instrucao = instrucao;
        this.tetoValor = tetoValor == null ? BigDecimal.ZERO : tetoValor;
        this.tetoMensagensDia = tetoMensagensDia;
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    /** Se o modelo pode falar direto com o cliente, sem ninguém revisar. */
    public boolean falaSozinho() {
        return ativo && "RESPONDE_SOZINHO".equals(modo);
    }

    /** Se ele pode escrever a mensagem para alguém revisar e mandar. */
    public boolean podeEscrever() {
        return ativo && !"SO_SUGERE".equals(modo);
    }

    public boolean dentroDoTeto(BigDecimal valor) {
        return valor == null || tetoValor.signum() == 0 || valor.compareTo(tetoValor) <= 0;
    }

    public String getModoLegivel() {
        return switch (modo) {
            case "RESPONDE_COM_REVISAO" -> "escreve, alguém revisa e manda";
            case "RESPONDE_SOZINHO" -> "responde sozinho";
            default -> "só sugere, não fala com ninguém";
        };
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public UUID getIntegracaoId() {
        return integracaoId;
    }

    public String getModelo() {
        return modelo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getModo() {
        return modo;
    }

    public String getInstrucao() {
        return instrucao;
    }

    public BigDecimal getTetoValor() {
        return tetoValor;
    }

    public int getTetoMensagensDia() {
        return tetoMensagensDia;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }
}
