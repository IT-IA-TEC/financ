package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma prova de pagamento que chegou, esperando conferência.
 *
 * O comprovante não dá baixa sozinho. Ele guarda o que foi lido, e a baixa só
 * acontece quando alguém confirma que o dinheiro caiu na conta certa. É por
 * isso que o destino lido fica gravado: é ele que separa pagamento de print
 * bonito.
 */
@Entity
@Table(name = "comprovante")
public class Comprovante {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "documento_id")
    private UUID documentoId;

    /** DIGITADO, WHATSAPP ou IMPORTADO. */
    @Column(nullable = false)
    private String origem = "DIGITADO";

    /** O texto do comprovante, do jeito que chegou. */
    private String texto;

    @Column(name = "valor_lido", precision = 14, scale = 2)
    private BigDecimal valorLido;

    @Column(name = "data_lida")
    private LocalDate dataLida;

    @Column(name = "destino_lido")
    private String destinoLido;

    @Column(name = "identificador_lido")
    private String identificadorLido;

    /** NA_FILA, CONFERIDO ou RECUSADO. */
    @Column(nullable = false)
    private String situacao = "NA_FILA";

    @Column(name = "titulo_id")
    private UUID tituloId;

    private String motivo;

    @Column(name = "conferido_em")
    private OffsetDateTime conferidoEm;

    @Column(name = "conferido_por")
    private String conferidoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Comprovante() {
        // exigido pelo JPA
    }

    public Comprovante(UUID empresaId, UUID pagadorId, String origem, String texto,
                       String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.origem = origem == null ? "DIGITADO" : origem;
        this.texto = texto;
        this.criadoPor = criadoPor;
    }

    public void guardarLeitura(BigDecimal valor, LocalDate data, String destino,
                               String identificador) {
        this.valorLido = valor;
        this.dataLida = data;
        this.destinoLido = destino;
        this.identificadorLido = identificador;
    }

    public void guardarArquivo(UUID documentoId, UUID unidadeId) {
        this.documentoId = documentoId;
        this.unidadeId = unidadeId;
    }

    public void conferir(UUID tituloId, String quem) {
        this.situacao = "CONFERIDO";
        this.tituloId = tituloId;
        this.conferidoEm = OffsetDateTime.now();
        this.conferidoPor = quem;
    }

    public void recusar(String motivo, String quem) {
        this.situacao = "RECUSADO";
        this.motivo = motivo;
        this.conferidoEm = OffsetDateTime.now();
        this.conferidoPor = quem;
    }

    public void voltarParaFila() {
        this.situacao = "NA_FILA";
        this.motivo = null;
        this.tituloId = null;
        this.conferidoEm = null;
        this.conferidoPor = null;
    }

    public boolean naFila() {
        return "NA_FILA".equals(situacao);
    }

    /** O que falta neste comprovante para poder virar baixa. */
    public String oQueFalta() {
        if (valorLido == null || valorLido.signum() <= 0) {
            return "sem valor";
        }
        if (identificadorLido == null || identificadorLido.isBlank()) {
            return "sem identificador da transação";
        }
        if (destinoLido == null || destinoLido.isBlank()) {
            return "sem destino";
        }
        return null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagadorId() {
        return pagadorId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public String getOrigem() {
        return origem;
    }

    public String getTexto() {
        return texto;
    }

    public BigDecimal getValorLido() {
        return valorLido;
    }

    public LocalDate getDataLida() {
        return dataLida;
    }

    public String getDestinoLido() {
        return destinoLido;
    }

    public String getIdentificadorLido() {
        return identificadorLido;
    }

    public String getSituacao() {
        return situacao;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getConferidoEm() {
        return conferidoEm;
    }

    public String getConferidoPor() {
        return conferidoPor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
