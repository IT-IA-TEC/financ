package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.AgenteIa;
import br.com.itia.financeiro.dominio.FonteDaFicha;
import br.com.itia.financeiro.dominio.PermissaoDaIa;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FichaDoCaso;
import br.com.itia.financeiro.servico.Inteligencia;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A tela do modelo de inteligência: o que ele pode ver, o que pode fazer, e
 * como a ficha do caso é montada nesta empresa.
 */
@Controller
@RequestMapping("/inteligencia")
public class InteligenciaController {

    private final Inteligencia inteligencia;
    private final FichaDoCaso ficha;
    private final ContextoEmpresa contexto;

    public InteligenciaController(Inteligencia inteligencia, FichaDoCaso ficha,
                                  ContextoEmpresa contexto) {
        this.inteligencia = inteligencia;
        this.ficha = ficha;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("agente", inteligencia.daEmpresa());
        model.addAttribute("chaves", inteligencia.chaves());
        model.addAttribute("leituras", PermissaoDaIa.leituras());
        model.addAttribute("acoes", PermissaoDaIa.acoes());
        model.addAttribute("nunca", inteligencia.oQueNuncaFaz());
        model.addAttribute("modos", AgenteIa.MODOS);
        model.addAttribute("integracoes", inteligencia.integracoesDisponiveis());
        model.addAttribute("usos", inteligencia.ultimosUsos());
        model.addAttribute("quantosUsos", inteligencia.quantosUsos());
        model.addAttribute("quantasAcoes", inteligencia.quantasAcoes());
        model.addAttribute("pronto", inteligencia.prontoParaTrabalhar());
        model.addAttribute("linhas", ficha.configuracao());
        java.util.UUID alguem = inteligencia.alguemParaTestar();
        model.addAttribute("travas", alguem == null ? java.util.List.of()
                : inteligencia.testarTravas(alguem, new BigDecimal("1000")));
        model.addAttribute("fontes", FonteDaFicha.todas());
        return "inteligencia";
    }

    @PostMapping
    public String salvar(@RequestParam(required = false) UUID integracaoId,
                         @RequestParam(required = false) String modelo,
                         @RequestParam(required = false, defaultValue = "false") boolean ativo,
                         @RequestParam(required = false, defaultValue = "SO_SUGERE") String modo,
                         @RequestParam(required = false) String instrucao,
                         @RequestParam(required = false) BigDecimal tetoValor,
                         @RequestParam(required = false, defaultValue = "1") int tetoMensagensDia,
                         RedirectAttributes redirect) {
        inteligencia.salvar(integracaoId, modelo, ativo, modo, instrucao, tetoValor,
                tetoMensagensDia);
        redirect.addFlashAttribute("aviso", ativo
                ? "Modelo ligado, com as chaves que estão marcadas abaixo."
                : "Modelo desligado. Ele não lê nem faz nada enquanto estiver assim.");
        return "redirect:/inteligencia";
    }

    @PostMapping("/chaves")
    public String chaves(@RequestParam(name = "ligadas", required = false) List<String> ligadas,
                         RedirectAttributes redirect) {
        inteligencia.guardarChaves(ligadas);
        redirect.addFlashAttribute("aviso",
                "Chaves guardadas. O que ficou desmarcado o modelo não vê e não faz.");
        return "redirect:/inteligencia";
    }

    // ------------------------------------------------------------------ ficha

    @PostMapping("/ficha")
    public String acrescentar(@RequestParam FonteDaFicha fonte,
                              @RequestParam(required = false) String rotulo,
                              RedirectAttributes redirect) {
        ficha.acrescentar(fonte, rotulo);
        redirect.addFlashAttribute("aviso", "Linha acrescentada à ficha do caso.");
        return "redirect:/inteligencia";
    }

    @PostMapping("/ficha/{id}")
    public String ajustar(@PathVariable UUID id,
                          @RequestParam(defaultValue = "1") int ordem,
                          @RequestParam(required = false) String rotulo,
                          @RequestParam(required = false, defaultValue = "false") boolean ativa,
                          RedirectAttributes redirect) {
        ficha.ajustar(id, ordem, rotulo, ativa);
        redirect.addFlashAttribute("aviso", "Linha ajustada.");
        return "redirect:/inteligencia";
    }

    @PostMapping("/ficha/{id}/tirar")
    public String tirar(@PathVariable UUID id, RedirectAttributes redirect) {
        ficha.tirar(id);
        redirect.addFlashAttribute("aviso", "Linha tirada da ficha.");
        return "redirect:/inteligencia";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/inteligencia";
    }
}
