package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um disparo de cobrança feito de uma vez só.
 *
 * O lote nasce como prévia: mostra quem entra, quem fica de fora e por quê.
 * Nada sai antes de alguém confirmar. É essa parada no meio do caminho que
 * separa um disparo de uma bagunça.
 */
@Entity
@Table(name = "lote_mensagem")
public class LoteDeMensagem {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "modelo_id")
    private UUID modeloId;

    @Column(nullable = false)
    private String canal = "WHATSAPP";

    private String filtro;

    /** PREVIA, CONFIRMADO, ENVIANDO, CONCLUIDO ou CANCELADO. */
    @Column(nullable = false)
    private String situacao = "PREVIA";

    @Column(nullable = false)
    private int quantidade = 0;

    @Column(nullable = false)
    private int fora = 0;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "agendado_para")
    private OffsetDateTime agendadoPara;

    @Column(name = "confirmado_em")
    private OffsetDateTime confirmadoEm;

    @Column(name = "confirmado_por")
    private String confirmadoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected LoteDeMensagem() {
        // exigido pelo JPA
    }

    public LoteDeMensagem(UUID empresaId, String nome, UUID modeloId, String canal,
                          String filtro, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.nome = nome == null || nome.isBlank() ? "Disparo de cobrança" : nome.trim();
        this.modeloId = modeloId;
        this.canal = canal == null ? "WHATSAPP" : canal;
        this.filtro = filtro;
        this.criadoPor = criadoPor;
    }

    public void contar(int quantidade, int fora, BigDecimal valorTotal) {
        this.quantidade = quantidade;
        this.fora = fora;
        this.valorTotal = valorTotal == null ? BigDecimal.ZERO : valorTotal;
    }

    public void confirmar(String quem, OffsetDateTime agendadoPara) {
        if (!"PREVIA".equals(situacao)) {
            throw new IllegalStateException("Este lote já saiu da prévia.");
        }
        if (quantidade == 0) {
            throw new IllegalStateException("Não há ninguém neste lote para receber a mensagem.");
        }
        this.situacao = "CONFIRMADO";
        this.confirmadoEm = OffsetDateTime.now();
        this.confirmadoPor = quem;
        this.agendadoPara = agendadoPara;
    }

    public void concluir() {
        this.situacao = "CONCLUIDO";
    }

    public void cancelar() {
        if ("CONCLUIDO".equals(situacao)) {
            throw new IllegalStateException("Lote já concluído não pode ser cancelado.");
        }
        this.situacao = "CANCELADO";
    }

    public boolean naPrevia() {
        return "PREVIA".equals(situacao);
    }

    public boolean confirmado() {
        return "CONFIRMADO".equals(situacao) || "ENVIANDO".equals(situacao);
    }

    public String getSituacaoLegivel() {
        return switch (situacao) {
            case "PREVIA" -> "prévia";
            case "CONFIRMADO" -> "confirmado";
            case "ENVIANDO" -> "enviando";
            case "CONCLUIDO" -> "concluído";
            default -> "cancelado";
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

    public UUID getModeloId() {
        return modeloId;
    }

    public String getCanal() {
        return canal;
    }

    public String getFiltro() {
        return filtro;
    }

    public String getSituacao() {
        return situacao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getFora() {
        return fora;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public OffsetDateTime getAgendadoPara() {
        return agendadoPara;
    }

    public OffsetDateTime getConfirmadoEm() {
        return confirmadoEm;
    }

    public String getConfirmadoPor() {
        return confirmadoPor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
