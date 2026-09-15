package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * De onde sai o dinheiro.
 *
 * Toda conta tem um titular identificado. Conta pessoal de sócio, quando
 * existir, fica na entidade dele, e não misturada com a da empresa.
 */
@Entity
@Table(name = "conta_financeira")
public class ContaFinanceira {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nome;

    /** CORRENTE, POUPANCA, CAIXA, CARTAO ou APLICACAO. */
    @Column(nullable = false)
    private String tipo = "CORRENTE";

    private String banco;
    private String agencia;
    private String numero;
    private String titular;

    @Column(name = "saldo_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected ContaFinanceira() {
        // exigido pelo JPA
    }

    public ContaFinanceira(Empresa empresa, String nome, String titular) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.nome = nome;
        this.titular = titular;
    }

    public void ajustar(String nome, String tipo, String banco, String agencia, String numero,
                        String titular, BigDecimal saldoInicial, boolean ativo) {
        this.nome = nome;
        this.tipo = tipo;
        this.banco = banco;
        this.agencia = agencia;
        this.numero = numero;
        this.titular = titular;
        this.saldoInicial = saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
        this.ativo = ativo;
    }

    public String getResumo() {
        if (banco == null || banco.isBlank()) {
            return nome;
        }
        return nome + " · " + banco;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTipo() {
        return tipo;
    }

    public String getBanco() {
        return banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getNumero() {
        return numero;
    }

    public String getTitular() {
        return titular;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
