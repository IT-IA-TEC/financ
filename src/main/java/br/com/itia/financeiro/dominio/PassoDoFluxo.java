package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Um pedaço de um fluxo: uma condição a conferir ou uma ação a fazer.
 *
 * Condição e ação moram na mesma tabela porque, na tela, elas são a mesma
 * lista em ordem: primeiro o que precisa valer, depois o que acontece.
 */
@Entity
@Table(name = "passo_do_fluxo")
public class PassoDoFluxo {

    public static final List<String> CAMPOS = List.of(
            "VALOR_EM_ABERTO", "DIAS_ATRASO", "SITUACAO_DO_CASO", "TOM_DO_CLIENTE");

    public static final List<String> OPERADORES = List.of("MAIOR", "MENOR", "IGUAL", "DIFERENTE");

    public static final List<String> ACOES = List.of(
            "MANDAR_MENSAGEM", "MARCAR_CASO", "ANOTAR_PROXIMA_ACAO", "REGISTRAR_OBSERVACAO");

    @Id
    private UUID id;

    @Column(name = "fluxo_id", nullable = false)
    private UUID fluxoId;

    /** CONDICAO ou ACAO. */
    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private int ordem = 1;

    private String campo;

    private String operador;

    private String valor;

    private String acao;

    @Column(name = "modelo_id")
    private UUID modeloId;

    private String texto;

    @Column(nullable = false)
    private int dias = 0;

    protected PassoDoFluxo() {
        // exigido pelo JPA
    }

    public static PassoDoFluxo condicao(UUID fluxoId, int ordem, String campo, String operador,
                                        String valor) {
        if (!CAMPOS.contains(campo) || !OPERADORES.contains(operador)) {
            throw new IllegalArgumentException("Condição incompleta. Escolha o que conferir.");
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("A condição precisa de um valor para comparar.");
        }
        PassoDoFluxo passo = new PassoDoFluxo();
        passo.id = UUID.randomUUID();
        passo.fluxoId = fluxoId;
        passo.tipo = "CONDICAO";
        passo.ordem = ordem;
        passo.campo = campo;
        passo.operador = operador;
        passo.valor = valor.trim();
        return passo;
    }

    public static PassoDoFluxo acao(UUID fluxoId, int ordem, String acao, UUID modeloId,
                                    String texto, int dias) {
        if (!ACOES.contains(acao)) {
            throw new IllegalArgumentException("Escolha o que este fluxo faz.");
        }
        if ("MANDAR_MENSAGEM".equals(acao) && modeloId == null) {
            throw new IllegalArgumentException("Escolha o texto que a mensagem usa.");
        }
        if (!"MANDAR_MENSAGEM".equals(acao) && (texto == null || texto.isBlank())) {
            throw new IllegalArgumentException("Escreva o que esta ação registra.");
        }
        PassoDoFluxo passo = new PassoDoFluxo();
        passo.id = UUID.randomUUID();
        passo.fluxoId = fluxoId;
        passo.tipo = "ACAO";
        passo.ordem = ordem;
        passo.acao = acao;
        passo.modeloId = modeloId;
        passo.texto = texto == null ? null : texto.trim();
        passo.dias = dias;
        return passo;
    }

    public boolean ehCondicao() {
        return "CONDICAO".equals(tipo);
    }

    /**
     * Confere a condição contra um número.
     *
     * Valor que não é número nunca passa por engano: a condição simplesmente
     * não vale, e o fluxo não age.
     */
    public boolean aceita(BigDecimal numero) {
        BigDecimal alvo = comoNumero();
        if (alvo == null || numero == null) {
            return false;
        }
        return switch (operador) {
            case "MAIOR" -> numero.compareTo(alvo) > 0;
            case "MENOR" -> numero.compareTo(alvo) < 0;
            case "IGUAL" -> numero.compareTo(alvo) == 0;
            default -> numero.compareTo(alvo) != 0;
        };
    }

    /** Confere a condição contra um texto, sem diferenciar maiúscula. */
    public boolean aceita(String texto) {
        String lido = texto == null ? "" : texto.trim();
        boolean igual = lido.equalsIgnoreCase(valor == null ? "" : valor.trim());
        return switch (operador) {
            case "DIFERENTE" -> !igual;
            default -> igual;
        };
    }

    private BigDecimal comoNumero() {
        try {
            return new BigDecimal(valor.replace(".", "").replace(",", "."));
        } catch (RuntimeException erro) {
            return null;
        }
    }

    public String getResumo() {
        if (ehCondicao()) {
            return switch (campo) {
                case "VALOR_EM_ABERTO" -> "valor em aberto " + sinal() + " " + valor;
                case "DIAS_ATRASO" -> "dias de atraso " + sinal() + " " + valor;
                case "SITUACAO_DO_CASO" -> "situação do caso " + sinal() + " " + valor;
                default -> "tom do cliente " + sinal() + " " + valor;
            };
        }
        return switch (acao) {
            case "MANDAR_MENSAGEM" -> "mandar mensagem";
            case "MARCAR_CASO" -> "marcar o caso como " + texto;
            case "ANOTAR_PROXIMA_ACAO" -> "anotar próxima ação: " + texto
                    + (dias > 0 ? " em " + dias + " dia(s)" : "");
            default -> "registrar observação: " + texto;
        };
    }

    private String sinal() {
        return switch (operador) {
            case "MAIOR" -> "maior que";
            case "MENOR" -> "menor que";
            case "DIFERENTE" -> "diferente de";
            default -> "igual a";
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getFluxoId() {
        return fluxoId;
    }

    public String getTipo() {
        return tipo;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getCampo() {
        return campo;
    }

    public String getOperador() {
        return operador;
    }

    public String getValor() {
        return valor;
    }

    public String getAcao() {
        return acao;
    }

    public UUID getModeloId() {
        return modeloId;
    }

    public String getTexto() {
        return texto;
    }

    public int getDias() {
        return dias;
    }
}
