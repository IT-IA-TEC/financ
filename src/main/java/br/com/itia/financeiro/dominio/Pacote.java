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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um pacote: o que o cliente passa a ter direito quando contrata.
 *
 * O pacote nunca cadastra serviço de novo. Ele aponta para o catálogo, e é por
 * isso que mudar a descrição de um serviço muda em todos os pacotes de uma vez.
 */
@Entity
@Table(name = "pacote")
public class Pacote {

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

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Periodicidade periodicidade = Periodicidade.MENSAL;

    @Column(name = "periodicidade_outra")
    private String periodicidadeOutra;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @OneToMany(mappedBy = "pacote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("ordem")
    private List<ItemDoPacote> composicao = new ArrayList<>();

    protected Pacote() {
        // exigido pelo JPA
    }

    public Pacote(Empresa empresa, String codigo, String nome, String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.codigo = codigo;
        this.nome = nome;
        this.criadoPor = criadoPor;
    }

    public void ajustar(String nome, String descricao, BigDecimal valor,
                        Periodicidade periodicidade, String periodicidadeOutra, boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.periodicidade = periodicidade;
        // O texto livre so faz sentido quando o intervalo e proprio.
        this.periodicidadeOutra = periodicidade == Periodicidade.OUTRA ? periodicidadeOutra : null;
        this.ativo = ativo;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void receber(ItemDoPacote linha) {
        composicao.add(linha);
    }

    public void inativar() {
        this.ativo = false;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void reativar() {
        this.ativo = true;
        this.atualizadoEm = OffsetDateTime.now();
    }

    /** Texto curto do valor, para a lista. */
    public String getValorResumido() {
        if (valor == null) {
            return "valor não definido";
        }
        return "R$ " + String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valor);
    }

    public String getPeriodicidadeResumida() {
        if (periodicidade == Periodicidade.OUTRA && periodicidadeOutra != null) {
            return periodicidadeOutra;
        }
        return periodicidade.getRotulo();
    }

    public boolean isValorDefinido() {
        return valor != null;
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

    public BigDecimal getValor() {
        return valor;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public String getPeriodicidadeOutra() {
        return periodicidadeOutra;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public List<ItemDoPacote> getComposicao() {
        return composicao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
