package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import br.com.itia.financeiro.servico.PainelAnalitico;
import br.com.itia.financeiro.servico.Regras;
import br.com.itia.financeiro.servico.PainelServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/titulos")
public class TituloController {

    private final FinanceiroServico financeiro;
    private final PainelServico painel;
    private final PainelAnalitico analitico;
    private final Regras regras;
    private final ContextoEmpresa contexto;

    public TituloController(FinanceiroServico financeiro, PainelServico painel,
                            PainelAnalitico analitico, Regras regras,
                            ContextoEmpresa contexto) {
        this.financeiro = financeiro;
        this.painel = painel;
        this.analitico = analitico;
        this.regras = regras;
        this.contexto = contexto;
    }

    /** Os dados dos dois paineis analiticos, para a tela desenhar. */
    @GetMapping("/graficos")
    @org.springframework.web.bind.annotation.ResponseBody
    public PainelAnalitico.Resposta graficos(
            @RequestParam(required = false, defaultValue = "mes") String periodo) {
        return analitico.montar(periodo);
    }

    @GetMapping
    public String listar(Model model) {
        LocalDate hoje = LocalDate.now();
        var titulos = financeiro.titulosDaEmpresa();

        // O que vence nos proximos sete dias sai da propria lista, para o
        // numero do cartao poder ser conferido linha a linha.
        BigDecimal aVencer = titulos.stream()
                .filter(t -> t.getSaldo().signum() > 0)
                .filter(t -> t.getVencimento() != null)
                .filter(t -> !t.getVencimento().isBefore(hoje)
                        && !t.getVencimento().isAfter(hoje.plusDays(7)))
                .map(Titulo::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("titulos", titulos);
        model.addAttribute("resumo", painel.resumoDoMes());
        model.addAttribute("aVencer7", aVencer);
        model.addAttribute("hoje", hoje);
        return "titulos";
    }

    @GetMapping("/novo")
    public String formulario(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("clientes", financeiro.clientesDaEmpresa());
        model.addAttribute("hoje", LocalDate.now());
        return "titulo-novo";
    }

    @PostMapping
    public String lancar(@RequestParam UUID clienteId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
                         @RequestParam String descricao,
                         @RequestParam BigDecimal valor,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimento,
                         RedirectAttributes redirect) {
        Titulo titulo = financeiro.lancarTitulo(clienteId, competencia, descricao, valor, vencimento);
        redirect.addFlashAttribute("aviso",
                "Titulo " + titulo.getNumero() + " lancado. Codigo do PIX: "
                        + titulo.getIdentificadorPix());
        return "redirect:/titulos/" + titulo.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model) {
        LocalDate hoje = LocalDate.now();
        var titulo = financeiro.titulo(id);
        var regra = regras.daEmpresa();

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("titulo", titulo);
        model.addAttribute("regra", regra);
        // O valor original nunca muda: o acrescimo do atraso e calculado na
        // hora, ate a data de hoje, e so quando a empresa liga essa regra.
        model.addAttribute("acrescimo",
                regra.acrescimoDe(titulo.getSaldo(), titulo.getVencimento(), hoje));
        model.addAttribute("hoje", hoje);
        return "titulo";
    }

    @PostMapping("/{id}/pagamentos")
    public String receber(@PathVariable UUID id,
                          @RequestParam BigDecimal valor,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pagoEm,
                          @RequestParam(defaultValue = "PIX") String forma,
                          @RequestParam(required = false) String transacaoId,
                          RedirectAttributes redirect) {
        financeiro.receberPagamento(id, valor, pagoEm, forma, transacaoId);
        redirect.addFlashAttribute("aviso", "Pagamento lancado.");
        return "redirect:/titulos/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id,
                           @RequestParam String motivo,
                           RedirectAttributes redirect) {
        financeiro.cancelarTitulo(id, motivo);
        redirect.addFlashAttribute("aviso", "Titulo cancelado.");
        return "redirect:/titulos/" + id;
    }
}
