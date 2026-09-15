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

/**
 * Uma linha da composição da conta.
 *
 * É daqui que sai a explicação de qualquer total: cada linha diz o que foi
 * pago, qual área absorve, a quem se refere e qual equipamento envolve.
 * Quando o valor veio de rateio, o critério fica escrito na própria linha.
 */
@Entity
@Table(name = "obrigacao_item")
public class ItemDaObrigacao {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obrigacao_id", nullable = false)
    private Obrigacao obrigacao;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natureza_id")
    private Natureza natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroDeCusto centroDeCusto;

    private String pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bem_id")
    private Bem bem;

    @Column(name = "criterio_rateio")
    private String criterioRateio;

    @Column(precision = 7, scale = 4)
    private BigDecimal percentual;

    @Column(nullable = false)
    private int ordem = 1;

    protected ItemDaObrigacao() {
        // exigido pelo JPA
    }

    public ItemDaObrigacao(Obrigacao obrigacao, String descricao, int ordem) {
        this.id = UUID.randomUUID();
        this.empresa = obrigacao.getEmpresa();
        this.obrigacao = obrigacao;
        this.descricao = descricao;
        this.ordem = ordem;
    }

    public void ajustar(String descricao, BigDecimal quantidade, BigDecimal valor,
                        Natureza natureza, CentroDeCusto centroDeCusto, String pessoa, Bem bem,
                        String criterioRateio, BigDecimal percentual, int ordem) {
        this.descricao = descricao;
        this.quantidade = quantidade == null ? BigDecimal.ONE : quantidade;
        this.valor = valor == null ? BigDecimal.ZERO : valor;
        this.natureza = natureza;
        this.centroDeCusto = centroDeCusto;
        this.pessoa = pessoa;
        this.bem = bem;
        this.criterioRateio = criterioRateio;
        this.percentual = percentual;
        this.ordem = ordem;
    }

    public BigDecimal getTotal() {
        return valor.multiply(quantidade);
    }

    public boolean veioDeRateio() {
        return criterioRateio != null && !criterioRateio.isBlank();
    }

    public UUID getId() {
        return id;
    }

    public Obrigacao getObrigacao() {
        return obrigacao;
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

    public Natureza getNatureza() {
        return natureza;
    }

    public CentroDeCusto getCentroDeCusto() {
        return centroDeCusto;
    }

    public String getPessoa() {
        return pessoa;
    }

    public Bem getBem() {
        return bem;
    }

    public String getCriterioRateio() {
        return criterioRateio;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public int getOrdem() {
        return ordem;
    }
}
