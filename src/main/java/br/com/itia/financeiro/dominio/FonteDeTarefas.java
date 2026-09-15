package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * De onde as tarefas de outro sistema chegam.
 *
 * O endereço e a chave não moram aqui: moram na integração, cifrados. Aqui
 * fica só o mapeamento, que é a parte que muda de sistema para sistema: qual
 * campo de lá vira qual campo daqui.
 *
 * O campo do identificador é o mais importante da lista. É ele que impede a
 * mesma tarefa de virar dez tarefas repetidas a cada sincronização.
 */
@Entity
@Table(name = "fonte_de_tarefas")
public class FonteDeTarefas {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "integracao_id", nullable = false)
    private UUID integracaoId;

    @Column(name = "operacao_id")
    private UUID operacaoId;

    @Column(name = "campo_id", nullable = false)
    private String campoId = "id";

    @Column(name = "campo_titulo", nullable = false)
    private String campoTitulo = "titulo";

    @Column(name = "campo_descricao")
    private String campoDescricao;

    @Column(name = "campo_situacao")
    private String campoSituacao;

    @Column(name = "campo_responsavel")
    private String campoResponsavel;

    @Column(name = "campo_prazo")
    private String campoPrazo;

    @Column(name = "campo_prioridade")
    private String campoPrioridade;

    @Column(name = "campo_link")
    private String campoLink;

    private String filtro;

    @Column(name = "setor_padrao")
    private String setorPadrao;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "ultima_puxada")
    private OffsetDateTime ultimaPuxada;

    @Column(name = "ultimo_resultado")
    private String ultimoResultado;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected FonteDeTarefas() {
        // exigido pelo JPA
    }

    public FonteDeTarefas(UUID empresaId, String nome, UUID integracaoId, String quem) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.integracaoId = integracaoId;
        this.criadoPor = quem;
        this.nome = nome == null || nome.isBlank() ? "Tarefas de fora" : nome.trim();
    }

    public void ajustar(String nome, UUID integracaoId, UUID operacaoId, String campoId,
                        String campoTitulo, String campoDescricao, String campoSituacao,
                        String campoResponsavel, String campoPrazo, String campoPrioridade,
                        String campoLink, String filtro, String setorPadrao, boolean ativa) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("A fonte precisa de um nome.");
        }
        if (integracaoId == null) {
            throw new IllegalArgumentException("Escolha a integração que fala com o outro sistema.");
        }
        if (campoId == null || campoId.isBlank()) {
            throw new IllegalArgumentException(
                    "Diga qual campo de lá é o identificador. Sem ele, cada sincronização"
                            + " criaria as mesmas tarefas de novo.");
        }
        if (campoTitulo == null || campoTitulo.isBlank()) {
            throw new IllegalArgumentException("Diga qual campo de lá é o título da tarefa.");
        }
        this.nome = nome.trim();
        this.integracaoId = integracaoId;
        this.operacaoId = operacaoId;
        this.campoId = campoId.trim();
        this.campoTitulo = campoTitulo.trim();
        this.campoDescricao = vazioViraNulo(campoDescricao);
        this.campoSituacao = vazioViraNulo(campoSituacao);
        this.campoResponsavel = vazioViraNulo(campoResponsavel);
        this.campoPrazo = vazioViraNulo(campoPrazo);
        this.campoPrioridade = vazioViraNulo(campoPrioridade);
        this.campoLink = vazioViraNulo(campoLink);
        this.filtro = vazioViraNulo(filtro);
        this.setorPadrao = vazioViraNulo(setorPadrao);
        this.ativa = ativa;
    }

    private String vazioViraNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public void anotarPuxada(String resultado) {
        this.ultimaPuxada = OffsetDateTime.now();
        this.ultimoResultado = resultado;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getNome() {
        return nome;
    }

    public UUID getIntegracaoId() {
        return integracaoId;
    }

    public UUID getOperacaoId() {
        return operacaoId;
    }

    public String getCampoId() {
        return campoId;
    }

    public String getCampoTitulo() {
        return campoTitulo;
    }

    public String getCampoDescricao() {
        return campoDescricao;
    }

    public String getCampoSituacao() {
        return campoSituacao;
    }

    public String getCampoResponsavel() {
        return campoResponsavel;
    }

    public String getCampoPrazo() {
        return campoPrazo;
    }

    public String getCampoPrioridade() {
        return campoPrioridade;
    }

    public String getCampoLink() {
        return campoLink;
    }

    public String getFiltro() {
        return filtro;
    }

    public String getSetorPadrao() {
        return setorPadrao;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public OffsetDateTime getUltimaPuxada() {
        return ultimaPuxada;
    }

    public String getUltimoResultado() {
        return ultimoResultado;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
