package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma empresa atendida pelo financeiro.
 *
 * Tudo neste sistema pendura numa empresa. Quem cuida de duas empresas ve as
 * duas, mas nunca no mesmo lugar: troca de empresa e troca de tela inteira.
 */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    private UUID id;

    /** Apelido curto que aparece na barra: you, 40, realizze. */
    @Column(nullable = false, unique = true)
    private String apelido;

    @Column(nullable = false)
    private String nome;

    private String cnpj;

    /** Chave PIX que recebe o dinheiro desta empresa. */
    @Column(name = "chave_pix")
    private String chavePix;

    /** Com qual setor do ERP esta empresa conversa. */
    @Column(name = "erp_setor_id")
    private UUID erpSetorId;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    protected Empresa() {
        // exigido pelo JPA
    }

    public Empresa(String apelido, String nome, String cnpj, String chavePix) {
        this.id = UUID.randomUUID();
        this.apelido = apelido;
        this.nome = nome;
        this.cnpj = cnpj;
        this.chavePix = chavePix;
    }

    /** Ajusta os dados da empresa pela tela de configuracao. */
    public void atualizar(String apelido, String nome, String cnpj, String chavePix) {
        this.apelido = apelido;
        this.nome = nome;
        this.cnpj = cnpj;
        this.chavePix = chavePix;
    }

    public void reativar() {
        this.ativa = true;
    }

    public UUID getId() {
        return id;
    }

    public String getApelido() {
        return apelido;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getChavePix() {
        return chavePix;
    }

    public UUID getErpSetorId() {
        return erpSetorId;
    }

    public void setErpSetorId(UUID erpSetorId) {
        this.erpSetorId = erpSetorId;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void desativar() {
        this.ativa = false;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }
}
