package br.com.itia.financeiro.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * O recado de "mudou alguma coisa".
 *
 * Quando o sistema era página de navegador, este recado viajava para todas as
 * telas abertas daquela empresa, porque várias pessoas podiam estar mexendo ao
 * mesmo tempo. No programa instalado existe uma janela só, e ela já monta a
 * tela de novo a cada vez que é aberta, então o recado só fica registrado.
 *
 * Os serviços continuam avisando do mesmo jeito: nenhum deles precisou mudar.
 */
@Component
public class Avisos {

    private static final Logger LOG = LoggerFactory.getLogger(Avisos.class);

    private volatile String ultimo;

    public void avisar(UUID empresaId, String oQueMudou) {
        this.ultimo = oQueMudou;
        LOG.debug("mudou {} na empresa {}", oQueMudou, empresaId);
    }

    /** A última coisa que mudou. Serve para a tela saber se vale recarregar. */
    public String ultimo() {
        return ultimo;
    }
}
