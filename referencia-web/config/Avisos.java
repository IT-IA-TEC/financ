package br.com.itia.financeiro.config;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aviso ao vivo: quando alguem lanca, recebe ou cancela alguma coisa, todas as
 * telas abertas daquela empresa ficam sabendo na hora.
 *
 * O aviso nao carrega dado nenhum, so diz "mudou isso aqui". Quem decide o que
 * fazer com ele e a tela, do lado do navegador. Assim nada sensivel trafega
 * pelo canal e uma tela da YOU nunca ouve o que aconteceu na 40%.
 */
@Component
public class Avisos extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Avisos.class);

    /** Uma sala por empresa. */
    private final Map<UUID, Set<WebSocketSession>> salas = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession sessao) {
        UUID empresaId = (UUID) sessao.getAttributes().get("empresaId");
        if (empresaId == null) {
            fechar(sessao);
            return;
        }
        salas.computeIfAbsent(empresaId, k -> ConcurrentHashMap.newKeySet()).add(sessao);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sessao, CloseStatus status) {
        salas.values().forEach(sala -> sala.remove(sessao));
    }

    /** Avisa todas as telas abertas daquela empresa. */
    public void avisar(UUID empresaId, String oQueMudou) {
        Set<WebSocketSession> sala = salas.get(empresaId);
        if (sala == null || sala.isEmpty()) {
            return;
        }
        TextMessage recado = new TextMessage("{\"mudou\":\"" + oQueMudou + "\"}");
        for (WebSocketSession sessao : sala) {
            try {
                if (sessao.isOpen()) {
                    sessao.sendMessage(recado);
                }
            } catch (IOException erro) {
                LOG.debug("nao consegui avisar uma tela: {}", erro.getMessage());
            }
        }
    }

    private void fechar(WebSocketSession sessao) {
        try {
            sessao.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignorado) {
            // a sessao ja caiu
        }
    }

    /**
     * Liga o canal e, no aperto de mao, anota de qual empresa aquela tela e.
     * A empresa vem da sessao de quem abriu, nunca do endereco: assim ninguem
     * escuta a sala de uma empresa em que nao entrou.
     */
    @Configuration
    @EnableWebSocket
    static class Ligacao implements WebSocketConfigurer {

        private final Avisos avisos;

        Ligacao(Avisos avisos) {
            this.avisos = avisos;
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
            registro.addHandler(avisos, "/ws/avisos")
                    .addInterceptors(new HandshakeInterceptor() {
                        @Override
                        public boolean beforeHandshake(ServerHttpRequest pedido,
                                                       ServerHttpResponse resposta,
                                                       WebSocketHandler manipulador,
                                                       Map<String, Object> atributos) {
                            if (!(pedido instanceof ServletServerHttpRequest servlet)) {
                                return false;
                            }
                            HttpSession sessao = servlet.getServletRequest().getSession(false);
                            if (sessao == null) {
                                return false;
                            }
                            Object bean = sessao.getAttribute("scopedTarget.contextoEmpresa");
                            if (!(bean instanceof ContextoEmpresa contexto)
                                    || !contexto.temEmpresaEscolhida()) {
                                return false;
                            }
                            atributos.put("empresaId", contexto.exigirEmpresaId());
                            return true;
                        }

                        @Override
                        public void afterHandshake(ServerHttpRequest pedido,
                                                   ServerHttpResponse resposta,
                                                   WebSocketHandler manipulador,
                                                   Exception erro) {
                            // nada a fazer
                        }
                    })
                    .setAllowedOriginPatterns("*");
        }
    }
}
