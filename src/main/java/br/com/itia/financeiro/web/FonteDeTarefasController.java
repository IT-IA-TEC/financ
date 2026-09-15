package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.TarefasDeFora;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * De onde as tarefas de outro sistema chegam: a configuração e o mapeamento.
 */
@Controller
@RequestMapping("/fontes-de-tarefas")
public class FonteDeTarefasController {

    private final TarefasDeFora deFora;
    private final ContextoEmpresa contexto;

    public FonteDeTarefasController(TarefasDeFora deFora, ContextoEmpresa contexto) {
        this.deFora = deFora;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        var fontes = deFora.todas();
        Map<UUID, Object> operacoes = new LinkedHashMap<>();
        for (var fonte : fontes) {
            operacoes.put(fonte.getId(), deFora.operacoesDe(fonte.getIntegracaoId()));
        }
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("fontes", fontes);
        model.addAttribute("operacoes", operacoes);
        model.addAttribute("integracoes", deFora.integracoesPossiveis());
        return "fontes-de-tarefas";
    }

    @PostMapping
    public String salvar(@RequestParam(required = false) UUID id,
                         @RequestParam String nome,
                         @RequestParam UUID integracaoId,
                         @RequestParam(required = false) UUID operacaoId,
                         @RequestParam(defaultValue = "id") String campoId,
                         @RequestParam(defaultValue = "titulo") String campoTitulo,
                         @RequestParam(required = false) String campoDescricao,
                         @RequestParam(required = false) String campoSituacao,
                         @RequestParam(required = false) String campoResponsavel,
                         @RequestParam(required = false) String campoPrazo,
                         @RequestParam(required = false) String campoPrioridade,
                         @RequestParam(required = false) String campoLink,
                         @RequestParam(required = false) String filtro,
                         @RequestParam(required = false) String setorPadrao,
                         @RequestParam(required = false, defaultValue = "false") boolean ativa,
                         RedirectAttributes redirect) {
        deFora.salvar(id, nome, integracaoId, operacaoId, campoId, campoTitulo, campoDescricao,
                campoSituacao, campoResponsavel, campoPrazo, campoPrioridade, campoLink,
                filtro, setorPadrao, ativa);
        redirect.addFlashAttribute("aviso", "Fonte guardada. Use \"Buscar agora\" para testar.");
        return "redirect:/fontes-de-tarefas";
    }

    @PostMapping("/{id}/puxar")
    public String puxar(@PathVariable UUID id, RedirectAttributes redirect) {
        TarefasDeFora.Puxada puxada = deFora.puxar(id);
        if (puxada.deuCerto()) {
            redirect.addFlashAttribute("aviso", "Busca feita: " + puxada.resumo() + ".");
        } else {
            redirect.addFlashAttribute("erro", puxada.erro());
        }
        return "redirect:/fontes-de-tarefas";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/fontes-de-tarefas";
    }
}
