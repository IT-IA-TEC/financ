package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.AgenteDeAtendimento;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Regras;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * O agente de primeiro atendimento: configuração e o que ele andou respondendo.
 */
@Controller
@RequestMapping("/agente")
public class AgenteController {

    private final AgenteDeAtendimento agente;
    private final Regras regras;
    private final ContextoEmpresa contexto;

    public AgenteController(AgenteDeAtendimento agente, Regras regras,
                            ContextoEmpresa contexto) {
        this.agente = agente;
        this.regras = regras;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("agente", agente.daEmpresa());
        model.addAttribute("regra", regras.daEmpresa());
        model.addAttribute("atendimentos", agente.ultimos());
        model.addAttribute("escalados", agente.quantosEscalados());
        model.addAttribute("atendidos", agente.quantosAtendidos());
        model.addAttribute("hoje", LocalDate.now());
        return "agente";
    }

    @PostMapping
    public String salvar(@RequestParam(required = false, defaultValue = "false") boolean ativo,
                         @RequestParam(required = false) String saudacao,
                         @RequestParam(required = false) String assinatura,
                         @RequestParam(defaultValue = "8") int comecaAs,
                         @RequestParam(defaultValue = "18") int terminaAs,
                         @RequestParam(required = false, defaultValue = "false")
                         boolean respondeSabado,
                         RedirectAttributes redirect) {
        agente.salvar(ativo, saudacao, assinatura, comecaAs, terminaAs, respondeSabado);
        redirect.addFlashAttribute("aviso", ativo
                ? "Agente ligado. Ele responde dentro do horário combinado."
                : "Agente desligado. As mensagens ficam esperando gente.");
        return "redirect:/agente";
    }

    /** Prova sem mandar nada: mostra o que o agente entenderia de uma frase. */
    @PostMapping("/testar")
    public String testar(@RequestParam String texto, RedirectAttributes redirect) {
        String intencao = agente.entender(texto);
        redirect.addFlashAttribute("aviso", "Nessa frase o agente entenderia: "
                + intencao.toLowerCase().replace('_', ' ') + ".");
        return "redirect:/agente";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/agente";
    }
}
