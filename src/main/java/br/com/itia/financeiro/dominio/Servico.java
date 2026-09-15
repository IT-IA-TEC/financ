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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um serviço do catálogo: o que a empresa faz.
 *
 * O preço pode ser nulo de propósito. Preço não definido é diferente de preço
 * zero: o primeiro quer dizer "ainda não decidimos", o segundo quer dizer
 * "é de graça". Misturar os dois é o erro clássico de catálogo.
 */
@Entity
@Table(name = "servico")
public class Servico {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @Column(name = "responsavel_padrao")
    private String responsavelPadrao;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_preco", nullable = false)
    private FormaDePreco formaPreco = FormaDePreco.VALOR_UNICO;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeDeCobranca unidade;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @OneToMany(mappedBy = "servico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("ordem")
    private List<ItemDeServico> itens = new ArrayList<>();

    protected Servico() {
        // exigido pelo JPA
    }

    public Servico(Empresa empresa, String codigo, String nome, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.codigo = codigo;
        this.nome = nome;
        this.criadoPor = criadoPor;
    }

    public void ajustarDadosGerais(String nome, String descricao, Departamento departamento,
                                   String responsavelPadrao, boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.departamento = departamento;
        this.responsavelPadrao = responsavelPadrao;
        this.ativo = ativo;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void ajustarPreco(FormaDePreco formaPreco, BigDecimal valor,
                             UnidadeDeCobranca unidade, LocalDate vigenciaInicio) {
        this.formaPreco = formaPreco;
        // Na soma dos itens o servico nao tem valor proprio.
        this.valor = formaPreco == FormaDePreco.SOMA_DOS_ITENS ? null : valor;
        this.unidade = unidade;
        this.vigenciaInicio = vigenciaInicio;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void inativar() {
        this.ativo = false;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void reativar() {
        this.ativo = true;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void receber(ItemDeServico item) {
        itens.add(item);
    }

    /**
     * O que o serviço custa hoje, pela forma escolhida.
     *
     * Item incluído no preço do serviço nunca entra na soma de novo: essa é a
     * regra que impede a cobrança em dobro.
     */
    public BigDecimal getTotalPadrao() {
        BigDecimal adicionais = itens.stream()
                .filter(ItemDeServico::isAtivo)
                .filter(i -> i.getTratamentoPreco().cobraSeparado())
                .filter(ItemDeServico::isObrigatorio)
                .map(ItemDeServico::getValorTotalPadrao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return switch (formaPreco) {
            case VALOR_UNICO -> valor;
            case SOMA_DOS_ITENS -> itens.stream()
                    .filter(ItemDeServico::isAtivo)
                    .filter(ItemDeServico::isObrigatorio)
                    .map(ItemDeServico::getValorTotalPadrao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case BASE_MAIS_ADICIONAIS -> valor == null ? null : valor.add(adicionais);
        };
    }

    /** Texto curto do preço, para a lista. */
    public String getPrecoResumido() {
        BigDecimal total = getTotalPadrao();
        if (formaPreco == FormaDePreco.SOMA_DOS_ITENS && itens.isEmpty()) {
            return "soma dos itens";
        }
        if (total == null) {
            return "preço não definido";
        }
        String texto = "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", total);
        if (unidade != null) {
            texto = texto + " por " + unidade.getNome();
        }
        return texto;
    }

    public boolean isPrecoDefinido() {
        return getTotalPadrao() != null;
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public String getResponsavelPadrao() {
        return responsavelPadrao;
    }

    public FormaDePreco getFormaPreco() {
        return formaPreco;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public UnidadeDeCobranca getUnidade() {
        return unidade;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public List<ItemDeServico> getItens() {
        return itens;
    }

    public List<ItemDeServico> getItensAtivos() {
        return itens.stream().filter(ItemDeServico::isAtivo).toList();
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
