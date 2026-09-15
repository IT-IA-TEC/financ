package br.com.itia.financeiro.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Uma linha da ficha do caso, do jeito que esta empresa quer ver.
 *
 * O rótulo é editável porque cada casa chama as coisas de um jeito: o que aqui
 * é "em aberto" na outra é "carteira", e forçar o nosso nome só atrapalha
 * quem usa.
 */
@Entity
@Table(name = "linha_da_ficha")
public class LinhaDaFicha {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private int ordem = 1;

    @Column(nullable = false)
    private String rotulo;

    @Column(nullable = false)
    private String fonte;

    @Column(nullable = false)
    private boolean ativa = true;

    protected LinhaDaFicha() {
        // exigido pelo JPA
    }

    public LinhaDaFicha(UUID empresaId, int ordem, FonteDaFicha fonte, String rotulo) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.ordem = ordem;
        this.fonte = fonte.name();
        this.rotulo = rotulo == null || rotulo.isBlank() ? fonte.getRotuloPadrao() : rotulo.trim();
    }

    public void ajustar(int ordem, String rotulo, boolean ativa) {
        this.ordem = ordem;
        if (rotulo != null && !rotulo.isBlank()) {
            this.rotulo = rotulo.trim();
        }
        this.ativa = ativa;
    }

    public FonteDaFicha daFonte() {
        return FonteDaFicha.valueOf(fonte);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getFonte() {
        return fonte;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
