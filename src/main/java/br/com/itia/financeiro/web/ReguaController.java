package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Mensagens;
import br.com.itia.financeiro.servico.ReguaDeCadencia;
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
import java.util.UUID;

/**
 * A régua de cadência: os passos e a rodada do dia.
 */
@Controller
@RequestMapping("/regua")
public class ReguaController {

    private final ReguaDeCadencia regua;
    private final Mensagens mensagens;
    private final ContextoEmpresa contexto;

    public ReguaController(ReguaDeCadencia regua, Mensagens mensagens,
                           ContextoEmpresa contexto) {
        this.regua = regua;
        this.mensagens = mensagens;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        mensagens.prepararEmpresa();
        regua.prepararEmpresa();
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("passos", regua.passos());
        model.addAttribute("modelos", mensagens.modelosAtivos());
        model.addAttribute("hoje", LocalDate.now());
        return "regua";
    }

    @PostMapping
    public String salvar(@RequestParam(required = false) UUID id,
                         @RequestParam String nome,
                         @RequestParam String gatilho,
                         @RequestParam(required = false, defaultValue = "0") int dias,
                         @RequestParam UUID modeloId,
                         @RequestParam(required = false) String canal,
                         @RequestParam(required = false, defaultValue = "1") int ordem,
                         @RequestParam(required = false, defaultValue = "false")
                         boolean exigeConfirmacao,
                         @RequestParam(required = false, defaultValue = "true") boolean ativo,
                         RedirectAttributes redirect) {
        regua.salvar(id, nome, gatilho, dias, modeloId, canal, ordem, exigeConfirmacao, ativo);
        redirect.addFlashAttribute("aviso", "Passo guardado na régua.");
        return "redirect:/regua";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable UUID id, RedirectAttributes redirect) {
        regua.desativar(id);
        redirect.addFlashAttribute("aviso", "Passo desativado. Ele não fala mais com ninguém.");
        return "redirect:/regua";
    }

    @PostMapping("/rodar")
    public String rodar(RedirectAttributes redirect) {
        LoteDeMensagem lote = regua.rodarHoje();
        if (lote == null) {
            redirect.addFlashAttribute("erro",
                    "Nenhum passo ativo na régua. Cadastre pelo menos um.");
            return "redirect:/regua";
        }
        redirect.addFlashAttribute("aviso", "Régua rodada: " + lote.getQuantidade()
                + " mensagem(ns) e " + lote.getFora() + " de fora.");
        return "redirect:/disparos/" + lote.getId();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/regua";
    }
}
