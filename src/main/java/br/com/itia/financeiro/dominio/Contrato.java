package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O contrato entre a empresa e o cliente.
 *
 * Mora na PESSOA, não na unidade. Uma pessoa com cinco CNPJs pode ter um único
 * contrato cobrindo todos, ou um contrato por CNPJ. Guardar contrato dentro de
 * cada unidade obrigaria a repetir o mesmo documento cinco vezes.
 */
@Entity
@Table(name = "contrato")
public class Contrato {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id", nullable = false)
    private UUID pagadorId;

    /** Quando vazio, o contrato cobre todas as unidades da pessoa. */
    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(nullable = false)
    private String numero;

    private String descricao;
    private LocalDate inicio;
    private LocalDate fim;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "indice_reajuste")
    private String indiceReajuste;

    @Column(name = "mes_reajuste")
    private Integer mesReajuste;

    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    private String periodicidade;

    @Column(name = "responsavel_comercial")
    private String responsavelComercial;

    @Column(nullable = false)
    private String situacao = "ATIVO";

    private String observacao;

    @Column(nullable = false)
    private String fonte = "MANUAL";

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @Transient
    private List<ServicoDoContrato> servicos = new ArrayList<>();

    @Transient
    private String nomeDaUnidade;

    protected Contrato() {
        // exigido pelo JPA
    }

    public Contrato(UUID empresaId, UUID pagadorId, UUID unidadeId, String numero,
                    String criadoPor) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.unidadeId = unidadeId;
        this.numero = numero;
        this.criadoPor = criadoPor;
    }

    public void ajustar(UUID unidadeId, String descricao, LocalDate inicio, LocalDate fim,
                        BigDecimal valor, String indiceReajuste, Integer mesReajuste,
                        Integer diaVencimento, String periodicidade, String responsavelComercial,
                        String situacao, String observacao) {
        this.unidadeId = unidadeId;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.valor = valor;
        this.indiceReajuste = indiceReajuste;
        this.mesReajuste = mesReajuste;
        this.diaVencimento = diaVencimento;
        this.periodicidade = periodicidade;
        this.responsavelComercial = responsavelComercial;
        this.situacao = situacao;
        this.observacao = observacao;
    }

    public boolean estaVigente(LocalDate hoje) {
        if (!"ATIVO".equals(situacao)) {
            return false;
        }
        if (inicio != null && inicio.isAfter(hoje)) {
            return false;
        }
        return fim == null || !fim.isBefore(hoje);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagadorId() {
        return pagadorId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getNumero() {
        return numero;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getIndiceReajuste() {
        return indiceReajuste;
    }

    public Integer getMesReajuste() {
        return mesReajuste;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public String getPeriodicidade() {
        return periodicidade;
    }

    public String getResponsavelComercial() {
        return responsavelComercial;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public List<ServicoDoContrato> getServicos() {
        return servicos;
    }

    public void receberServicos(List<ServicoDoContrato> servicos) {
        this.servicos = servicos;
    }

    public String getNomeDaUnidade() {
        return nomeDaUnidade;
    }

    public void receberNomeDaUnidade(String nomeDaUnidade) {
        this.nomeDaUnidade = nomeDaUnidade;
    }
}
