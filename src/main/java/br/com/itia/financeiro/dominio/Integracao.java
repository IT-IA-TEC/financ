package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Uma ligacao configurada com um sistema de fora.
 *
 * As credenciais ficam num unico texto cifrado. Nem a tela nem o registro de
 * eventos mostram segredo: quem precisa deles e so o servico que chama a outra
 * ponta, na hora da chamada.
 */
@Entity
@Table(name = "integracao")
public class Integracao {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** O modelo de partida. Depois de criada, o que vale e o tipo e a autenticacao. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Conector provedor;

    /** COMO a ligacao funciona. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIntegracao tipo;

    /** COMO o outro lado sabe que somos nos. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAutenticacao autenticacao;

    @Column(name = "tempo_limite_segundos")
    private Integer tempoLimiteSegundos = 20;

    @Column(name = "tentativas")
    private Integer tentativas = 3;

    @Column(name = "espera_entre_tentativas_ms")
    private Integer esperaEntreTentativasMs = 2000;

    @Column(name = "verificar_assinatura")
    private boolean verificarAssinatura = false;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String ambiente = "PRODUCAO";

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "credenciais_cifradas")
    private String credenciaisCifradas;

    /** Pedaco secreto do endereco de webhook. Sem ele, ninguem entrega nada. */
    @Column(name = "webhook_segredo", nullable = false)
    private String webhookSegredo;

    @Column(nullable = false)
    private boolean ativa = false;

    @Column(name = "ultima_checagem_em")
    private OffsetDateTime ultimaChecagemEm;

    @Column(name = "ultima_checagem_ok")
    private Boolean ultimaChecagemOk;

    @Column(name = "ultima_checagem_erro")
    private String ultimaChecagemErro;

    private String observacao;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Column(name = "criada_por")
    private String criadaPor;

    @Column(name = "atualizada_em", nullable = false)
    private OffsetDateTime atualizadaEm = OffsetDateTime.now();

    protected Integracao() {
        // exigido pelo JPA
    }

    public Integracao(Empresa empresa, Conector conector, TipoIntegracao tipo,
                      TipoAutenticacao autenticacao, String nome, String criadaPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.provedor = conector;
        this.tipo = tipo != null ? tipo : conector.getTipoSugerido();
        this.autenticacao = autenticacao != null ? autenticacao : conector.getAutenticacaoSugerida();
        this.nome = nome;
        this.criadaPor = criadaPor;
        this.baseUrl = conector.getEnderecoSugerido();
        this.webhookSegredo = novoSegredo();
    }

    private static String novoSegredo() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void ajustar(String nome, String ambiente, String baseUrl, String observacao,
                        TipoIntegracao tipo, TipoAutenticacao autenticacao,
                        Integer tempoLimiteSegundos, Integer tentativas,
                        Integer esperaEntreTentativasMs, boolean verificarAssinatura) {
        if (tipo != null) {
            this.tipo = tipo;
        }
        if (autenticacao != null) {
            this.autenticacao = autenticacao;
        }
        this.tempoLimiteSegundos = tempoLimiteSegundos == null ? 20 : tempoLimiteSegundos;
        this.tentativas = tentativas == null ? 3 : tentativas;
        this.esperaEntreTentativasMs = esperaEntreTentativasMs == null ? 2000 : esperaEntreTentativasMs;
        this.verificarAssinatura = verificarAssinatura;
        this.nome = nome;
        this.ambiente = ambiente == null ? "PRODUCAO" : ambiente;
        this.baseUrl = baseUrl;
        this.observacao = observacao;
        this.atualizadaEm = OffsetDateTime.now();
    }

    public void guardarCredenciais(String cifrado) {
        this.credenciaisCifradas = cifrado;
        this.atualizadaEm = OffsetDateTime.now();
    }

    public void ligar() {
        this.ativa = true;
        this.atualizadaEm = OffsetDateTime.now();
    }

    public void desligar() {
        this.ativa = false;
        this.atualizadaEm = OffsetDateTime.now();
    }

    public void anotarChecagem(boolean deuCerto, String erro) {
        this.ultimaChecagemEm = OffsetDateTime.now();
        this.ultimaChecagemOk = deuCerto;
        this.ultimaChecagemErro = deuCerto ? null : erro;
    }

    /** Troca o segredo do webhook. O endereco antigo para de funcionar na hora. */
    public void trocarSegredo() {
        this.webhookSegredo = novoSegredo();
        this.atualizadaEm = OffsetDateTime.now();
    }

    public String getCaminhoDoWebhook() {
        return "/webhooks/" + id + "/" + webhookSegredo;
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Conector getProvedor() {
        return provedor;
    }

    public TipoIntegracao getTipo() {
        return tipo;
    }

    public TipoAutenticacao getAutenticacao() {
        return autenticacao;
    }

    public Integer getTempoLimiteSegundos() {
        return tempoLimiteSegundos == null ? 20 : tempoLimiteSegundos;
    }

    public Integer getTentativas() {
        return tentativas == null ? 3 : tentativas;
    }

    public Integer getEsperaEntreTentativasMs() {
        return esperaEntreTentativasMs == null ? 2000 : esperaEntreTentativasMs;
    }

    public boolean isVerificarAssinatura() {
        return verificarAssinatura;
    }

    /** Os campos que a forma de autenticacao escolhida exige. */
    public java.util.List<TipoAutenticacao.Campo> getCamposDeAcesso() {
        return autenticacao.getCampos();
    }

    public String getNome() {
        return nome;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCredenciaisCifradas() {
        return credenciaisCifradas;
    }

    public String getWebhookSegredo() {
        return webhookSegredo;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public OffsetDateTime getUltimaChecagemEm() {
        return ultimaChecagemEm;
    }

    public Boolean getUltimaChecagemOk() {
        return ultimaChecagemOk;
    }

    public String getUltimaChecagemErro() {
        return ultimaChecagemErro;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }
}
