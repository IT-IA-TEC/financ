package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Endereço de uma unidade.
 *
 * São vários de propósito: o endereço de cobrança, o de entrega e o fiscal
 * podem ser diferentes, e cobrar no endereço errado é problema conhecido.
 */
@Entity
@Table(name = "endereco")
public class Endereco {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    /** PRINCIPAL, COBRANCA, ENTREGA ou FISCAL. */
    @Column(nullable = false)
    private String tipo = "PRINCIPAL";

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String pais = "Brasil";

    @Column(nullable = false)
    private String fonte = "MANUAL";

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Endereco() {
        // exigido pelo JPA
    }

    public Endereco(UUID empresaId, UUID unidadeId, String tipo) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.unidadeId = unidadeId;
        this.tipo = tipo == null ? "PRINCIPAL" : tipo;
    }

    public void ajustar(String tipo, String cep, String logradouro, String numero,
                        String complemento, String bairro, String cidade, String uf,
                        String pais, Fonte fonte) {
        this.tipo = tipo;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.pais = pais == null || pais.isBlank() ? "Brasil" : pais;
        this.fonte = fonte == null ? "MANUAL" : fonte.name();
    }

    /** O endereço numa linha só, para a tela. */
    public String getLinha() {
        StringBuilder texto = new StringBuilder();
        if (logradouro != null) {
            texto.append(logradouro);
        }
        if (numero != null && !numero.isBlank()) {
            texto.append(", ").append(numero);
        }
        if (complemento != null && !complemento.isBlank()) {
            texto.append(" ").append(complemento);
        }
        if (bairro != null && !bairro.isBlank()) {
            texto.append(" · ").append(bairro);
        }
        if (cidade != null && !cidade.isBlank()) {
            texto.append(" · ").append(cidade);
        }
        if (uf != null && !uf.isBlank()) {
            texto.append("/").append(uf);
        }
        return texto.toString();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getPais() {
        return pais;
    }

    public String getFonte() {
        return fonte;
    }
}
