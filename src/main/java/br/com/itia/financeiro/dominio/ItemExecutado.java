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

/** Um item que a equipe de fato executou dentro do atendimento. */
@Entity
@Table(name = "servico_realizado_item")
public class ItemExecutado {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_id", nullable = false)
    private ServicoRealizado realizado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemDeServico item;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    private String observacao;

    protected ItemExecutado() {
        // exigido pelo JPA
    }

    public ItemExecutado(ServicoRealizado realizado, ItemDeServico item,
                         BigDecimal quantidade, String observacao) {
        this.id = UUID.randomUUID();
        this.empresa = realizado.getEmpresa();
        this.realizado = realizado;
        this.item = item;
        this.quantidade = quantidade == null ? BigDecimal.ONE : quantidade;
        this.observacao = observacao;
    }

    public UUID getId() {
        return id;
    }

    public ServicoRealizado getRealizado() {
        return realizado;
    }

    public ItemDeServico getItem() {
        return item;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getObservacao() {
        return observacao;
    }
}
