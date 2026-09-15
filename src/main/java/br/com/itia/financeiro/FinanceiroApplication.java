package br.com.itia.financeiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Modulo Financeiro da YOU Contabilidade.
 *
 * Banco proprio. Este sistema e dono do dinheiro (titulo, pagamento, baixa).
 * O ERP continua dono do cadastro (cliente, usuario, WhatsApp).
 */
@SpringBootApplication
@EnableScheduling
public class FinanceiroApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceiroApplication.class, args);
    }
}
