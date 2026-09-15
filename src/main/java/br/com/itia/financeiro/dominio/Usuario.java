package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Quem usa o sistema. A mesma pessoa do ERP, ligada pelo erpUsuarioId.
 * O que ela pode ver fica em {@link UsuarioEmpresa}, uma linha por empresa.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "erp_usuario_id", unique = true)
    private UUID erpUsuarioId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String nome, String email, UUID erpUsuarioId) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.email = email;
        this.erpUsuarioId = erpUsuarioId;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public UUID getErpUsuarioId() {
        return erpUsuarioId;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
