package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Quem vai receber o dinheiro.
 *
 * Fornecedor é um papel de uma pessoa ou empresa, e não uma categoria de
 * despesa. O que foi comprado fica na natureza, dentro da composição.
 */
@Entity
@Table(name = "favorecido")
public class Favorecido {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nome;

    private String documento;

    /** PESSOA, EMPRESA, FUNCIONARIO, SOCIO ou ORGAO_PUBLICO. */
    @Column(nullable = false)
    private String tipo = "EMPRESA";

    @Column(name = "chave_pix")
    private String chavePix;

    private String banco;
    private String agencia;
    private String conta;
    private String observacao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Favorecido() {
        // exigido pelo JPA
    }

    public Favorecido(Empresa empresa, String nome, String documento, String tipo) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.nome = nome;
        this.documento = documento;
        this.tipo = tipo == null ? "EMPRESA" : tipo;
    }

    public void ajustar(String nome, String documento, String tipo, String chavePix,
                        String banco, String agencia, String conta, String observacao,
                        boolean ativo) {
        this.nome = nome;
        this.documento = documento;
        this.tipo = tipo;
        this.chavePix = chavePix;
        this.banco = banco;
        this.agencia = agencia;
        this.conta = conta;
        this.observacao = observacao;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTipo() {
        return tipo;
    }

    public String getChavePix() {
        return chavePix;
    }

    public String getBanco() {
        return banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getConta() {
        return conta;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
