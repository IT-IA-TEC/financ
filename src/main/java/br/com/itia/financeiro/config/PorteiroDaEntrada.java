package br.com.itia.financeiro.config;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Quem nao entrou vai para a tela de entrada. Hoje a tela de entrada nao pede
 * senha, mas o caminho por onde a senha vai passar ja existe.
 */
@Configuration
public class PorteiroDaEntrada implements WebMvcConfigurer {

    private final ContextoEmpresa contexto;

    public PorteiroDaEntrada(ContextoEmpresa contexto) {
        this.contexto = contexto;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest pedido, HttpServletResponse resposta,
                                     Object manipulador) throws Exception {
                if (contexto.estaLogado()) {
                    return true;
                }
                resposta.sendRedirect("/entrar");
                return false;
            }
        }).addPathPatterns("/**")
          .excludePathPatterns("/entrar", "/sair", "/css/**", "/js/**", "/imagens/**",
                  "/error", "/webhooks/**");
    }
}
