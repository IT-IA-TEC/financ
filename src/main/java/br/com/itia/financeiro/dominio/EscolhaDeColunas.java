package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** As colunas que uma empresa escolheu ver na tela de clientes. */
@Entity
@Table(name = "carteira_colunas")
public class EscolhaDeColunas {

    @Id
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false)
    private String colunas;

    @Column(name = "salvo_em", nullable = false)
    private OffsetDateTime salvoEm = OffsetDateTime.now();

    @Column(name = "salvo_por")
    private String salvoPor;

    protected EscolhaDeColunas() {
        // exigido pelo JPA
    }

    public EscolhaDeColunas(UUID empresaId, String colunas, String salvoPor) {
        this.empresaId = empresaId;
        this.colunas = colunas;
        this.salvoPor = salvoPor;
    }

    public void trocar(String colunas, String salvoPor) {
        this.colunas = colunas;
        this.salvoPor = salvoPor;
        this.salvoEm = OffsetDateTime.now();
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getColunas() {
        return colunas;
    }
}
