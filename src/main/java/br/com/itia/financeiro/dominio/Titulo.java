package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A conta que o cliente tem para pagar.
 *
 * Regra de ouro deste arquivo: <b>saldo nunca e guardado, e sempre calculado</b>
 * a partir dos pagamentos. Assim nao existe a situacao classica de o campo dizer
 * uma coisa e a soma dizer outra.
 */
@Entity
@Table(name = "titulo")
public class Titulo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEspelho cliente;

    @Column(nullable = false, updatable = false)
    private Long numero;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoTitulo situacao = SituacaoTitulo.ABERTO;

    @Column(nullable = false)
    private String origem = "MANUAL";

    @Column(name = "identificador_pix", unique = true)
    private String identificadorPix;

    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "criado_por")
    private String criadoPor;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Column(name = "cancelado_por")
    private String canceladoPor;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @OneToMany(mappedBy = "titulo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pagamento> pagamentos = new ArrayList<>();

    protected Titulo() {
        // exigido pelo JPA
    }

    public Titulo(ClienteEspelho cliente, Long numero, LocalDate competencia, String descricao,
                  BigDecimal valor, LocalDate vencimento, String origem, String criadoPor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do titulo tem que ser maior que zero.");
        }
        if (vencimento == null) {
            throw new IllegalArgumentException("Todo titulo precisa de data de vencimento.");
        }
        this.id = UUID.randomUUID();
        // A empresa do titulo e sempre a do cliente. Nao existe titulo de uma
        // empresa preso a cliente de outra.
        this.empresa = cliente.getEmpresa();
        this.cliente = cliente;
        this.numero = numero;
        this.competencia = competencia;
        this.descricao = descricao;
        this.valor = valor;
        this.vencimento = vencimento;
        this.origem = origem == null ? "MANUAL" : origem;
        this.criadoPor = criadoPor;
        // O identificador que vai no QR do PIX. E ele que faz a baixa automatica
        // acontecer sem ninguem precisar ler comprovante.
        this.identificadorPix = this.empresa.getApelido().toUpperCase()
                + String.format("%08d", numero);
    }

    /** Quanto ja entrou neste titulo. */
    public BigDecimal getTotalPago() {
        return pagamentos.stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Quanto ainda falta. Nunca negativo: o que sobra vira credito do cliente. */
    public BigDecimal getSaldo() {
        BigDecimal saldo = valor.subtract(getTotalPago());
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    /** O que o cliente pagou a mais, se pagou. */
    public BigDecimal getCredito() {
        BigDecimal sobra = getTotalPago().subtract(valor);
        return sobra.signum() > 0 ? sobra : BigDecimal.ZERO;
    }

    public boolean estaVencido(LocalDate hoje) {
        return situacao != SituacaoTitulo.PAGO
                && situacao != SituacaoTitulo.CANCELADO
                && situacao != SituacaoTitulo.EM_ACORDO
                && vencimento.isBefore(hoje);
    }

    /**
     * Se este documento entra nas contas de quanto a empresa tem a receber.
     *
     * Cancelado nao entra porque nao existe mais. Em acordo nao entra porque
     * quem esta sendo cobrado agora sao as parcelas do acordo, e contar os
     * dois seria enxergar o dobro do dinheiro.
     */
    public boolean contaNoTotal() {
        return situacao != SituacaoTitulo.CANCELADO && situacao != SituacaoTitulo.EM_ACORDO;
    }

    /** Tira o documento da cobranca porque um acordo tomou o lugar dele. */
    public void entrarEmAcordo() {
        if (situacao == SituacaoTitulo.PAGO || situacao == SituacaoTitulo.CANCELADO) {
            throw new IllegalStateException("Documento pago ou cancelado nao entra em acordo.");
        }
        this.situacao = SituacaoTitulo.EM_ACORDO;
        this.atualizadoEm = OffsetDateTime.now();
    }

    /** Volta para a cobranca normal quando o acordo quebra. */
    public void sairDoAcordo() {
        if (situacao != SituacaoTitulo.EM_ACORDO) {
            return;
        }
        this.situacao = getTotalPago().signum() > 0
                ? SituacaoTitulo.PARCIAL : SituacaoTitulo.ABERTO;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public long diasDeAtraso(LocalDate hoje) {
        if (!estaVencido(hoje)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(vencimento, hoje);
    }

    /**
     * Recebe um pagamento e recalcula a situacao.
     *
     * Diferenca de ate um real conta como quitado, do jeito que a operacao ja
     * trabalha hoje.
     */
    public void receber(Pagamento pagamento) {
        if (situacao == SituacaoTitulo.EM_ACORDO) {
            throw new IllegalStateException(
                    "Este documento esta dentro de um acordo. Receba pela parcela do acordo.");
        }
        if (situacao == SituacaoTitulo.CANCELADO) {
            throw new IllegalStateException("Titulo cancelado nao recebe pagamento.");
        }
        pagamento.vincularA(this);
        pagamentos.add(pagamento);
        recalcularSituacao();
    }

    private void recalcularSituacao() {
        BigDecimal falta = valor.subtract(getTotalPago());
        if (falta.compareTo(TOLERANCIA) <= 0) {
            this.situacao = SituacaoTitulo.PAGO;
        } else if (getTotalPago().signum() > 0) {
            this.situacao = SituacaoTitulo.PARCIAL;
        } else {
            this.situacao = SituacaoTitulo.ABERTO;
        }
        this.atualizadoEm = OffsetDateTime.now();
    }

    /** Ate um real de diferenca o titulo e dado como quitado. */
    public static final BigDecimal TOLERANCIA = new BigDecimal("1.00");

    public void cancelar(String autor, String motivo) {
        if (getTotalPago().signum() > 0) {
            throw new IllegalStateException(
                    "Titulo que ja recebeu pagamento nao e cancelado. Faca um estorno.");
        }
        this.situacao = SituacaoTitulo.CANCELADO;
        this.canceladoEm = OffsetDateTime.now();
        this.canceladoPor = autor;
        this.motivoCancelamento = motivo;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public ClienteEspelho getCliente() {
        return cliente;
    }

    public Long getNumero() {
        return numero;
    }

    public LocalDate getCompetencia() {
        return competencia;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public SituacaoTitulo getSituacao() {
        return situacao;
    }

    public String getOrigem() {
        return origem;
    }

    public String getIdentificadorPix() {
        return identificadorPix;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<Pagamento> getPagamentos() {
        return List.copyOf(pagamentos);
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
