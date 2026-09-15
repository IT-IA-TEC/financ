package br.com.itia.financeiro.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um serviço realizado: o que a equipe fez para o cliente.
 *
 * O registro guarda o tratamento já decidido. Só o que está marcado como
 * incluído no pacote consome o limite, e é por isso que a conta do que o
 * cliente ainda tem direito nunca precisa adivinhar nada.
 */
@Entity
@Table(name = "servico_realizado")
public class ServicoRealizado {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagador_id", nullable = false)
    private Pagador pagador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private ClienteEspelho unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratacao_id")
    private ContratacaoDePacote contratacao;

    @Column(name = "realizado_em", nullable = false)
    private LocalDate realizadoEm = LocalDate.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TratamentoDoAtendimento tratamento = TratamentoDoAtendimento.COBRADO_A_PARTE;

    @Column(name = "valor_cobrado", precision = 14, scale = 2)
    private BigDecimal valorCobrado;

    @Column(precision = 14, scale = 2)
    private BigDecimal desconto;

    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoDoAtendimento situacao = SituacaoDoAtendimento.REGISTRADO;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @OneToMany(mappedBy = "realizado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemExecutado> itens = new ArrayList<>();

    protected ServicoRealizado() {
        // exigido pelo JPA
    }

    public ServicoRealizado(Empresa empresa, Pagador pagador, Servico servico, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.pagador = pagador;
        this.servico = servico;
        this.criadoPor = criadoPor;
    }

    public void ajustar(Pagador pagador, ClienteEspelho unidade, Servico servico,
                        ContratacaoDePacote contratacao, LocalDate realizadoEm,
                        BigDecimal quantidade, String responsavel,
                        TratamentoDoAtendimento tratamento, BigDecimal valorCobrado,
                        BigDecimal desconto, String observacao) {
        this.pagador = pagador;
        this.unidade = unidade;
        this.servico = servico;
        // A contratacao so fica amarrada quando o atendimento entra pelo pacote.
        this.contratacao = tratamento == TratamentoDoAtendimento.INCLUIDO_NO_PACOTE
                ? contratacao : null;
        this.realizadoEm = realizadoEm == null ? LocalDate.now() : realizadoEm;
        this.quantidade = quantidade == null ? BigDecimal.ONE : quantidade;
        this.responsavel = responsavel;
        this.tratamento = tratamento;
        // Atendimento que nao gera cobranca nao guarda valor nem desconto:
        // numero solto aqui vira cobranca indevida na primeira distracao.
        this.valorCobrado = tratamento.geraCobranca() ? valorCobrado : null;
        this.desconto = tratamento == TratamentoDoAtendimento.COM_DESCONTO ? desconto : null;
        this.observacao = observacao;
    }

    public void receber(ItemExecutado item) {
        itens.add(item);
    }

    public void cancelar() {
        this.situacao = SituacaoDoAtendimento.CANCELADO;
    }

    public void reabrir() {
        this.situacao = SituacaoDoAtendimento.REGISTRADO;
    }

    /** Quanto este atendimento gera de cobrança, já com o desconto. */
    public BigDecimal getValorAPagar() {
        if (!tratamento.geraCobranca() || valorCobrado == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal abatimento = desconto == null ? BigDecimal.ZERO : desconto;
        BigDecimal total = valorCobrado.subtract(abatimento);
        return total.signum() < 0 ? BigDecimal.ZERO : total;
    }

    public String getValorResumido() {
        if (!tratamento.geraCobranca()) {
            return "sem cobrança";
        }
        if (valorCobrado == null) {
            return "valor não definido";
        }
        return "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", getValorAPagar());
    }

    /** Se este atendimento conta contra o limite do pacote. */
    public boolean consomeLimite() {
        return situacao == SituacaoDoAtendimento.REGISTRADO && tratamento.consomeLimite();
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Pagador getPagador() {
        return pagador;
    }

    public ClienteEspelho getUnidade() {
        return unidade;
    }

    public Servico getServico() {
        return servico;
    }

    public ContratacaoDePacote getContratacao() {
        return contratacao;
    }

    public LocalDate getRealizadoEm() {
        return realizadoEm;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public TratamentoDoAtendimento getTratamento() {
        return tratamento;
    }

    public BigDecimal getValorCobrado() {
        return valorCobrado;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public String getObservacao() {
        return observacao;
    }

    public SituacaoDoAtendimento getSituacao() {
        return situacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public List<ItemExecutado> getItens() {
        return itens;
    }
}
