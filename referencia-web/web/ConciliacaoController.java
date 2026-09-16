package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ConciliacaoBancaria;
import br.com.itia.financeiro.servico.ContasAPagar;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * A conciliação bancária: o extrato de um lado, o sistema do outro.
 */
@Controller
@RequestMapping("/conciliacao")
public class ConciliacaoController {

    private final ConciliacaoBancaria conciliacao;
    private final ContasAPagar contas;
    private final ContextoEmpresa contexto;

    public ConciliacaoController(ConciliacaoBancaria conciliacao, ContasAPagar contas,
                                 ContextoEmpresa contexto) {
        this.conciliacao = conciliacao;
        this.contas = contas;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(@RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                       Model model) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de == null ? hoje.withDayOfMonth(1) : de;
        LocalDate fim = ate == null ? hoje : ate;

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("movimentos", conciliacao.todos());
        model.addAttribute("pendentes", conciliacao.pendentes());
        model.addAttribute("titulos", conciliacao.titulosEmAberto());
        model.addAttribute("contasEmAberto", conciliacao.contasEmAberto());
        model.addAttribute("contasFinanceiras", contas.contasAtivas());
        model.addAttribute("importacoes", conciliacao.ultimasImportacoes());
        model.addAttribute("fechamento", conciliacao.fechamentoDiario(inicio, fim));
        model.addAttribute("de", inicio);
        model.addAttribute("ate", fim);
        model.addAttribute("hoje", hoje);
        return "conciliacao";
    }

    @PostMapping("/importar")
    public String importar(@RequestParam MultipartFile arquivo,
                           @RequestParam(required = false) UUID contaId,
                           RedirectAttributes redirect) {
        ConciliacaoBancaria.Resultado resultado = conciliacao.importar(arquivo, contaId);
        redirect.addFlashAttribute("aviso", resultado.lidas() + " linha(s) lida(s), "
                + resultado.novos() + " movimento(s) novo(s), "
                + resultado.repetidos() + " já estavam aqui e "
                + resultado.casadosSozinho() + " casaram sozinho pelo identificador."
                + (resultado.recusadas().isEmpty() ? ""
                : " Recusadas: " + String.join("; ", resultado.recusadas())));
        return "redirect:/conciliacao";
    }

    @PostMapping("/casar")
    public String casar(RedirectAttributes redirect) {
        int casados = conciliacao.casarSozinho();
        redirect.addFlashAttribute("aviso", casados == 0
                ? "Nenhum movimento novo casou pelo identificador. O resto precisa de decisão."
                : casados + " movimento(s) casaram pelo identificador.");
        return "redirect:/conciliacao";
    }

    @PostMapping("/{id}/titulo")
    public String ligarAoTitulo(@PathVariable UUID id,
                                @RequestParam UUID tituloId,
                                @RequestParam(required = false, defaultValue = "false") boolean darBaixa,
                                RedirectAttributes redirect) {
        conciliacao.ligarAoTitulo(id, tituloId, darBaixa);
        redirect.addFlashAttribute("aviso", darBaixa
                ? "Movimento ligado ao título, com a baixa registrada."
                : "Movimento ligado ao título, sem mexer no saldo.");
        return "redirect:/conciliacao";
    }

    @PostMapping("/{id}/conta")
    public String ligarAConta(@PathVariable UUID id,
                              @RequestParam UUID obrigacaoId,
                              @RequestParam(required = false, defaultValue = "false") boolean registrarPagamento,
                              RedirectAttributes redirect) {
        conciliacao.ligarAConta(id, obrigacaoId, registrarPagamento);
        redirect.addFlashAttribute("aviso", registrarPagamento
                ? "Movimento ligado à conta, com o pagamento registrado."
                : "Movimento ligado à conta, sem mexer no saldo.");
        return "redirect:/conciliacao";
    }

    @PostMapping("/{id}/ignorar")
    public String ignorar(@PathVariable UUID id, @RequestParam String motivo,
                          RedirectAttributes redirect) {
        conciliacao.ignorar(id, motivo);
        redirect.addFlashAttribute("aviso", "Movimento marcado como ignorado, com o motivo.");
        return "redirect:/conciliacao";
    }

    @PostMapping("/{id}/desfazer")
    public String desfazer(@PathVariable UUID id, RedirectAttributes redirect) {
        conciliacao.desfazer(id);
        redirect.addFlashAttribute("aviso", "Movimento voltou para pendente.");
        return "redirect:/conciliacao";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/conciliacao";
    }
}
