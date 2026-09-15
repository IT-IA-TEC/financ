package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.UUID;

/** Um serviço do catálogo dentro de um contrato. */
@Entity
@Table(name = "contrato_servico")
public class ServicoDoContrato {

    @Id
    private UUID id;

    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;

    @Column(name = "servico_id")
    private UUID servicoId;

    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @Transient
    private String nomeDoServico;

    protected ServicoDoContrato() {
        // exigido pelo JPA
    }

    public ServicoDoContrato(UUID contratoId, UUID servicoId, String descricao,
                             BigDecimal quantidade, BigDecimal valor) {
        this.id = UUID.randomUUID();
        this.contratoId = contratoId;
        this.servicoId = servicoId;
        this.descricao = descricao;
        this.quantidade = quantidade == null ? BigDecimal.ONE : quantidade;
        this.valor = valor;
    }

    public BigDecimal getTotal() {
        return valor == null ? BigDecimal.ZERO : valor.multiply(quantidade);
    }

    public UUID getId() {
        return id;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public UUID getServicoId() {
        return servicoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getNomeDoServico() {
        return nomeDoServico;
    }

    public void receberNomeDoServico(String nomeDoServico) {
        this.nomeDoServico = nomeDoServico;
    }
}
