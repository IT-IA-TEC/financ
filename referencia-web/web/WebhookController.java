package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.servico.IntegracaoServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * A porta por onde os outros sistemas avisam o que aconteceu.
 *
 * Cada integracao tem o seu endereco, com um pedaco secreto no caminho. Sem o
 * segredo certo, ou com a integracao desligada, a porta responde "nao existe":
 * de proposito, para nao contar la fora quais integracoes existem aqui.
 *
 * O aviso e guardado bruto e a resposta sai na hora. Interpretar vem depois:
 * assim a outra ponta nunca fica esperando o nosso processamento.
 */
@RestController
public class WebhookController {

    private final IntegracaoServico integracoes;

    public WebhookController(IntegracaoServico integracoes) {
        this.integracoes = integracoes;
    }

    @PostMapping("/webhooks/{id}/{segredo}")
    public ResponseEntity<String> receber(@PathVariable UUID id,
                                          @PathVariable String segredo,
                                          @RequestHeader(value = "X-Evento", required = false) String tipo,
                                          @RequestBody(required = false) String corpo) {
        Optional<Integracao> achada = integracoes.porWebhook(id, segredo);
        if (achada.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        integracoes.receber(achada.get(), tipo == null ? "AVISO" : tipo, corpo);
        return ResponseEntity.ok("recebido");
    }
}
