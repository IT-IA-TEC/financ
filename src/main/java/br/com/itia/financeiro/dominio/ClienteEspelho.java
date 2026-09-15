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
 * Copia do cliente que vive no ERP.
 *
 * Este sistema NUNCA cria nem altera cliente por conta propria: quem manda no
 * cadastro e o ERP. Aqui a copia existe so para que o titulo tenha dono e para
 * que a tela mostre o nome sem precisar ir buscar la toda hora.
 */
@Entity
@Table(name = "cliente_espelho")
public class ClienteEspelho {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** A pessoa que paga por esta unidade. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_id")
    private Pagador pagador;

    /** O codigo da unidade no sistema de origem, quando existe. */
    @Column(name = "codigo_externo")
    private String codigoExterno;

    @Column(name = "erp_cliente_id")
    private UUID erpClienteId;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "cnpj_cpf")
    private String cnpjCpf;

    /** O quanto desta unidade e cobrado, quando a cobranca e por percentual. */
    @Column(precision = 7, scale = 4)
    private java.math.BigDecimal percentual;

    /** De onde vem o faturamento desta unidade: marketplace, loja propria. */
    private String plataforma;

    /** Como falar com esta unidade: PADRAO, FIRME ou CUIDADOSO. */
    @Column(name = "tom_de_cobranca")
    private String tomDeCobranca = "PADRAO";

    @Column(name = "aceita_parcelamento")
    private boolean aceitaParcelamento = true;

    @Column(name = "inicio_na_casa")
    private java.time.LocalDate inicioNaCasa;

    private String responsavel;
    private String telefone;
    private String email;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "inscricao_estadual")
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal")
    private String inscricaoMunicipal;

    @Column(name = "regime_tributario")
    private String regimeTributario;

    private String porte;

    @Column(name = "cnae_principal")
    private String cnaePrincipal;

    @Column(name = "cnae_descricao")
    private String cnaeDescricao;

    @Column(name = "data_abertura")
    private java.time.LocalDate dataAbertura;

    @Column(name = "situacao_cadastral")
    private String situacaoCadastral;

    @Column(name = "optante_simples")
    private Boolean optanteSimples;

    /** Quais impostos sao retidos nesta unidade, separados por virgula. */
    private String retencoes;

    /** De onde vieram os dados fiscais: MANUAL, CNPJA, CONEXA, BANCO. */
    @Column(nullable = false)
    private String fonte = "MANUAL";

    @Column(name = "sincronizado_de_fora_em")
    private OffsetDateTime sincronizadoDeForaEm;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "sincronizado_em", nullable = false)
    private OffsetDateTime sincronizadoEm = OffsetDateTime.now();

    protected ClienteEspelho() {
        // exigido pelo JPA
    }

    public ClienteEspelho(Empresa empresa, UUID erpClienteId, String razaoSocial) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.erpClienteId = erpClienteId;
        this.razaoSocial = razaoSocial;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    /** Os dados fiscais, vindos da tela ou de uma consulta externa. */
    public void atualizarDadosFiscais(String nomeFantasia, String inscricaoEstadual,
                                      String inscricaoMunicipal, String regimeTributario,
                                      String porte, String cnaePrincipal, String cnaeDescricao,
                                      java.time.LocalDate dataAbertura, String situacaoCadastral,
                                      Boolean optanteSimples, String retencoes, Fonte fonte) {
        this.nomeFantasia = nomeFantasia;
        this.inscricaoEstadual = inscricaoEstadual;
        this.inscricaoMunicipal = inscricaoMunicipal;
        this.regimeTributario = regimeTributario;
        this.porte = porte;
        this.cnaePrincipal = cnaePrincipal;
        this.cnaeDescricao = cnaeDescricao;
        this.dataAbertura = dataAbertura;
        this.situacaoCadastral = situacaoCadastral;
        this.optanteSimples = optanteSimples;
        this.retencoes = retencoes;
        if (fonte != null) {
            this.fonte = fonte.name();
            if (fonte != Fonte.MANUAL) {
                this.sincronizadoDeForaEm = OffsetDateTime.now();
            }
        }
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public String getInscricaoMunicipal() {
        return inscricaoMunicipal;
    }

    public String getRegimeTributario() {
        return regimeTributario;
    }

    public String getPorte() {
        return porte;
    }

    public String getCnaePrincipal() {
        return cnaePrincipal;
    }

    public String getCnaeDescricao() {
        return cnaeDescricao;
    }

    public java.time.LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public String getSituacaoCadastral() {
        return situacaoCadastral;
    }

    public Boolean getOptanteSimples() {
        return optanteSimples;
    }

    public String getRetencoes() {
        return retencoes;
    }

    public String getFonte() {
        return fonte;
    }

    public OffsetDateTime getSincronizadoDeForaEm() {
        return sincronizadoDeForaEm;
    }

    public Pagador getPagador() {
        return pagador;
    }

    public void vincularA(Pagador pagador) {
        this.pagador = pagador;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    /** Atualiza a copia com o que veio do ERP. */
    public void atualizarCom(String razaoSocial, String cnpjCpf, String responsavel,
                             String telefone, String email, boolean ativo) {
        this.razaoSocial = razaoSocial;
        this.cnpjCpf = cnpjCpf;
        this.responsavel = responsavel;
        this.telefone = telefone;
        this.email = email;
        this.ativo = ativo;
        this.sincronizadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getErpClienteId() {
        return erpClienteId;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    /** Os dados comerciais da unidade, que sao dela e nao da pessoa. */
    public void ajustarComercial(java.math.BigDecimal percentual, String plataforma,
                                 String tomDeCobranca, boolean aceitaParcelamento,
                                 java.time.LocalDate inicioNaCasa) {
        this.percentual = percentual;
        this.plataforma = plataforma;
        this.tomDeCobranca = tomDeCobranca == null ? "PADRAO" : tomDeCobranca;
        this.aceitaParcelamento = aceitaParcelamento;
        this.inicioNaCasa = inicioNaCasa;
    }

    public java.math.BigDecimal getPercentual() {
        return percentual;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getTomDeCobranca() {
        return tomDeCobranca;
    }

    public boolean isAceitaParcelamento() {
        return aceitaParcelamento;
    }

    public java.time.LocalDate getInicioNaCasa() {
        return inicioNaCasa;
    }

    public String getCnpjCpf() {
        return cnpjCpf;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getSincronizadoEm() {
        return sincronizadoEm;
    }
}
