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
 * Uma linha da história do cliente.
 *
 * Cobrança enviada, promessa feita, acordo fechado, ligação registrada,
 * comprovante recebido, contestação aberta. É o que sustenta a negociação e a
 * defesa da empresa se a conversa virar disputa.
 */
@Entity
@Table(name = "interacao")
public class Interacao {

    /** Os tipos que a tela oferece. */
    public static final java.util.List<String> TIPOS = java.util.List.of(
            "COBRANCA_ENVIADA", "PROMESSA", "ACORDO", "LIGACAO", "COMPROVANTE",
            "CONTESTACAO", "OBSERVACAO", "BLOQUEIO", "DESBLOQUEIO");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(nullable = false)
    private String tipo;

    private String descricao;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_prometida")
    private LocalDate dataPrometida;

    /** Para promessa: EM_ABERTO, CUMPRIDA ou QUEBRADA. */
    private String situacao;

    private String canal;

    private String autor;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    protected Interacao() {
        // exigido pelo JPA
    }

    public Interacao(UUID empresaId, UUID pagadorId, UUID unidadeId, String tipo,
                     String descricao, BigDecimal valor, LocalDate dataPrometida,
                     String canal, String autor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.unidadeId = unidadeId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.dataPrometida = dataPrometida;
        this.canal = canal;
        this.autor = autor;
        if ("PROMESSA".equals(tipo)) {
            this.situacao = "EM_ABERTO";
        }
    }

    public void marcarCumprida() {
        this.situacao = "CUMPRIDA";
    }

    public void marcarQuebrada() {
        this.situacao = "QUEBRADA";
    }

    /** Promessa que passou da data e ninguém baixou. */
    public boolean promessaVencida(LocalDate hoje) {
        return "PROMESSA".equals(tipo) && "EM_ABERTO".equals(situacao)
                && dataPrometida != null && dataPrometida.isBefore(hoje);
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

    public String getTipo() {
        return tipo;
    }

    /** O tipo escrito de um jeito que dá para ler. */
    public String getTipoLegivel() {
        return switch (tipo) {
            case "COBRANCA_ENVIADA" -> "Cobrança enviada";
            case "PROMESSA" -> "Promessa de pagamento";
            case "ACORDO" -> "Acordo";
            case "LIGACAO" -> "Ligação";
            case "COMPROVANTE" -> "Comprovante recebido";
            case "CONTESTACAO" -> "Contestação";
            case "BLOQUEIO" -> "Pedido de bloqueio";
            case "DESBLOQUEIO" -> "Pedido de desbloqueio";
            default -> "Observação";
        };
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataPrometida() {
        return dataPrometida;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getCanal() {
        return canal;
    }

    public String getAutor() {
        return autor;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
