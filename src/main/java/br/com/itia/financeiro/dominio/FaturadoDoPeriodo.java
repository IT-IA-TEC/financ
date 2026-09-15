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
 * O valor que veio de fora para um cliente, naquele mês.
 *
 * É a entrada do faturado: quando o valor da conta não é fixo, ele chega por
 * arquivo e manda no valor da leva. Fica guardado com a origem, para o total
 * do mês poder ser explicado linha a linha depois.
 */
@Entity
@Table(name = "faturado_do_periodo")
public class FaturadoDoPeriodo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String referencia;

    /** O CPF ou CNPJ que veio no arquivo, do jeito que veio. */
    @Column(nullable = false)
    private String documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_id")
    private Pagador pagador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private ClienteEspelho unidade;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    private String observacao;

    /** O nome do arquivo de onde a linha veio. */
    private String origem;

    @Column(name = "importado_em", nullable = false)
    private OffsetDateTime importadoEm = OffsetDateTime.now();

    @Column(name = "importado_por")
    private String importadoPor;

    protected FaturadoDoPeriodo() {
        // exigido pelo JPA
    }

    public FaturadoDoPeriodo(Empresa empresa, String referencia, String documento,
                             BigDecimal valor, String origem, String importadoPor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.referencia = referencia;
        this.documento = documento;
        this.valor = valor;
        this.origem = origem;
        this.importadoPor = importadoPor;
    }

    public void amarrarAoCliente(Pagador pagador, ClienteEspelho unidade) {
        this.pagador = pagador;
        this.unidade = unidade;
    }

    public void ajustar(BigDecimal valor, String observacao) {
        this.valor = valor;
        this.observacao = observacao;
    }

    /** Linha que não achou dono fica visível, e não vira cobrança de ninguém. */
    public boolean semDono() {
        return pagador == null;
    }

    public UUID getId() {
        return id;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getDocumento() {
        return documento;
    }

    public Pagador getPagador() {
        return pagador;
    }

    public ClienteEspelho getUnidade() {
        return unidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getOrigem() {
        return origem;
    }

    public OffsetDateTime getImportadoEm() {
        return importadoEm;
    }

    public String getImportadoPor() {
        return importadoPor;
    }
}
