package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.Fluxo;
import br.com.itia.financeiro.dominio.PassoDoFluxo;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Fluxos;
import br.com.itia.financeiro.servico.Mensagens;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O estúdio de fluxos: montar automação sem programar.
 */
@Controller
@RequestMapping("/fluxos")
public class FluxoController {

    private final Fluxos fluxos;
    private final Mensagens mensagens;
    private final ContextoEmpresa contexto;

    public FluxoController(Fluxos fluxos, Mensagens mensagens, ContextoEmpresa contexto) {
        this.fluxos = fluxos;
        this.mensagens = mensagens;
        this.contexto = contexto;
    }

    @GetMapping
    public String lista(Model model) {
        List<Fluxo> todos = fluxos.todos();
        Map<UUID, List<PassoDoFluxo>> receitas = new LinkedHashMap<>();
        for (Fluxo fluxo : todos) {
            receitas.put(fluxo.getId(), fluxos.passosDe(fluxo.getId()));
        }

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("fluxos", todos);
        model.addAttribute("receitas", receitas);
        model.addAttribute("execucoes", fluxos.ultimasExecucoes());
        model.addAttribute("modelos", mensagens.modelosAtivos());
        model.addAttribute("gatilhos", Fluxo.GATILHOS);
        model.addAttribute("campos", PassoDoFluxo.CAMPOS);
        model.addAttribute("operadores", PassoDoFluxo.OPERADORES);
        model.addAttribute("acoes", PassoDoFluxo.ACOES);
        model.addAttribute("situacoes", CasoDeCobranca.SITUACOES);
        model.addAttribute("hoje", LocalDate.now());
        return "fluxos";
    }

    @PostMapping
    public String salvar(@RequestParam(required = false) UUID id,
                         @RequestParam String nome,
                         @RequestParam(required = false) String descricao,
                         @RequestParam String gatilho,
                         @RequestParam(required = false, defaultValue = "0") int dias,
                         @RequestParam(required = false, defaultValue = "true") boolean ativo,
                         RedirectAttributes redirect) {
        fluxos.salvar(id, nome, descricao, gatilho, dias, ativo);
        redirect.addFlashAttribute("aviso",
                "Fluxo guardado. Acrescente as condições e as ações.");
        return "redirect:/fluxos";
    }

    @PostMapping("/{id}/condicoes")
    public String condicao(@PathVariable UUID id, @RequestParam String campo,
                           @RequestParam String operador, @RequestParam String valor,
                           RedirectAttributes redirect) {
        fluxos.acrescentarCondicao(id, campo, operador, valor);
        redirect.addFlashAttribute("aviso", "Condição acrescentada.");
        return "redirect:/fluxos";
    }

    @PostMapping("/{id}/acoes")
    public String acao(@PathVariable UUID id, @RequestParam String acao,
                       @RequestParam(required = false) UUID modeloId,
                       @RequestParam(required = false) String texto,
                       @RequestParam(required = false, defaultValue = "0") int dias,
                       RedirectAttributes redirect) {
        fluxos.acrescentarAcao(id, acao, modeloId, texto, dias);
        redirect.addFlashAttribute("aviso", "Ação acrescentada.");
        return "redirect:/fluxos";
    }

    @PostMapping("/{id}/passos/{passoId}/apagar")
    public String apagarPasso(@PathVariable UUID id, @PathVariable UUID passoId,
                              RedirectAttributes redirect) {
        fluxos.apagarPasso(id, passoId);
        redirect.addFlashAttribute("aviso", "Passo tirado do fluxo.");
        return "redirect:/fluxos";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable UUID id, RedirectAttributes redirect) {
        fluxos.desativar(id);
        redirect.addFlashAttribute("aviso", "Fluxo desligado. O histórico continua guardado.");
        return "redirect:/fluxos";
    }

    @PostMapping("/rodar")
    public String rodar(RedirectAttributes redirect) {
        int quantas = fluxos.rodarHoje();
        redirect.addFlashAttribute("aviso", quantas == 0
                ? "Nenhum fluxo tinha o que fazer hoje."
                : "Os fluxos fizeram " + quantas + " ação(ões) agora.");
        return "redirect:/fluxos";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/fluxos";
    }
}
