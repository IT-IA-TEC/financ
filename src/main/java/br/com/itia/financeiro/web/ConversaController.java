package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Conversas;
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
 * A conversa de cobrança com o cliente, dentro da plataforma.
 */
@Controller
@RequestMapping("/conversas")
public class ConversaController {

    private final Conversas conversas;
    private final ContextoEmpresa contexto;

    public ConversaController(Conversas conversas, ContextoEmpresa contexto) {
        this.conversas = conversas;
        this.contexto = contexto;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("linhas", conversas.lista());
        model.addAttribute("semDono", conversas.semDono());
        model.addAttribute("canalLigado", conversas.canalDeWhatsApp().isPresent());
        model.addAttribute("hoje", LocalDate.now());
        return "conversas";
    }

    @GetMapping("/{unidadeId}")
    public String conversa(@PathVariable UUID unidadeId, Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        var emAberto = conversas.emAbertoDe(unidadeId);
        model.addAttribute("cliente", conversas.cliente(unidadeId));
        model.addAttribute("mensagens", conversas.conversa(unidadeId));
        model.addAttribute("titulos", emAberto);
        model.addAttribute("emAberto", emAberto.stream()
                .map(br.com.itia.financeiro.dominio.Titulo::getSaldo)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("canalLigado", conversas.canalDeWhatsApp().isPresent());
        model.addAttribute("hoje", LocalDate.now());
        return "conversa";
    }

    @PostMapping("/{unidadeId}/responder")
    public String responder(@PathVariable UUID unidadeId,
                            @RequestParam String texto,
                            @RequestParam(required = false) String destino,
                            RedirectAttributes redirect) {
        var mensagem = conversas.responder(unidadeId, texto, destino);
        redirect.addFlashAttribute("aviso", mensagem.naFila()
                ? "Mensagem guardada na fila. O canal do WhatsApp ainda não está ligado."
                : ("FALHOU".equals(mensagem.getSituacao())
                ? "O canal não aceitou a mensagem: " + mensagem.getMotivo()
                : "Mensagem enviada."));
        return "redirect:/conversas/" + unidadeId;
    }

    @PostMapping("/{unidadeId}/anotar")
    public String anotar(@PathVariable UUID unidadeId, @RequestParam String texto,
                         RedirectAttributes redirect) {
        conversas.anotarResposta(unidadeId, texto);
        redirect.addFlashAttribute("aviso", "Resposta do cliente registrada na conversa.");
        return "redirect:/conversas/" + unidadeId;
    }

    @PostMapping("/{mensagemId}/dono")
    public String darDono(@PathVariable UUID mensagemId, @RequestParam UUID unidadeId,
                          RedirectAttributes redirect) {
        conversas.darDono(mensagemId, unidadeId);
        redirect.addFlashAttribute("aviso", "Mensagem ligada ao cliente.");
        return "redirect:/conversas/" + unidadeId;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/conversas";
    }
}
