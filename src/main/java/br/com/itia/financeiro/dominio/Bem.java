package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um bem do patrimônio, com número próprio.
 *
 * O número é o que liga a compra, as manutenções e o custo da área ao mesmo
 * equipamento. Estar sob a guarda de alguém não comprova mau uso: o motivo de
 * cada manutenção fica na obrigação, com relato e conclusão separados.
 */
@Entity
@Table(name = "bem")
public class Bem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "numero_patrimonial", nullable = false, updatable = false)
    private String numeroPatrimonial;

    @Column(nullable = false)
    private String descricao;

    private String tipo;
    private String marca;
    private String modelo;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroDeCusto centroDeCusto;

    private String responsavel;
    private String localizacao;

    /** EM_USO, EM_MANUTENCAO, PARADO, BAIXADO ou EMPRESTADO. */
    @Column(nullable = false)
    private String situacao = "EM_USO";

    @Column(name = "aquisicao_em")
    private LocalDate aquisicaoEm;

    @Column(name = "valor_aquisicao", precision = 14, scale = 2)
    private BigDecimal valorAquisicao;

    private String observacao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Bem() {
        // exigido pelo JPA
    }

    public Bem(Empresa empresa, String numeroPatrimonial, String descricao) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.numeroPatrimonial = numeroPatrimonial;
        this.descricao = descricao;
    }

    public void ajustar(String descricao, String tipo, String marca, String modelo,
                        String numeroSerie, CentroDeCusto centroDeCusto, String responsavel,
                        String localizacao, String situacao, LocalDate aquisicaoEm,
                        BigDecimal valorAquisicao, String observacao, boolean ativo) {
        this.descricao = descricao;
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.centroDeCusto = centroDeCusto;
        this.responsavel = responsavel;
        this.localizacao = localizacao;
        this.situacao = situacao;
        this.aquisicaoEm = aquisicaoEm;
        this.valorAquisicao = valorAquisicao;
        this.observacao = observacao;
        this.ativo = ativo;
    }

    public String getEtiqueta() {
        return numeroPatrimonial + " · " + descricao;
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroPatrimonial() {
        return numeroPatrimonial;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public CentroDeCusto getCentroDeCusto() {
        return centroDeCusto;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public String getSituacao() {
        return situacao;
    }

    public LocalDate getAquisicaoEm() {
        return aquisicaoEm;
    }

    public BigDecimal getValorAquisicao() {
        return valorAquisicao;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
