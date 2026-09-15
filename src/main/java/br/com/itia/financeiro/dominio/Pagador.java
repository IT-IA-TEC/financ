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
 * A pessoa que paga, identificada pelo CPF.
 *
 * Uma pessoa responde por uma ou mais unidades (CNPJ, loja, filial, contrato).
 * A divida nasce na unidade; a cobranca vai para a pessoa. E o mesmo desenho da
 * base de clientes do sistema do grupo, onde uma pessoa aparece com varias
 * empresas penduradas nela.
 */
@Entity
@Table(name = "pagador")
public class Pagador {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nome;

    private String cpf;
    private String whatsapp;
    private String telefone;
    private String email;
    private String observacao;

    @Column(name = "data_nascimento")
    private java.time.LocalDate dataNascimento;

    @Column(name = "nome_social")
    private String nomeSocial;

    @Column(name = "cliente_desde")
    private java.time.LocalDate clienteDesde;

    /** ATIVO, INATIVO, EM_DISPUTA ou ENCERRADO. */
    @Column(nullable = false)
    private String situacao = "ATIVO";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Pagador() {
        // exigido pelo JPA
    }

    public Pagador(Empresa empresa, String nome, String cpf, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.nome = nome;
        this.cpf = cpf;
        this.criadoPor = criadoPor;
    }

    /** Os dados de identificacao, do bloco de cadastro do perfil. */
    public void atualizarIdentificacao(String nome, String nomeSocial, String cpf,
                                       java.time.LocalDate dataNascimento,
                                       java.time.LocalDate clienteDesde, String situacao) {
        this.nome = nome;
        this.nomeSocial = nomeSocial;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.clienteDesde = clienteDesde;
        this.situacao = situacao == null ? "ATIVO" : situacao;
        this.ativo = !"ENCERRADO".equals(this.situacao);
    }

    public java.time.LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getNomeSocial() {
        return nomeSocial;
    }

    public java.time.LocalDate getClienteDesde() {
        return clienteDesde;
    }

    public String getSituacao() {
        return situacao;
    }

    public void atualizar(String nome, String cpf, String whatsapp,
                         String telefone, String email, String observacao) {
        this.nome = nome;
        this.cpf = cpf;
        this.whatsapp = whatsapp;
        this.telefone = telefone;
        this.email = email;
        this.observacao = observacao;
    }

    /** As duas letras que aparecem no circulo do perfil. */
    public String getIniciais() {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
