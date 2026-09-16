package br.com.itia.financeiro.tela;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O que a janela precisa para funcionar.
 *
 * Só uma coisa: um jeito de montar a tela inteira dentro de uma leitura no
 * banco. Antes isso acontecia sozinho, porque cada página de navegador era uma
 * visita. Agora a janela pede explicitamente, e assim uma tela pode percorrer
 * o cliente de um título sem esbarrar em conexão fechada.
 */
@Configuration
public class ConfiguracaoDaJanela {

    @Bean
    public TransactionTemplate transacaoDaTela(PlatformTransactionManager gerente) {
        return new TransactionTemplate(gerente);
    }
}
