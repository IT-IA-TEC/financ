package br.com.itia.financeiro;

import br.com.itia.financeiro.tela.Janela;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * O IT.FC: o financeiro do grupo, como programa instalado no computador.
 *
 * Por dentro continua o mesmo sistema de sempre (as regras, os cálculos, o
 * banco). O que mudou é a frente: era página de navegador, agora é janela.
 *
 * Aqui só acontecem duas coisas: liga o motor do sistema antes de a janela
 * aparecer, e desliga quando a pessoa fecha.
 */
@SpringBootApplication
@EnableScheduling
public class FinanceiroApplication extends Application {

    private ConfigurableApplicationContext motor;

    @Override
    public void init() {
        motor = new SpringApplicationBuilderSemNavegador().montar(getParameters().getRaw());
    }

    @Override
    public void start(Stage palco) {
        motor.getBean(Janela.class).abrir(palco);
    }

    @Override
    public void stop() {
        if (motor != null) {
            motor.close();
        }
        javafx.application.Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Sobe o sistema sem servidor nenhum: não existe mais porta nem navegador. */
    private static class SpringApplicationBuilderSemNavegador {
        ConfigurableApplicationContext montar(java.util.List<String> argumentos) {
            SpringApplication mola = new SpringApplication(FinanceiroApplication.class);
            mola.setWebApplicationType(WebApplicationType.NONE);
            mola.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
            // liga no banco de verdade sozinho, quando o arquivo de ligação existe
            mola.setDefaultProperties(LigacaoComOBanco.descobrir());
            return mola.run(argumentos.toArray(new String[0]));
        }
    }
}
