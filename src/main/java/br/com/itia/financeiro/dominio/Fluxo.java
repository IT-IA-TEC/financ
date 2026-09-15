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
 * Uma automação montada pelo usuário.
 *
 * O fluxo é sempre a mesma frase: quando acontecer tal coisa, se tais
 * condições valerem, faça tais ações. Manter essa forma é o que permite
 * qualquer pessoa da equipe ler o que o sistema vai fazer antes de ligar.
 */
@Entity
@Table(name = "fluxo")
public class Fluxo {

    public static final List<String> GATILHOS = List.of(
            "VENCE_EM_DIAS", "VENCEU_HOJE", "ATRASO_DE_DIAS",
            "CLIENTE_RESPONDEU", "PAGAMENTO_ENTROU", "ACORDO_QUEBRADO");

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private String gatilho;

    @Column(nullable = false)
    private int dias = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultima_rodada")
    private LocalDate ultimaRodada;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    protected Fluxo() {
        // exigido pelo JPA
    }

    public Fluxo(UUID empresaId, String nome, String descricao, String gatilho, int dias,
                 String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.criadoPor = criadoPor;
        ajustar(nome, descricao, gatilho, dias, true);
    }

    public void ajustar(String nome, String descricao, String gatilho, int dias, boolean ativo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O fluxo precisa de um nome.");
        }
        if (gatilho == null || !GATILHOS.contains(gatilho)) {
            throw new IllegalArgumentException("Escolha quando este fluxo acontece.");
        }
        if (dias < 0) {
            throw new IllegalArgumentException("Os dias contam sempre para frente, sem sinal.");
        }
        this.nome = nome.trim();
        this.descricao = descricao;
        this.gatilho = gatilho;
        this.dias = dias;
        this.ativo = ativo;
    }

    /** Se este fluxo é disparado por data, e não por algo que aconteceu. */
    public boolean deCalendario() {
        return "VENCE_EM_DIAS".equals(gatilho) || "VENCEU_HOJE".equals(gatilho)
                || "ATRASO_DE_DIAS".equals(gatilho);
    }

    /** Se hoje é o dia deste fluxo para um documento que vence em tal data. */
    public boolean valeHoje(LocalDate vencimento, LocalDate hoje) {
        if (!ativo || vencimento == null) {
            return false;
        }
        return switch (gatilho) {
            case "VENCE_EM_DIAS" -> vencimento.minusDays(dias).isEqual(hoje);
            case "VENCEU_HOJE" -> vencimento.isEqual(hoje);
            case "ATRASO_DE_DIAS" -> vencimento.plusDays(dias).isEqual(hoje);
            default -> false;
        };
    }

    public void anotarRodada(LocalDate quando) {
        this.ultimaRodada = quando;
    }

    public String getQuandoLegivel() {
        return switch (gatilho) {
            case "VENCE_EM_DIAS" -> "quando faltarem " + dias + " dia(s) para vencer";
            case "VENCEU_HOJE" -> "no dia do vencimento";
            case "ATRASO_DE_DIAS" -> "quando completar " + dias + " dia(s) de atraso";
            case "CLIENTE_RESPONDEU" -> "quando o cliente responder";
            case "PAGAMENTO_ENTROU" -> "quando o pagamento entrar";
            default -> "quando um acordo for quebrado";
        };
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

    public String getDescricao() {
        return descricao;
    }

    public String getGatilho() {
        return gatilho;
    }

    public int getDias() {
        return dias;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public LocalDate getUltimaRodada() {
        return ultimaRodada;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
