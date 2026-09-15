package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Como o agente de primeiro atendimento se comporta nesta empresa.
 *
 * O horário não é detalhe: cliente que recebe resposta automática de
 * madrugada percebe na hora que está falando com máquina, e a conversa começa
 * pior do que se ninguém tivesse respondido.
 */
@Entity
@Table(name = "agente")
public class Agente {

    @Id
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false)
    private boolean ativo = false;

    private String saudacao;

    private String assinatura;

    @Column(name = "comeca_as", nullable = false)
    private int comecaAs = 8;

    @Column(name = "termina_as", nullable = false)
    private int terminaAs = 18;

    @Column(name = "responde_sabado", nullable = false)
    private boolean respondeSabado = false;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_por")
    private String atualizadoPor;

    protected Agente() {
        // exigido pelo JPA
    }

    public Agente(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public void ajustar(boolean ativo, String saudacao, String assinatura, int comecaAs,
                        int terminaAs, boolean respondeSabado, String quem) {
        if (comecaAs < 0 || comecaAs > 23 || terminaAs < 1 || terminaAs > 24
                || terminaAs <= comecaAs) {
            throw new IllegalArgumentException(
                    "O horário do agente precisa começar antes de terminar, dentro do dia.");
        }
        this.ativo = ativo;
        this.saudacao = saudacao;
        this.assinatura = assinatura;
        this.comecaAs = comecaAs;
        this.terminaAs = terminaAs;
        this.respondeSabado = respondeSabado;
        this.atualizadoEm = OffsetDateTime.now();
        this.atualizadoPor = quem;
    }

    /** Se o agente pode falar agora. */
    public boolean podeFalar(LocalDateTime agora) {
        if (!ativo) {
            return false;
        }
        DayOfWeek dia = agora.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY) {
            return false;
        }
        if (dia == DayOfWeek.SATURDAY && !respondeSabado) {
            return false;
        }
        int hora = agora.getHour();
        return hora >= comecaAs && hora < terminaAs;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getSaudacao() {
        return saudacao;
    }

    public String getAssinatura() {
        return assinatura;
    }

    public int getComecaAs() {
        return comecaAs;
    }

    public int getTerminaAs() {
        return terminaAs;
    }

    public boolean isRespondeSabado() {
        return respondeSabado;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }
}
