package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.EtiquetaServico;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * O gerenciador de etiquetas.
 *
 * Fica numa tela própria porque não serve só ao papel de contato: serve para
 * marcar pessoa, unidade, contrato e documento, e o mesmo conjunto vai ser
 * espelhado em outros bancos do grupo.
 */
@Controller
@RequestMapping("/etiquetas")
public class EtiquetaController {

    private final EtiquetaServico etiquetas;
    private final ContextoEmpresa contexto;

    public EtiquetaController(EtiquetaServico etiquetas, ContextoEmpresa contexto) {
        this.etiquetas = etiquetas;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("etiquetas", etiquetas.todas());
        model.addAttribute("escopos", EtiquetaServico.ESCOPOS);
        return "etiquetas";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam(required = false) String descricao,
                            @RequestParam(required = false, defaultValue = "CONTATO") String escopo,
                            @RequestParam(required = false) String cor,
                            RedirectAttributes redirect) {
        etiquetas.cadastrar(nome.trim(), descricao, escopo, cor);
        redirect.addFlashAttribute("aviso", "Etiqueta cadastrada.");
        return "redirect:/etiquetas";
    }

    @PostMapping("/{id}")
    public String salvar(@PathVariable UUID id,
                         @RequestParam String nome,
                         @RequestParam(required = false) String descricao,
                         @RequestParam(required = false) String escopo,
                         @RequestParam(required = false) String cor,
                         @RequestParam(required = false, defaultValue = "false") boolean ativo,
                         RedirectAttributes redirect) {
        etiquetas.salvar(id, nome.trim(), descricao, escopo, cor, ativo);
        redirect.addFlashAttribute("aviso", "Etiqueta salva.");
        return "redirect:/etiquetas";
    }

    @PostMapping("/sugestao")
    public String sugestao(RedirectAttributes redirect) {
        int criadas = etiquetas.criarSugestaoInicial();
        redirect.addFlashAttribute("aviso", criadas == 0
                ? "Esta empresa já tem etiquetas. Nada foi criado."
                : criadas + " etiquetas criadas para começar.");
        return "redirect:/etiquetas";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/etiquetas";
    }
}
