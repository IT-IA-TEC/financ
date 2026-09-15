package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma mensagem de cobrança, de ida ou de volta.
 *
 * O texto já vem montado e gravado aqui. É esta linha que responde "o que foi
 * dito, para quem, quando e por qual canal", e é ela que a conversa do
 * WhatsApp lê para montar a tela.
 */
@Entity
@Table(name = "mensagem")
public class Mensagem {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "lote_id")
    private UUID loteId;

    @Column(name = "modelo_id")
    private UUID modeloId;

    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "contato_id")
    private UUID contatoId;

    @Column(name = "titulo_id")
    private UUID tituloId;

    @Column(name = "cobranca_id")
    private UUID cobrancaId;

    /** WHATSAPP ou EMAIL. */
    @Column(nullable = false)
    private String canal = "WHATSAPP";

    /** SAIDA ou ENTRADA. */
    @Column(nullable = false)
    private String direcao = "SAIDA";

    private String destino;

    private String assunto;

    @Column(nullable = false, length = 4000)
    private String corpo;

    /** NA_FILA, ENVIADA, ENTREGUE, LIDA, RESPONDIDA, FALHOU ou CANCELADA. */
    @Column(nullable = false)
    private String situacao = "NA_FILA";

    private String motivo;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "agendada_para")
    private OffsetDateTime agendadaPara;

    @Column(name = "enviada_em")
    private OffsetDateTime enviadaEm;

    @Column(name = "entregue_em")
    private OffsetDateTime entregueEm;

    @Column(name = "lida_em")
    private OffsetDateTime lidaEm;

    @Column(name = "respondida_em")
    private OffsetDateTime respondidaEm;

    @Column(name = "id_externo")
    private String idExterno;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Mensagem() {
        // exigido pelo JPA
    }

    public Mensagem(UUID empresaId, String canal, String direcao, String destino,
                    String corpo, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.canal = canal == null ? "WHATSAPP" : canal;
        this.direcao = direcao == null ? "SAIDA" : direcao;
        this.destino = destino;
        this.corpo = corpo;
        this.criadoPor = criadoPor;
    }

    public void ligarAoLote(UUID loteId, UUID modeloId) {
        this.loteId = loteId;
        this.modeloId = modeloId;
    }

    public void ligarAoCliente(UUID pagadorId, UUID unidadeId, UUID contatoId) {
        this.pagadorId = pagadorId;
        this.unidadeId = unidadeId;
        this.contatoId = contatoId;
    }

    public void ligarAoDocumento(UUID tituloId, UUID cobrancaId) {
        this.tituloId = tituloId;
        this.cobrancaId = cobrancaId;
    }

    public void agendar(OffsetDateTime quando) {
        this.agendadaPara = quando;
    }

    public void definirAssunto(String assunto) {
        this.assunto = assunto;
    }

    public void marcarEnviada(String idExterno) {
        this.situacao = "ENVIADA";
        this.enviadaEm = OffsetDateTime.now();
        this.tentativas = this.tentativas + 1;
        this.idExterno = idExterno;
        this.motivo = null;
    }

    public void guardarIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public void marcarEntregue() {
        if (enviadaEm == null) {
            this.enviadaEm = OffsetDateTime.now();
        }
        this.situacao = "ENTREGUE";
        this.entregueEm = OffsetDateTime.now();
    }

    public void marcarLida() {
        this.situacao = "LIDA";
        this.lidaEm = OffsetDateTime.now();
    }

    public void marcarRespondida() {
        this.situacao = "RESPONDIDA";
        this.respondidaEm = OffsetDateTime.now();
    }

    public void marcarFalha(String motivo) {
        this.situacao = "FALHOU";
        this.motivo = motivo;
        this.tentativas = this.tentativas + 1;
    }

    public void cancelar(String motivo) {
        if (!"NA_FILA".equals(situacao)) {
            throw new IllegalStateException("Só dá para cancelar mensagem que ainda está na fila.");
        }
        this.situacao = "CANCELADA";
        this.motivo = motivo;
    }

    public void voltarParaFila() {
        this.situacao = "NA_FILA";
        this.motivo = null;
    }

    public boolean naFila() {
        return "NA_FILA".equals(situacao);
    }

    public boolean saiu() {
        return "ENVIADA".equals(situacao) || "ENTREGUE".equals(situacao)
                || "LIDA".equals(situacao) || "RESPONDIDA".equals(situacao);
    }

    public boolean deEntrada() {
        return "ENTRADA".equals(direcao);
    }

    public String getSituacaoLegivel() {
        return switch (situacao) {
            case "NA_FILA" -> "na fila";
            case "ENVIADA" -> "enviada";
            case "ENTREGUE" -> "entregue";
            case "LIDA" -> "lida";
            case "RESPONDIDA" -> "respondida";
            case "FALHOU" -> "falhou";
            default -> "cancelada";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public UUID getModeloId() {
        return modeloId;
    }

    public UUID getPagadorId() {
        return pagadorId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public UUID getContatoId() {
        return contatoId;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public UUID getCobrancaId() {
        return cobrancaId;
    }

    public String getCanal() {
        return canal;
    }

    public String getDirecao() {
        return direcao;
    }

    public String getDestino() {
        return destino;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getCorpo() {
        return corpo;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public int getTentativas() {
        return tentativas;
    }

    public OffsetDateTime getAgendadaPara() {
        return agendadaPara;
    }

    public OffsetDateTime getEnviadaEm() {
        return enviadaEm;
    }

    public OffsetDateTime getEntregueEm() {
        return entregueEm;
    }

    public OffsetDateTime getLidaEm() {
        return lidaEm;
    }

    public OffsetDateTime getRespondidaEm() {
        return respondidaEm;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
