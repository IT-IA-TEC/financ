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
 * Uma linha da composição do pacote: o que está incluído e até quanto.
 *
 * Quantidade vazia quer dizer ilimitado, e não zero. Por isso a quantidade
 * aceita nulo e existe a marcação própria de ilimitado.
 */
@Entity
@Table(name = "pacote_composicao")
public class ItemDoPacote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pacote_id", nullable = false)
    private Pacote pacote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbrangenciaDoPacote abrangencia = AbrangenciaDoPacote.SERVICO_COMPLETO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemDeServico item;

    @Column(nullable = false)
    private boolean ilimitado = false;

    @Column(name = "quantidade_incluida", precision = 10, scale = 2)
    private BigDecimal quantidadeIncluida;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_limite", nullable = false)
    private PeriodoDoLimite periodoLimite = PeriodoDoLimite.POR_PERIODO_DO_PACOTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tratamento_excedente", nullable = false)
    private TratamentoDoExcedente tratamentoExcedente = TratamentoDoExcedente.PRECO_DO_CATALOGO;

    private String observacao;

    @Column(nullable = false)
    private int ordem = 1;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected ItemDoPacote() {
        // exigido pelo JPA
    }

    public ItemDoPacote(Pacote pacote, Servico servico, int ordem) {
        this.id = UUID.randomUUID();
        this.empresa = pacote.getEmpresa();
        this.pacote = pacote;
        this.servico = servico;
        this.ordem = ordem;
    }

    public void ajustar(Servico servico, AbrangenciaDoPacote abrangencia, ItemDeServico item,
                        boolean ilimitado, BigDecimal quantidadeIncluida,
                        PeriodoDoLimite periodoLimite, TratamentoDoExcedente tratamentoExcedente,
                        String observacao, int ordem) {
        this.servico = servico;
        this.abrangencia = abrangencia;
        // Item so existe quando a linha e de um item escolhido.
        this.item = abrangencia == AbrangenciaDoPacote.ITEM_ESCOLHIDO ? item : null;
        this.ilimitado = ilimitado;
        // Ilimitado nao guarda numero: numero guardado junto com ilimitado vira
        // duvida na hora de conferir a que o cliente tem direito.
        this.quantidadeIncluida = ilimitado ? null : quantidadeIncluida;
        this.periodoLimite = periodoLimite;
        this.tratamentoExcedente = tratamentoExcedente;
        this.observacao = observacao;
        this.ordem = ordem;
    }

    /** O que está incluído, em uma linha de texto. */
    public String getResumoDoQueEntra() {
        if (abrangencia == AbrangenciaDoPacote.ITEM_ESCOLHIDO && item != null) {
            return servico.getNome() + ": " + item.getNome();
        }
        return servico.getNome();
    }

    /** O limite, em uma linha de texto. */
    public String getResumoDoLimite() {
        if (ilimitado) {
            return "ilimitado";
        }
        if (quantidadeIncluida == null) {
            return "sem limite definido";
        }
        String numero = quantidadeIncluida.stripTrailingZeros().toPlainString();
        return numero + " " + periodoLimite.getRotulo().toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public Pacote getPacote() {
        return pacote;
    }

    public Servico getServico() {
        return servico;
    }

    public AbrangenciaDoPacote getAbrangencia() {
        return abrangencia;
    }

    public ItemDeServico getItem() {
        return item;
    }

    public boolean isIlimitado() {
        return ilimitado;
    }

    public BigDecimal getQuantidadeIncluida() {
        return quantidadeIncluida;
    }

    public PeriodoDoLimite getPeriodoLimite() {
        return periodoLimite;
    }

    public TratamentoDoExcedente getTratamentoExcedente() {
        return tratamentoExcedente;
    }

    public String getObservacao() {
        return observacao;
    }

    public int getOrdem() {
        return ordem;
    }
}
