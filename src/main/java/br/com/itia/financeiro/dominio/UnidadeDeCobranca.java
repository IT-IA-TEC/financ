package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Por que unidade o serviço é cobrado: serviço, hora, atendimento, documento. */
@Entity
@Table(name = "unidade_cobranca")
public class UnidadeDeCobranca {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected UnidadeDeCobranca() {
        // exigido pelo JPA
    }

    public UnidadeDeCobranca(Empresa empresa, String nome) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.nome = nome;
    }

    public void ajustar(String nome, boolean ativo) {
        this.nome = nome;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
