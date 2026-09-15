package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O que a empresa decidiu sobre o risco daquela pessoa.
 *
 * Só guarda decisão humana: classificação, limite e bloqueio. O score de
 * comportamento é calculado do histórico de pagamento e não fica gravado, para
 * não envelhecer em silêncio.
 */
@Entity
@Table(name = "analise_credito")
public class AnaliseDeCredito {

    @Id
    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    private String classificacao;

    @Column(name = "limite_credito", precision = 14, scale = 2)
    private BigDecimal limiteCredito;

    @Column(nullable = false)
    private boolean bloqueado = false;

    @Column(name = "motivo_bloqueio")
    private String motivoBloqueio;

    @Column(name = "ultima_analise_em")
    private OffsetDateTime ultimaAnaliseEm;

    @Column(name = "analisado_por")
    private String analisadoPor;

    private String observacao;

    protected AnaliseDeCredito() {
        // exigido pelo JPA
    }

    public AnaliseDeCredito(UUID pagadorId, UUID empresaId) {
        this.pagadorId = pagadorId;
        this.empresaId = empresaId;
    }

    public void ajustar(String classificacao, BigDecimal limiteCredito, boolean bloqueado,
                        String motivoBloqueio, String observacao, String autor) {
        this.classificacao = classificacao;
        this.limiteCredito = limiteCredito;
        this.bloqueado = bloqueado;
        this.motivoBloqueio = motivoBloqueio;
        this.observacao = observacao;
        this.ultimaAnaliseEm = OffsetDateTime.now();
        this.analisadoPor = autor;
    }

    public UUID getPagadorId() {
        return pagadorId;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public String getMotivoBloqueio() {
        return motivoBloqueio;
    }

    public OffsetDateTime getUltimaAnaliseEm() {
        return ultimaAnaliseEm;
    }

    public String getAnalisadoPor() {
        return analisadoPor;
    }

    public String getObservacao() {
        return observacao;
    }
}
