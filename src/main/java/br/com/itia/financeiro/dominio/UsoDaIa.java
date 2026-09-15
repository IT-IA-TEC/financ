package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O registro de cada vez que o modelo foi usado.
 *
 * Guarda o que ele recebeu, o que respondeu, que permissões estavam valendo e
 * o que foi feito com a resposta. Sem esse registro ninguém consegue conferir
 * se o modelo está trabalhando direito, e modelo que ninguém confere não
 * deveria estar falando com cliente.
 */
@Entity
@Table(name = "uso_da_ia")
public class UsoDaIa {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    /** LEITURA_DO_CASO, RESPOSTA_SUGERIDA ou ACAO. */
    @Column(nullable = false)
    private String tipo;

    private String permissoes;

    @Column(length = 4000)
    private String pergunta;

    @Column(length = 4000)
    private String resposta;

    private String acao;

    @Column(nullable = false)
    private boolean executada = false;

    private String recusa;

    @Column(name = "aprovada_por")
    private String aprovadaPor;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    protected UsoDaIa() {
        // exigido pelo JPA
    }

    public UsoDaIa(UUID empresaId, UUID unidadeId, String tipo, String permissoes,
                   String pergunta) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
        this.tipo = tipo;
        this.permissoes = permissoes;
        this.pergunta = pergunta;
    }

    public void respondeu(String resposta) {
        this.resposta = resposta;
    }

    public void fez(String acao, String quem) {
        this.acao = acao;
        this.executada = true;
        this.aprovadaPor = quem;
    }

    public void recusou(String acao, String motivo) {
        this.acao = acao;
        this.executada = false;
        this.recusa = motivo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPermissoes() {
        return permissoes;
    }

    public String getPergunta() {
        return pergunta;
    }

    public String getResposta() {
        return resposta;
    }

    public String getAcao() {
        return acao;
    }

    public boolean isExecutada() {
        return executada;
    }

    public String getRecusa() {
        return recusa;
    }

    public String getAprovadaPor() {
        return aprovadaPor;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
