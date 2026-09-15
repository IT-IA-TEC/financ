package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Uma linha do que está sendo cobrado, com quantidade e valor. */
@Entity
@Table(name = "cobranca_item")
public class ItemDaCobranca {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cobranca_id", nullable = false)
    private Cobranca cobranca;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(name = "valor_unitario", precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemDeServico item;

    protected ItemDaCobranca() {
        // exigido pelo JPA
    }

    public ItemDaCobranca(Cobranca cobranca, String descricao, BigDecimal quantidade,
                          BigDecimal valorUnitario, ItemDeServico item) {
        this.id = UUID.randomUUID();
        this.empresa = cobranca.getEmpresa();
        this.cobranca = cobranca;
        this.descricao = descricao;
        this.quantidade = quantidade == null ? BigDecimal.ONE : quantidade;
        this.valorUnitario = valorUnitario;
        this.item = item;
    }

    public BigDecimal getTotal() {
        return valorUnitario == null ? BigDecimal.ZERO : valorUnitario.multiply(quantidade);
    }

    public UUID getId() {
        return id;
    }

    public Cobranca getCobranca() {
        return cobranca;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public ItemDeServico getItem() {
        return item;
    }
}
