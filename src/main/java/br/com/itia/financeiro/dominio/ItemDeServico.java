package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma parte que compõe um serviço.
 *
 * Serviço simples pode não ter item nenhum. Item que já foi usado é inativado,
 * nunca apagado, para o histórico continuar fazendo sentido.
 */
@Entity
@Table(name = "servico_item")
public class ItemDeServico {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private boolean obrigatorio = true;

    @Column(name = "quantidade_padrao", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidadePadrao = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tratamento_preco", nullable = false)
    private TratamentoDePreco tratamentoPreco = TratamentoDePreco.INCLUIDO;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeDeCobranca unidade;

    @Column(nullable = false)
    private int ordem = 1;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected ItemDeServico() {
        // exigido pelo JPA
    }

    public ItemDeServico(Servico servico, String nome, int ordem) {
        this.id = UUID.randomUUID();
        this.empresa = servico.getEmpresa();
        this.servico = servico;
        this.nome = nome;
        this.ordem = ordem;
    }

    public void ajustar(String nome, String descricao, boolean obrigatorio,
                        BigDecimal quantidadePadrao, TratamentoDePreco tratamentoPreco,
                        BigDecimal valor, UnidadeDeCobranca unidade, int ordem, boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.obrigatorio = obrigatorio;
        this.quantidadePadrao = quantidadePadrao == null ? BigDecimal.ONE : quantidadePadrao;
        this.tratamentoPreco = tratamentoPreco;
        // Item incluido no servico nao carrega valor proprio: o valor dele ja
        // esta no preco do servico, e guardar um numero aqui viraria cobranca
        // em dobro na primeira distracao.
        this.valor = tratamentoPreco.cobraSeparado() ? valor : null;
        this.unidade = unidade;
        this.ordem = ordem;
        this.ativo = ativo;
    }

    public void inativar() {
        this.ativo = false;
    }

    /** Quanto este item soma, na quantidade padrão dele. */
    public BigDecimal getValorTotalPadrao() {
        if (!tratamentoPreco.cobraSeparado() || valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(quantidadePadrao);
    }

    public UUID getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isObrigatorio() {
        return obrigatorio;
    }

    public BigDecimal getQuantidadePadrao() {
        return quantidadePadrao;
    }

    public TratamentoDePreco getTratamentoPreco() {
        return tratamentoPreco;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public UnidadeDeCobranca getUnidade() {
        return unidade;
    }

    public int getOrdem() {
        return ordem;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
