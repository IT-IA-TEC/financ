package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O texto pronto que a empresa usa para cobrar.
 *
 * O modelo guarda o texto com os espaços a preencher, escritos entre chaves. O
 * texto final é montado na hora do envio e gravado na mensagem, porque o modelo
 * muda com o tempo e a prova do que foi dito não pode mudar junto.
 */
@Entity
@Table(name = "modelo_mensagem")
public class ModeloDeMensagem {

    /** Os espaços que o texto aceita, e o que cada um significa. */
    public static final Map<String, String> ESPACOS = Map.ofEntries(
            Map.entry("cliente", "o nome do cliente"),
            Map.entry("contato", "o nome de quem recebe a mensagem"),
            Map.entry("empresa", "o nome da sua empresa"),
            Map.entry("numero", "o número do documento"),
            Map.entry("descricao", "a descrição do que está sendo cobrado"),
            Map.entry("valor", "o valor em aberto"),
            Map.entry("valor_atualizado", "o valor em aberto com juros e multa, se a empresa cobrar"),
            Map.entry("vencimento", "a data de vencimento"),
            Map.entry("atraso", "quantos dias de atraso"),
            Map.entry("chave_pix", "a chave PIX da sua empresa"),
            Map.entry("identificador", "o identificador para o PIX"));

    public static final List<String> TONS = List.of("AMIGAVEL", "FIRME", "FORMAL");

    public static final List<String> MOMENTOS =
            List.of("ANTES_VENCER", "NO_DIA", "APOS_VENCER", "LIVRE");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String nome;

    /** WHATSAPP ou EMAIL. */
    @Column(nullable = false)
    private String canal = "WHATSAPP";

    private String assunto;

    @Column(nullable = false, length = 4000)
    private String corpo;

    @Column(nullable = false)
    private String tom = "AMIGAVEL";

    @Column(nullable = false)
    private String momento = "LIVRE";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected ModeloDeMensagem() {
        // exigido pelo JPA
    }

    public ModeloDeMensagem(UUID empresaId, String nome, String canal, String assunto,
                            String corpo, String tom, String momento, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.criadoPor = criadoPor;
        ajustar(nome, canal, assunto, corpo, tom, momento, true);
    }

    public void ajustar(String nome, String canal, String assunto, String corpo,
                        String tom, String momento, boolean ativo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O modelo precisa de um nome.");
        }
        if (corpo == null || corpo.isBlank()) {
            throw new IllegalArgumentException("O modelo precisa de um texto.");
        }
        this.nome = nome.trim();
        this.canal = canal == null ? "WHATSAPP" : canal;
        this.assunto = assunto;
        this.corpo = corpo.trim();
        this.tom = tom == null ? "AMIGAVEL" : tom;
        this.momento = momento == null ? "LIVRE" : momento;
        this.ativo = ativo;
    }

    /** Troca os espaços entre chaves pelos valores de verdade. */
    public String montar(Map<String, String> valores) {
        return preencher(corpo, valores);
    }

    public String montarAssunto(Map<String, String> valores) {
        return assunto == null ? null : preencher(assunto, valores);
    }

    public static String preencher(String texto, Map<String, String> valores) {
        String pronto = texto;
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            pronto = pronto.replace("{" + valor.getKey() + "}",
                    valor.getValue() == null ? "" : valor.getValue());
        }
        return pronto;
    }

    /** Os espaços escritos no texto que não existem na lista. */
    public List<String> espacosDesconhecidos() {
        java.util.List<String> fora = new java.util.ArrayList<>();
        java.util.regex.Matcher achou =
                java.util.regex.Pattern.compile("\\{([a-z_]+)\\}").matcher(corpo);
        while (achou.find()) {
            String nomeDoEspaco = achou.group(1);
            if (!ESPACOS.containsKey(nomeDoEspaco) && !fora.contains(nomeDoEspaco)) {
                fora.add(nomeDoEspaco);
            }
        }
        return fora;
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

    public String getCanal() {
        return canal;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getCorpo() {
        return corpo;
    }

    public String getTom() {
        return tom;
    }

    public String getTomLegivel() {
        return switch (tom) {
            case "FIRME" -> "firme";
            case "FORMAL" -> "formal";
            default -> "amigável";
        };
    }

    public String getMomento() {
        return momento;
    }

    public String getMomentoLegivel() {
        return switch (momento) {
            case "ANTES_VENCER" -> "antes de vencer";
            case "NO_DIA" -> "no dia";
            case "APOS_VENCER" -> "depois de vencer";
            default -> "qualquer momento";
        };
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
