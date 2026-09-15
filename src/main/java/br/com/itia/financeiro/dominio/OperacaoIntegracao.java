package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma chamada que aquela integração pode fazer.
 *
 * É o que deixa o sistema pronto para qualquer API sem precisar de código novo:
 * cada operação diz o que faz, com qual verbo, em qual caminho. Nada roda para
 * fora sem estar cadastrado aqui.
 */
@Entity
@Table(name = "integracao_operacao")
public class OperacaoIntegracao {

    @Id
    private UUID id;

    @Column(name = "integracao_id", nullable = false)
    private UUID integracaoId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String verbo = "GET";

    @Column(nullable = false)
    private String caminho;

    @Column(name = "para_que")
    private String paraQue;

    @Column(name = "corpo_modelo")
    private String corpoModelo;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    protected OperacaoIntegracao() {
        // exigido pelo JPA
    }

    public OperacaoIntegracao(UUID integracaoId, String nome, String verbo,
                              String caminho, String paraQue) {
        this.id = UUID.randomUUID();
        this.integracaoId = integracaoId;
        this.nome = nome;
        this.verbo = verbo == null ? "GET" : verbo.toUpperCase();
        this.caminho = caminho;
        this.paraQue = paraQue;
    }

    public void ajustar(String nome, String verbo, String caminho,
                        String paraQue, String corpoModelo) {
        this.nome = nome;
        this.verbo = verbo == null ? "GET" : verbo.toUpperCase();
        this.caminho = caminho;
        this.paraQue = paraQue;
        this.corpoModelo = corpoModelo;
    }

    public void desligar() {
        this.ativa = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIntegracaoId() {
        return integracaoId;
    }

    public String getNome() {
        return nome;
    }

    public String getVerbo() {
        return verbo;
    }

    public String getCaminho() {
        return caminho;
    }

    public String getParaQue() {
        return paraQue;
    }

    public String getCorpoModelo() {
        return corpoModelo;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
