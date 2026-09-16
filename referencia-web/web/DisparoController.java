package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.LoteDeMensagem;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Mensagens;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * O disparo de cobrança em lote e os modelos de mensagem.
 */
@Controller
public class DisparoController {

    private final Mensagens mensagens;
    private final ContextoEmpresa contexto;

    public DisparoController(Mensagens mensagens, ContextoEmpresa contexto) {
        this.mensagens = mensagens;
        this.contexto = contexto;
    }

    // ---------------------------------------------------------------- disparos

    @GetMapping("/disparos")
    public String disparos(Model model) {
        mensagens.prepararEmpresa();
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("lotes", mensagens.lotes());
        model.addAttribute("modelos", mensagens.modelosAtivos());
        model.addAttribute("naFila", mensagens.quantasNaFila());
        model.addAttribute("hoje", LocalDate.now());
        return "disparos";
    }

    @PostMapping("/disparos")
    public String montar(@RequestParam(required = false) String nome,
                         @RequestParam UUID modeloId,
                         @RequestParam(required = false) String canal,
                         @RequestParam(required = false, defaultValue = "vencidos") String visao,
                         @RequestParam(required = false) Integer de,
                         @RequestParam(required = false) Integer ate,
                         RedirectAttributes redirect) {
        LoteDeMensagem lote = mensagens.montarPrevia(nome, modeloId, canal, visao, de, ate);
        redirect.addFlashAttribute("aviso", "Prévia pronta: " + lote.getQuantidade()
                + " vão receber e " + lote.getFora() + " ficaram de fora. "
                + "Confira antes de confirmar.");
        return "redirect:/disparos/" + lote.getId();
    }

    @GetMapping("/disparos/{id}")
    public String lote(@PathVariable UUID id, Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("lote", mensagens.lote(id));
        model.addAttribute("mensagens", mensagens.mensagensDo(id));
        model.addAttribute("foras", mensagens.forasDo(id));
        model.addAttribute("nomes", mensagens.nomesDosTitulos());
        model.addAttribute("hoje", LocalDate.now());
        return "disparo";
    }

    @PostMapping("/disparos/{id}/confirmar")
    public String confirmar(@PathVariable UUID id,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
                            @RequestParam(required = false)
                            @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
                            RedirectAttributes redirect) {
        mensagens.confirmar(id, dia, hora);
        redirect.addFlashAttribute("aviso", dia == null
                ? "Disparo confirmado. As mensagens estão na fila, prontas para sair."
                : "Disparo confirmado e agendado. As mensagens saem na data escolhida.");
        return "redirect:/disparos/" + id;
    }

    @PostMapping("/disparos/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, RedirectAttributes redirect) {
        mensagens.cancelarLote(id);
        redirect.addFlashAttribute("aviso", "Disparo cancelado. Nada saiu.");
        return "redirect:/disparos/" + id;
    }

    @PostMapping("/disparos/{id}/mensagens/{mensagemId}/tirar")
    public String tirar(@PathVariable UUID id, @PathVariable UUID mensagemId,
                        RedirectAttributes redirect) {
        mensagens.tirarDoLote(mensagemId);
        redirect.addFlashAttribute("aviso", "Esta pessoa saiu do disparo.");
        return "redirect:/disparos/" + id;
    }

    @PostMapping("/disparos/{id}/mensagens/{mensagemId}/enviada")
    public String enviada(@PathVariable UUID id, @PathVariable UUID mensagemId,
                          RedirectAttributes redirect) {
        mensagens.marcarEnviada(mensagemId, null);
        redirect.addFlashAttribute("aviso",
                "Marcada como enviada e registrada na história do cliente.");
        return "redirect:/disparos/" + id;
    }

    @PostMapping("/disparos/{id}/mensagens/{mensagemId}/falhou")
    public String falhou(@PathVariable UUID id, @PathVariable UUID mensagemId,
                         @RequestParam(required = false) String motivo,
                         RedirectAttributes redirect) {
        mensagens.marcarFalha(mensagemId, motivo);
        redirect.addFlashAttribute("aviso", "Marcada como não entregue, com o motivo guardado.");
        return "redirect:/disparos/" + id;
    }

    // ----------------------------------------------------------------- modelos

    @GetMapping("/modelos")
    public String modelos(Model model) {
        mensagens.prepararEmpresa();
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("modelos", mensagens.modelos());
        model.addAttribute("espacos", ModeloDeMensagem.ESPACOS);
        model.addAttribute("tons", ModeloDeMensagem.TONS);
        model.addAttribute("momentos", ModeloDeMensagem.MOMENTOS);
        return "modelos";
    }

    @PostMapping("/modelos")
    public String salvarModelo(@RequestParam(required = false) UUID id,
                               @RequestParam String nome,
                               @RequestParam(required = false) String canal,
                               @RequestParam(required = false) String assunto,
                               @RequestParam String corpo,
                               @RequestParam(required = false) String tom,
                               @RequestParam(required = false) String momento,
                               @RequestParam(required = false, defaultValue = "true") boolean ativo,
                               RedirectAttributes redirect) {
        mensagens.salvarModelo(id, nome, canal, assunto, corpo, tom, momento, ativo);
        redirect.addFlashAttribute("aviso", "Modelo guardado.");
        return "redirect:/modelos";
    }

    @PostMapping("/modelos/{id}/desativar")
    public String desativarModelo(@PathVariable UUID id, RedirectAttributes redirect) {
        mensagens.desativarModelo(id);
        redirect.addFlashAttribute("aviso", "Modelo desativado. Ele não aparece mais nos disparos.");
        return "redirect:/modelos";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/disparos";
    }
}
