package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Protesto, negativação ou qualquer restrição registrada sobre a pessoa. */
@Entity
@Table(name = "restricao")
public class Restricao {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id", nullable = false)
    private UUID pagadorId;

    @Column(nullable = false)
    private String tipo;

    private String origem;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    private LocalDate data;

    @Column(nullable = false)
    private String situacao = "ATIVA";

    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Restricao() {
        // exigido pelo JPA
    }

    public Restricao(UUID empresaId, UUID pagadorId, String tipo, String origem,
                     BigDecimal valor, LocalDate data, String observacao) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.tipo = tipo;
        this.origem = origem;
        this.valor = valor;
        this.data = data;
        this.observacao = observacao;
    }

    public void baixar() {
        this.situacao = "BAIXADA";
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getOrigem() {
        return origem;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getObservacao() {
        return observacao;
    }
}
