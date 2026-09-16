package br.com.itia.financeiro.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Anota qual foi a ultima tela aberta.
 *
 * Serve para quem sai e volta cair exatamente onde estava, em vez de voltar
 * para o comeco. Guarda so o endereco da tela, nada de conteudo.
 */
@Configuration
public class GuardaOndeParou implements WebMvcConfigurer {

    public static final String ONDE_PAROU = "ondeParou";

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest pedido, HttpServletResponse resposta,
                                   Object manipulador, org.springframework.web.servlet.ModelAndView mv) {
                if (!"GET".equals(pedido.getMethod())) {
                    return;
                }
                String caminho = pedido.getRequestURI();
                if (naoVale(caminho)) {
                    return;
                }
                String consulta = pedido.getQueryString();
                pedido.getSession(true).setAttribute(ONDE_PAROU,
                        consulta == null ? caminho : caminho + "?" + consulta);
            }
        }).addPathPatterns("/**");
    }

    /** Telas de passagem e pedacos de janela nao contam como "onde parou". */
    private boolean naoVale(String caminho) {
        return caminho.startsWith("/entrar")
                || caminho.startsWith("/sair")
                || caminho.startsWith("/css")
                || caminho.startsWith("/js")
                || caminho.startsWith("/ws")
                || caminho.startsWith("/error")
                || caminho.contains("/perfil")
                || caminho.contains("/unidades/")
                || caminho.endsWith("/entrar");
    }
}
