package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tudo que entra e sai por uma integracao, guardado como veio.
 *
 * Guardar bruto e o que permite reprocessar depois sem pedir de novo para a
 * outra ponta, e e a prova do que foi recebido em caso de divergencia.
 */
@Entity
@Table(name = "integracao_evento")
public class EventoIntegracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "integracao_id", nullable = false)
    private UUID integracaoId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String direcao;

    @Column(nullable = false)
    private String tipo;

    private String referencia;

    private String carga;

    @Column(nullable = false)
    private String status = "RECEBIDO";

    private String erro;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    @Column(name = "processado_em")
    private OffsetDateTime processadoEm;

    protected EventoIntegracao() {
        // exigido pelo JPA
    }

    public EventoIntegracao(UUID integracaoId, UUID empresaId, String direcao,
                            String tipo, String referencia, String carga) {
        this.integracaoId = integracaoId;
        this.empresaId = empresaId;
        this.direcao = direcao;
        this.tipo = tipo;
        this.referencia = referencia;
        this.carga = carga;
    }

    public void marcarProcessado() {
        this.status = "PROCESSADO";
        this.processadoEm = OffsetDateTime.now();
    }

    public void marcarErro(String erro) {
        this.status = "ERRO";
        this.erro = erro;
        this.processadoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getIntegracaoId() {
        return integracaoId;
    }

    public String getDirecao() {
        return direcao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getCarga() {
        return carga;
    }

    public String getStatus() {
        return status;
    }

    public String getErro() {
        return erro;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    /** Um pedaco da carga, para caber na tela sem estourar a linha. */
    public String getResumoDaCarga() {
        if (carga == null) {
            return "";
        }
        String limpo = carga.replaceAll("\\s+", " ").trim();
        return limpo.length() <= 160 ? limpo : limpo.substring(0, 160) + "...";
    }
}
