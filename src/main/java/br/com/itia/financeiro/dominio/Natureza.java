package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do plano gerencial: o que está sendo pago.
 *
 * A árvore é revisável e não substitui o plano contábil oficial. Natureza que
 * saiu de uso é inativada, nunca apagada, para o histórico continuar de pé.
 */
@Entity
@Table(name = "natureza")
public class Natureza {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pai_id")
    private Natureza pai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrupoDeNatureza grupo = GrupoDeNatureza.DESPESA;

    /**
     * Se o que entra nesta linha do plano vai para o livro caixa.
     *
     * A marca é do item, não do lançamento: quem lança não precisa lembrar
     * disso conta a conta.
     */
    @Column(name = "livro_caixa", nullable = false)
    private boolean livroCaixa;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Natureza() {
        // exigido pelo JPA
    }

    public Natureza(Empresa empresa, String codigo, String nome, GrupoDeNatureza grupo,
                    Natureza pai) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.codigo = codigo;
        this.nome = nome;
        this.grupo = grupo == null ? GrupoDeNatureza.DESPESA : grupo;
        this.pai = pai;
    }

    public void ajustar(String nome, GrupoDeNatureza grupo, Natureza pai, boolean ativo) {
        this.nome = nome;
        this.grupo = grupo;
        this.pai = pai;
        this.ativo = ativo;
    }

    public void marcarLivroCaixa(boolean entra) {
        this.livroCaixa = entra;
    }

    /** O nome com o grupo acima, para a lista não virar sopa de letras. */
    public String getCaminho() {
        return pai == null ? nome : pai.getNome() + " · " + nome;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public Natureza getPai() {
        return pai;
    }

    public GrupoDeNatureza getGrupo() {
        return grupo;
    }

    public boolean isLivroCaixa() {
        return livroCaixa;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
