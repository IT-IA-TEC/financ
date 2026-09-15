package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do extrato do banco.
 *
 * O movimento é o que o banco diz que aconteceu. Conciliar é ligar essa linha
 * ao que o sistema já registrou, e não criar dinheiro: por isso o movimento
 * guarda só a ligação, e o valor continua morando no título ou na conta a
 * pagar.
 */
@Entity
@Table(name = "movimento_bancario")
public class MovimentoBancario {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "conta_id")
    private UUID contaId;

    @Column(name = "importacao_id")
    private UUID importacaoId;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDate ocorridoEm;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    /** CREDITO quando entrou, DEBITO quando saiu. */
    @Column(nullable = false)
    private String tipo;

    private String descricao;

    /** O que o banco usa para identificar aquela transação. */
    private String identificador;

    /** PENDENTE, CONCILIADO ou IGNORADO. */
    @Column(nullable = false)
    private String situacao = "PENDENTE";

    @Column(name = "titulo_id")
    private UUID tituloId;

    @Column(name = "obrigacao_id")
    private UUID obrigacaoId;

    private String motivo;

    @Column(name = "conciliado_em")
    private OffsetDateTime conciliadoEm;

    @Column(name = "conciliado_por")
    private String conciliadoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected MovimentoBancario() {
        // exigido pelo JPA
    }

    public MovimentoBancario(UUID empresaId, UUID contaId, UUID importacaoId, LocalDate ocorridoEm,
                             BigDecimal valor, String tipo, String descricao,
                             String identificador) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.contaId = contaId;
        this.importacaoId = importacaoId;
        this.ocorridoEm = ocorridoEm;
        this.valor = valor;
        this.tipo = tipo;
        this.descricao = descricao;
        this.identificador = identificador;
    }

    public void conciliarComTitulo(UUID tituloId, String quem) {
        this.tituloId = tituloId;
        this.obrigacaoId = null;
        marcarConciliado(quem);
    }

    public void conciliarComConta(UUID obrigacaoId, String quem) {
        this.obrigacaoId = obrigacaoId;
        this.tituloId = null;
        marcarConciliado(quem);
    }

    private void marcarConciliado(String quem) {
        this.situacao = "CONCILIADO";
        this.conciliadoEm = OffsetDateTime.now();
        this.conciliadoPor = quem;
    }

    /** Tarifa, transferência entre contas próprias, o que não tem par no sistema. */
    public void ignorar(String motivo, String quem) {
        this.situacao = "IGNORADO";
        this.motivo = motivo;
        this.conciliadoEm = OffsetDateTime.now();
        this.conciliadoPor = quem;
    }

    public void voltarParaPendente() {
        this.situacao = "PENDENTE";
        this.tituloId = null;
        this.obrigacaoId = null;
        this.motivo = null;
        this.conciliadoEm = null;
        this.conciliadoPor = null;
    }

    public boolean entrou() {
        return "CREDITO".equals(tipo);
    }

    public boolean pendente() {
        return "PENDENTE".equals(situacao);
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public LocalDate getOcorridoEm() {
        return ocorridoEm;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getSituacao() {
        return situacao;
    }

    public UUID getTituloId() {
        return tituloId;
    }

    public UUID getObrigacaoId() {
        return obrigacaoId;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getConciliadoEm() {
        return conciliadoEm;
    }

    public String getConciliadoPor() {
        return conciliadoPor;
    }
}
