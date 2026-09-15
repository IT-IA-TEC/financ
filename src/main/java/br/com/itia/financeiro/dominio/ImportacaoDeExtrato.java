package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um arquivo de extrato que já entrou.
 *
 * A impressão digital é do conteúdo, e não do nome: renomear o arquivo não
 * engana o sistema. É o que garante que a mesma importação não entre duas
 * vezes e dobre o extrato do dia.
 */
@Entity
@Table(name = "importacao_extrato")
public class ImportacaoDeExtrato {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "conta_id")
    private UUID contaId;

    @Column(nullable = false)
    private String arquivo;

    @Column(nullable = false)
    private String impressao;

    @Column(nullable = false)
    private int linhas;

    @Column(nullable = false)
    private int novos;

    @Column(nullable = false)
    private int repetidos;

    @Column(nullable = false)
    private OffsetDateTime quando = OffsetDateTime.now();

    private String quem;

    protected ImportacaoDeExtrato() {
        // exigido pelo JPA
    }

    public ImportacaoDeExtrato(UUID empresaId, UUID contaId, String arquivo, String impressao,
                               String quem) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.contaId = contaId;
        this.arquivo = arquivo;
        this.impressao = impressao;
        this.quem = quem;
    }

    public void contar(int linhas, int novos, int repetidos) {
        this.linhas = linhas;
        this.novos = novos;
        this.repetidos = repetidos;
    }

    public UUID getId() {
        return id;
    }

    public String getArquivo() {
        return arquivo;
    }

    public int getLinhas() {
        return linhas;
    }

    public int getNovos() {
        return novos;
    }

    public int getRepetidos() {
        return repetidos;
    }

    public OffsetDateTime getQuando() {
        return quando;
    }

    public String getQuem() {
        return quem;
    }
}
