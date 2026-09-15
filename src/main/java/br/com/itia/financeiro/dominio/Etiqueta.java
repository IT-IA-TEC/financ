package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma etiqueta, usada entre outras coisas para o papel de cada contato.
 *
 * Papel não é campo fixo de propósito: cada empresa chama as coisas do seu
 * jeito, e o conjunto muda com o tempo. Cada etiqueta tem um código estável,
 * que é o que permite a mesma etiqueta existir em outro banco do grupo sem
 * depender do texto escrito na tela.
 */
@Entity
@Table(name = "etiqueta")
public class Etiqueta {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    /** Onde ela pode ser usada: CONTATO, PESSOA, UNIDADE, CONTRATO, DOCUMENTO. */
    @Column(nullable = false)
    private String escopo = "CONTATO";

    private String cor;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Etiqueta() {
        // exigido pelo JPA
    }

    public Etiqueta(Empresa empresa, String nome, String descricao, String escopo, String cor) {
        this.id = UUID.randomUUID();
        this.empresa = empresa;
        this.codigo = codigoDe(nome);
        this.nome = nome;
        this.descricao = descricao;
        this.escopo = escopo == null ? "CONTATO" : escopo;
        this.cor = cor;
    }

    /** O código nasce do nome, sem acento e sem espaço, e nunca muda depois. */
    public static String codigoDe(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        return semAcento.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public void ajustar(String nome, String descricao, String escopo, String cor, boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.escopo = escopo;
        this.cor = cor;
        this.ativo = ativo;
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

    public String getDescricao() {
        return descricao;
    }

    public String getEscopo() {
        return escopo;
    }

    public String getCor() {
        return cor;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
