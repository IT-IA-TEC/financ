package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Quem é procurado na cobrança.
 *
 * O papel de cada um vem por etiqueta, não por campo fixo: quem paga, quem
 * assina, quem só recebe recado. A ordem de prioridade diz quem é procurado
 * primeiro, e o aceite por canal é o que sustenta a cobrança pelo WhatsApp.
 */
@Entity
@Table(name = "contato")
public class Contato {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pagador_id")
    private UUID pagadorId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(nullable = false)
    private String nome;

    private String telefone;
    private String whatsapp;
    private String email;

    @Column(nullable = false)
    private int prioridade = 1;

    @Column(name = "aceita_cobranca", nullable = false)
    private boolean aceitaCobranca = true;

    @Column(name = "melhor_horario")
    private String melhorHorario;

    private String observacao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    /** As etiquetas de papel, carregadas junto para a tela. */
    @Transient
    private List<Etiqueta> papeis = new ArrayList<>();

    protected Contato() {
        // exigido pelo JPA
    }

    public Contato(UUID empresaId, UUID pagadorId, UUID unidadeId, String nome) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.pagadorId = pagadorId;
        this.unidadeId = unidadeId;
        this.nome = nome;
    }

    public void ajustar(String nome, String telefone, String whatsapp, String email,
                        int prioridade, boolean aceitaCobranca, String melhorHorario,
                        String observacao, boolean ativo) {
        this.nome = nome;
        this.telefone = telefone;
        this.whatsapp = whatsapp;
        this.email = email;
        this.prioridade = prioridade;
        this.aceitaCobranca = aceitaCobranca;
        this.melhorHorario = melhorHorario;
        this.observacao = observacao;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagadorId() {
        return pagadorId;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getEmail() {
        return email;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public boolean isAceitaCobranca() {
        return aceitaCobranca;
    }

    public String getMelhorHorario() {
        return melhorHorario;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public List<Etiqueta> getPapeis() {
        return papeis;
    }

    public void receberPapeis(List<Etiqueta> papeis) {
        this.papeis = papeis;
    }
}
