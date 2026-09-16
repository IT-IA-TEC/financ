package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Comprovante;
import br.com.itia.financeiro.servico.Comprovantes;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A fila de comprovantes, dentro do contas a receber.
 */
@Controller
@RequestMapping("/comprovantes")
public class ComprovanteController {

    private final Comprovantes comprovantes;
    private final ContextoEmpresa contexto;

    public ComprovanteController(Comprovantes comprovantes, ContextoEmpresa contexto) {
        this.comprovantes = comprovantes;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        var fila = comprovantes.fila();
        Map<UUID, Object> sugestoes = new LinkedHashMap<>();
        for (Comprovante c : fila) {
            if (c.naFila()) {
                sugestoes.put(c.getId(), comprovantes.sugestoesPara(c));
            }
        }

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("comprovantes", fila);
        model.addAttribute("esperando", comprovantes.esperando());
        model.addAttribute("clientes", comprovantes.clientes());
        model.addAttribute("titulos", comprovantes.titulosEmAberto());
        model.addAttribute("sugestoes", sugestoes);
        model.addAttribute("hoje", LocalDate.now());
        return "comprovantes";
    }

    @PostMapping
    public String receber(@RequestParam(required = false) UUID pagadorId,
                          @RequestParam(required = false) String texto,
                          @RequestParam(required = false) MultipartFile arquivo,
                          @RequestParam(required = false, defaultValue = "DIGITADO") String origem,
                          RedirectAttributes redirect) {
        Comprovante guardado = comprovantes.receber(pagadorId, texto, arquivo, origem);
        String falta = guardado.oQueFalta();
        redirect.addFlashAttribute("aviso", falta == null
                ? "Comprovante na fila, com valor, data, destino e identificador lidos."
                : "Comprovante na fila, mas " + falta + ". Complete antes de dar baixa.");
        return "redirect:/comprovantes";
    }

    @PostMapping("/{id}/conferir")
    public String conferir(@PathVariable UUID id,
                           @RequestParam UUID tituloId,
                           @RequestParam(required = false) BigDecimal valor,
                           RedirectAttributes redirect) {
        comprovantes.conferir(id, tituloId, valor);
        redirect.addFlashAttribute("aviso",
                "Comprovante conferido e baixa registrada no título.");
        return "redirect:/comprovantes";
    }

    @PostMapping("/{id}/recusar")
    public String recusar(@PathVariable UUID id, @RequestParam String motivo,
                          RedirectAttributes redirect) {
        comprovantes.recusar(id, motivo);
        redirect.addFlashAttribute("aviso",
                "Comprovante recusado, com o motivo guardado para responder ao cliente.");
        return "redirect:/comprovantes";
    }

    @PostMapping("/{id}/voltar")
    public String voltar(@PathVariable UUID id, RedirectAttributes redirect) {
        comprovantes.voltarParaFila(id);
        redirect.addFlashAttribute("aviso", "Comprovante voltou para a fila.");
        return "redirect:/comprovantes";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/comprovantes";
    }
}
