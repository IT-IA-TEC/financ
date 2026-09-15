package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma tarefa da Cúpula.
 *
 * A demanda grande é a tarefa pai; cada ticket dela é uma tarefa filha, com o
 * próprio responsável e o próprio prazo. Tarefa sem responsável está aberta
 * para o setor inteiro, e some da lista de todos assim que alguém pega.
 *
 * Nada aqui é apagado: cancelar guarda o motivo e quem cancelou, porque a
 * pergunta "por que isso parou" chega meses depois.
 */
@Entity
@Table(name = "tarefa")
public class Tarefa {

    public static final List<String> PRIORIDADES = List.of("BAIXA", "NORMAL", "ALTA", "CRITICA");

    /**
     * O que o "+ Novo" abre. É a mesma lista da Cúpula do sistema matriz: seis
     * coisas diferentes que andam pelo mesmo caminho (alguém pede, alguém faz,
     * quem pediu confere) e mudam só de vocabulário na tela.
     */
    public static final List<String> TIPOS =
            List.of("TAREFA", "DEMANDA", "DECISAO", "ALERTA", "CHAMADO", "COMPRA");

    public static final List<String> SITUACOES = List.of(
            "A_FAZER", "EM_ANDAMENTO", "AGUARDANDO_REVISAO", "CONCLUIDA", "CANCELADA");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "pai_id")
    private UUID paiId;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 4000)
    private String descricao;

    @Column(nullable = false)
    private String tipo = "TAREFA";

    @Column(nullable = false)
    private String prioridade = "NORMAL";

    @Column(nullable = false)
    private String situacao = "A_FAZER";

    @Column(name = "pedida_por")
    private String pedidaPor;

    private String responsavel;

    private String setor;

    private LocalDate prazo;

    /** MANUAL, API ou SINCRONIZADA. */
    @Column(nullable = false)
    private String origem = "MANUAL";

    @Column(name = "fonte_id")
    private UUID fonteId;

    @Column(name = "id_externo")
    private String idExterno;

    @Column(name = "link_externo")
    private String linkExterno;

    @Column(name = "vista_em")
    private OffsetDateTime vistaEm;

    @Column(name = "iniciada_em")
    private OffsetDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    @Column(name = "revisada_em")
    private OffsetDateTime revisadaEm;

    @Column(name = "revisada_por")
    private String revisadaPor;

    @Column(name = "motivo_retrabalho")
    private String motivoRetrabalho;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "cancelada_por")
    private String canceladaPor;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Column(name = "cobrada_em")
    private OffsetDateTime cobradaEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(UUID empresaId, Integer numero, String titulo, String descricao,
                  String prioridade, String responsavel, String setor, LocalDate prazo,
                  UUID paiId, String quem) {
        this(empresaId, numero, "TAREFA", titulo, descricao, prioridade, responsavel, setor,
                prazo, paiId, quem);
    }

    public Tarefa(UUID empresaId, Integer numero, String tipo, String titulo, String descricao,
                  String prioridade, String responsavel, String setor, LocalDate prazo,
                  UUID paiId, String quem) {
        if (tipo != null && !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de tarefa desconhecido.");
        }
        this.tipo = tipo == null ? "TAREFA" : tipo;
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("A tarefa precisa de um título.");
        }
        if (prioridade != null && !PRIORIDADES.contains(prioridade)) {
            throw new IllegalArgumentException("Prioridade desconhecida.");
        }
        if ((responsavel == null || responsavel.isBlank())
                && (setor == null || setor.isBlank())) {
            throw new IllegalArgumentException(
                    "Diga quem faz, ou para qual setor a tarefa vai.");
        }
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.numero = numero;
        this.paiId = paiId;
        this.titulo = titulo.trim();
        this.descricao = descricao;
        this.prioridade = prioridade == null ? "NORMAL" : prioridade;
        this.responsavel = responsavel == null || responsavel.isBlank() ? null
                : responsavel.trim();
        this.setor = setor == null || setor.isBlank() ? null : setor.trim();
        this.prazo = prazo;
        this.pedidaPor = quem;
        this.criadoPor = quem;
    }

    // ------------------------------------------------------------------ andar

    /** Alguém pegou a tarefa que estava aberta para o setor. */
    public void pegarParaMim(String quem) {
        if (responsavel != null) {
            throw new IllegalStateException("Esta tarefa já tem dono: " + responsavel + ".");
        }
        exigirViva();
        this.responsavel = quem;
    }

    public void comecar(String quem) {
        exigirViva();
        if (responsavel == null) {
            this.responsavel = quem;
        }
        this.situacao = "EM_ANDAMENTO";
        this.iniciadaEm = OffsetDateTime.now();
    }

    /**
     * Quem faz diz que terminou.
     *
     * Não fecha: fica esperando quem pediu conferir. Fechar sozinho é como a
     * tarefa some sem ninguém saber se ficou boa.
     */
    public void terminar() {
        exigirViva();
        this.situacao = "AGUARDANDO_REVISAO";
        this.concluidaEm = OffsetDateTime.now();
    }

    /** Quem pediu aceitou: agora sim está concluída. */
    public void aceitar(String quem) {
        if (!"AGUARDANDO_REVISAO".equals(situacao)) {
            throw new IllegalStateException(
                    "Só dá para aceitar tarefa que está esperando revisão.");
        }
        this.situacao = "CONCLUIDA";
        this.revisadaEm = OffsetDateTime.now();
        this.revisadaPor = quem;
        this.motivoRetrabalho = null;
    }

    /** Quem pediu mandou refazer, e o motivo fica escrito. */
    public void mandarRefazer(String motivo, String quem) {
        if (!"AGUARDANDO_REVISAO".equals(situacao)) {
            throw new IllegalStateException(
                    "Só dá para mandar refazer tarefa que está esperando revisão.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escreva o que precisa ser refeito. Sem isso, quem fez não sabe o que mudar.");
        }
        this.situacao = "EM_ANDAMENTO";
        this.motivoRetrabalho = motivo.trim();
        this.revisadaPor = quem;
        this.revisadaEm = OffsetDateTime.now();
        this.concluidaEm = null;
    }

    public void cancelar(String motivo, String quem) {
        if ("CONCLUIDA".equals(situacao)) {
            throw new IllegalStateException("Tarefa concluída não é cancelada.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar sem motivo não explica nada depois.");
        }
        this.situacao = "CANCELADA";
        this.canceladaEm = OffsetDateTime.now();
        this.canceladaPor = quem;
        this.motivoCancelamento = motivo.trim();
    }

    public void trocarResponsavel(String quem) {
        exigirViva();
        this.responsavel = quem == null || quem.isBlank() ? null : quem.trim();
    }

    public void mudarPrazo(LocalDate novo) {
        exigirViva();
        this.prazo = novo;
    }

    public void cobrar() {
        exigirViva();
        this.cobradaEm = OffsetDateTime.now();
    }

    /** A primeira vez que alguém abriu: é isto que tira da Central. */
    public void marcarVista() {
        if (vistaEm == null) {
            this.vistaEm = OffsetDateTime.now();
        }
    }

    /** O que chegou de fora mudou lá: atualiza aqui sem duplicar. */
    public void atualizarDeFora(String titulo, String descricao, String situacao,
                                String responsavel, LocalDate prazo, String prioridade,
                                String link) {
        if (titulo != null && !titulo.isBlank()) {
            this.titulo = titulo.trim();
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (situacao != null && SITUACOES.contains(situacao)) {
            this.situacao = situacao;
        }
        if (responsavel != null && !responsavel.isBlank()) {
            this.responsavel = responsavel.trim();
        }
        if (prazo != null) {
            this.prazo = prazo;
        }
        if (prioridade != null && PRIORIDADES.contains(prioridade)) {
            this.prioridade = prioridade;
        }
        if (ehEnderecoDeVerdade(link)) {
            this.linkExterno = link.trim();
        }
    }

    /**
     * Se o link que veio de fora e mesmo um endereco de pagina.
     *
     * So http e https entram. Isso nao e frescura: o link chega de outro
     * sistema e vira um botao clicavel na tela, entao um "javascript:..."
     * ali dentro rodaria no navegador de quem clicasse.
     */
    private boolean ehEnderecoDeVerdade(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        String limpo = link.trim().toLowerCase();
        return limpo.startsWith("http://") || limpo.startsWith("https://");
    }

    public void veioDeFora(UUID fonteId, String idExterno, String origem) {
        this.fonteId = fonteId;
        this.idExterno = idExterno;
        this.origem = origem;
    }

    private void exigirViva() {
        if ("CANCELADA".equals(situacao)) {
            throw new IllegalStateException("Esta tarefa foi cancelada.");
        }
    }

    // ---------------------------------------------------------------- leitura

    public boolean aberta() {
        return !"CONCLUIDA".equals(situacao) && !"CANCELADA".equals(situacao);
    }

    /** Sem dono: está esperando alguém do setor pegar. */
    public boolean doSetor() {
        return responsavel == null && aberta();
    }

    public boolean atrasada(LocalDate hoje) {
        return aberta() && prazo != null && prazo.isBefore(hoje);
    }

    public boolean venceHoje(LocalDate hoje) {
        return aberta() && prazo != null && prazo.isEqual(hoje);
    }

    /** Se ainda não foi aberta por ninguém. */
    public boolean naoVista() {
        return vistaEm == null;
    }

    public boolean esperandoRevisao() {
        return "AGUARDANDO_REVISAO".equals(situacao);
    }

    public boolean voltouParaRefazer() {
        return motivoRetrabalho != null && "EM_ANDAMENTO".equals(situacao);
    }

    /** A coluna do quadro: três, como na Cúpula. */
    public String coluna() {
        return switch (situacao) {
            case "A_FAZER" -> "A_FAZER";
            case "CONCLUIDA", "CANCELADA" -> "CONCLUIDA";
            default -> "EM_ANDAMENTO";
        };
    }

    public String getSituacaoLegivel() {
        return switch (situacao) {
            case "A_FAZER" -> "a fazer";
            case "EM_ANDAMENTO" -> voltouParaRefazer() ? "refazendo" : "em andamento";
            case "AGUARDANDO_REVISAO" -> "esperando revisão";
            case "CONCLUIDA" -> "concluída";
            default -> "cancelada";
        };
    }

    public String getPrioridadeLegivel() {
        return switch (prioridade) {
            case "BAIXA" -> "baixa";
            case "ALTA" -> "alta";
            case "CRITICA" -> "crítica";
            default -> "normal";
        };
    }

    public String getCodigo() {
        return "T-" + numero;
    }

    public String getTipo() {
        return tipo;
    }

    /** O nome do tipo como a pessoa lê na tela. */
    public String getRotuloDoTipo() {
        return switch (tipo) {
            case "DEMANDA" -> "Demanda";
            case "DECISAO" -> "Decisão";
            case "ALERTA" -> "Alerta";
            case "CHAMADO" -> "Chamado";
            case "COMPRA" -> "Compra";
            default -> "Tarefa";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public Integer getNumero() {
        return numero;
    }

    public UUID getPaiId() {
        return paiId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getPedidaPor() {
        return pedidaPor;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getSetor() {
        return setor;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public String getOrigem() {
        return origem;
    }

    public UUID getFonteId() {
        return fonteId;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public String getLinkExterno() {
        return linkExterno;
    }

    public OffsetDateTime getVistaEm() {
        return vistaEm;
    }

    public OffsetDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public String getRevisadaPor() {
        return revisadaPor;
    }

    public String getMotivoRetrabalho() {
        return motivoRetrabalho;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public OffsetDateTime getCobradaEm() {
        return cobradaEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
