package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Um arquivo anexado à pessoa, à unidade ou a um contrato. */
@Entity
@Table(name = "documento")
public class Documento {

    /** Os tipos que a tela oferece. */
    public static final java.util.List<String> TIPOS = java.util.List.of(
            "CONTRATO", "PROCURACAO", "CARTAO CNPJ", "COMPROVANTE DE ENDERECO",
            "COMPROVANTE DE PAGAMENTO", "NOTA FISCAL", "TERMO DE ACORDO", "OUTRO");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "contrato_id")
    private UUID contratoId;

    /** Quando o anexo e o comprovante de um atendimento. */
    @Column(name = "realizado_id")
    private UUID realizadoId;

    /** Quando o anexo e o boleto, a nota ou o comprovante de uma conta a pagar. */
    @Column(name = "obrigacao_id")
    private UUID obrigacaoId;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String caminho;

    private Long tamanho;

    private LocalDate validade;

    private String observacao;

    @Column(name = "anexado_por")
    private String anexadoPor;

    @Column(name = "anexado_em", nullable = false)
    private OffsetDateTime anexadoEm = OffsetDateTime.now();

    protected Documento() {
        // exigido pelo JPA
    }

    public Documento(UUID empresaId, UUID pagadorId, UUID unidadeId, UUID contratoId,
                     String tipo, String nomeArquivo, String caminho, Long tamanho,
                     LocalDate validade, String observacao, String anexadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.unidadeId = unidadeId;
        this.contratoId = contratoId;
        this.tipo = tipo;
        this.nomeArquivo = nomeArquivo;
        this.caminho = caminho;
        this.tamanho = tamanho;
        this.validade = validade;
        this.observacao = observacao;
        this.anexadoPor = anexadoPor;
    }

    public void vincularAoAtendimento(UUID realizadoId) {
        this.realizadoId = realizadoId;
    }

    public UUID getRealizadoId() {
        return realizadoId;
    }

    public void vincularAObrigacao(UUID obrigacaoId) {
        this.obrigacaoId = obrigacaoId;
    }

    public UUID getObrigacaoId() {
        return obrigacaoId;
    }

    public boolean venceu(LocalDate hoje) {
        return validade != null && validade.isBefore(hoje);
    }

    public String getTamanhoLegivel() {
        if (tamanho == null) {
            return "";
        }
        if (tamanho < 1024) {
            return tamanho + " B";
        }
        if (tamanho < 1024 * 1024) {
            return (tamanho / 1024) + " KB";
        }
        return String.format("%.1f MB", tamanho / (1024.0 * 1024.0));
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

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getCaminho() {
        return caminho;
    }

    public Long getTamanho() {
        return tamanho;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getAnexadoPor() {
        return anexadoPor;
    }

    public OffsetDateTime getAnexadoEm() {
        return anexadoEm;
    }
}
