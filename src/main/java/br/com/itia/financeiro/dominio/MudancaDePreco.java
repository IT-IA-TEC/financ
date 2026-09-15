package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do histórico de preço.
 *
 * Cada troca de valor escreve uma linha aqui, com o que era, o que passou a
 * ser, desde quando vale, por quê e quem mudou. O histórico não é apagado, e é
 * ele que sustenta a regra de que mudar o preço hoje não mexe no que já foi
 * registrado ontem.
 */
@Entity
@Table(name = "servico_preco_historico")
public class MudancaDePreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "o_que_mudou", nullable = false)
    private String oQueMudou;

    @Column(name = "forma_preco")
    private String formaPreco;

    @Column(name = "valor_anterior", precision = 14, scale = 2)
    private BigDecimal valorAnterior;

    @Column(name = "valor_novo", precision = 14, scale = 2)
    private BigDecimal valorNovo;

    private String unidade;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    private String justificativa;

    @Column(nullable = false)
    private OffsetDateTime quando = OffsetDateTime.now();

    private String quem;

    protected MudancaDePreco() {
        // exigido pelo JPA
    }

    public MudancaDePreco(UUID empresaId, UUID servicoId, UUID itemId, String oQueMudou,
                          String formaPreco, BigDecimal valorAnterior, BigDecimal valorNovo,
                          String unidade, LocalDate vigenciaInicio, String justificativa,
                          String quem) {
        this.empresaId = empresaId;
        this.servicoId = servicoId;
        this.itemId = itemId;
        this.oQueMudou = oQueMudou;
        this.formaPreco = formaPreco;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.unidade = unidade;
        this.vigenciaInicio = vigenciaInicio;
        this.justificativa = justificativa;
        this.quem = quem;
    }

    public String getOQueMudou() {
        return oQueMudou;
    }

    public String getFormaPreco() {
        return formaPreco;
    }

    public BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public BigDecimal getValorNovo() {
        return valorNovo;
    }

    public String getUnidade() {
        return unidade;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public OffsetDateTime getQuando() {
        return quando;
    }

    public String getQuem() {
        return quem;
    }
}
