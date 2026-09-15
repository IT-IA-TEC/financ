package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Como esta unidade paga.
 *
 * Tudo aqui pode chegar de fora: da plataforma de cobrança, do banco ou de uma
 * importação. Por isso guarda a fonte e a data em que o dado chegou, e não só o
 * valor. Sem isso, ninguém sabe se o que está na tela é o que o banco tem.
 */
@Entity
@Table(name = "preferencia_cobranca")
public class PreferenciaDeCobranca {

    @Id
    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "forma_preferida")
    private String formaPreferida;

    @Column(name = "chave_pix")
    private String chavePix;

    private String banco;
    private String agencia;
    private String conta;
    private String titular;

    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    private String periodicidade;

    @Column(name = "email_cobranca")
    private String emailCobranca;

    @Column(name = "aceita_debito_recorrente", nullable = false)
    private boolean aceitaDebitoRecorrente = false;

    @Column(name = "juros_ao_mes", precision = 6, scale = 3)
    private BigDecimal jurosAoMes;

    @Column(name = "multa_percentual", precision = 6, scale = 3)
    private BigDecimal multaPercentual;

    @Column(name = "desconto_antecipacao", precision = 6, scale = 3)
    private BigDecimal descontoAntecipacao;

    @Column(name = "dias_carencia")
    private Integer diasCarencia;

    @Column(name = "protestar_apos_dias")
    private Integer protestarAposDias;

    private String instrucoes;

    @Column(nullable = false)
    private String fonte = "MANUAL";

    @Column(name = "sincronizado_de_fora_em")
    private OffsetDateTime sincronizadoDeForaEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected PreferenciaDeCobranca() {
        // exigido pelo JPA
    }

    public PreferenciaDeCobranca(UUID unidadeId, UUID empresaId) {
        this.unidadeId = unidadeId;
        this.empresaId = empresaId;
    }

    public void ajustar(String formaPreferida, String chavePix, String banco, String agencia,
                        String conta, String titular, Integer diaVencimento, String periodicidade,
                        String emailCobranca, boolean aceitaDebitoRecorrente, BigDecimal jurosAoMes,
                        BigDecimal multaPercentual, BigDecimal descontoAntecipacao,
                        Integer diasCarencia, Integer protestarAposDias, String instrucoes,
                        Fonte fonte) {
        this.formaPreferida = formaPreferida;
        this.chavePix = chavePix;
        this.banco = banco;
        this.agencia = agencia;
        this.conta = conta;
        this.titular = titular;
        this.diaVencimento = diaVencimento;
        this.periodicidade = periodicidade;
        this.emailCobranca = emailCobranca;
        this.aceitaDebitoRecorrente = aceitaDebitoRecorrente;
        this.jurosAoMes = jurosAoMes;
        this.multaPercentual = multaPercentual;
        this.descontoAntecipacao = descontoAntecipacao;
        this.diasCarencia = diasCarencia;
        this.protestarAposDias = protestarAposDias;
        this.instrucoes = instrucoes;
        this.atualizadoEm = OffsetDateTime.now();
        if (fonte != null && fonte != Fonte.MANUAL) {
            this.fonte = fonte.name();
            this.sincronizadoDeForaEm = OffsetDateTime.now();
        } else if (fonte != null) {
            this.fonte = fonte.name();
        }
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getFormaPreferida() {
        return formaPreferida;
    }

    public String getChavePix() {
        return chavePix;
    }

    public String getBanco() {
        return banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getConta() {
        return conta;
    }

    public String getTitular() {
        return titular;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public String getPeriodicidade() {
        return periodicidade;
    }

    public String getEmailCobranca() {
        return emailCobranca;
    }

    public boolean isAceitaDebitoRecorrente() {
        return aceitaDebitoRecorrente;
    }

    public BigDecimal getJurosAoMes() {
        return jurosAoMes;
    }

    public BigDecimal getMultaPercentual() {
        return multaPercentual;
    }

    public BigDecimal getDescontoAntecipacao() {
        return descontoAntecipacao;
    }

    public Integer getDiasCarencia() {
        return diasCarencia;
    }

    public Integer getProtestarAposDias() {
        return protestarAposDias;
    }

    public String getInstrucoes() {
        return instrucoes;
    }

    public String getFonte() {
        return fonte;
    }

    public OffsetDateTime getSincronizadoDeForaEm() {
        return sincronizadoDeForaEm;
    }
}
