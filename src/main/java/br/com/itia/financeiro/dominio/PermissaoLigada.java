package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma chave do modelo de inteligência, nesta empresa.
 *
 * Guarda quem ligou e quando. Permissão sem dono é permissão que ninguém
 * assume: quando alguém perguntar "quem deixou o robô fazer isso", a resposta
 * tem que estar escrita.
 */
@Entity
@Table(name = "permissao_ia")
public class PermissaoLigada {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    /** LEITURA ou ACAO. */
    @Column(nullable = false)
    private String especie;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private boolean ligada = false;

    @Column(name = "ligada_em")
    private OffsetDateTime ligadaEm;

    @Column(name = "ligada_por")
    private String ligadaPor;

    protected PermissaoLigada() {
        // exigido pelo JPA
    }

    public PermissaoLigada(UUID empresaId, PermissaoDaIa permissao) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.especie = permissao.getEspecie().name();
        this.chave = permissao.name();
    }

    public void ligar(String quem) {
        this.ligada = true;
        this.ligadaEm = OffsetDateTime.now();
        this.ligadaPor = quem;
    }

    public void desligar(String quem) {
        this.ligada = false;
        this.ligadaEm = OffsetDateTime.now();
        this.ligadaPor = quem;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEspecie() {
        return especie;
    }

    public String getChave() {
        return chave;
    }

    public boolean isLigada() {
        return ligada;
    }

    public OffsetDateTime getLigadaEm() {
        return ligadaEm;
    }

    public String getLigadaPor() {
        return ligadaPor;
    }
}
