package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.servico.Acordos;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Acordos e parcelamento de dívida.
 */
@Controller
@RequestMapping("/acordos")
public class AcordoController {

    private final Acordos acordos;
    private final ContextoEmpresa contexto;

    public AcordoController(Acordos acordos, ContextoEmpresa contexto) {
        this.acordos = acordos;
        this.contexto = contexto;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) UUID cliente, Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("acordos", acordos.todos());
        model.addAttribute("clientes", acordos.clientesComDivida());
        model.addAttribute("escolhido", cliente);
        model.addAttribute("emAberto", cliente == null
                ? List.of() : acordos.podeEntrar(cliente));
        model.addAttribute("hoje", LocalDate.now());
        return "acordos";
    }

    @PostMapping
    public String fechar(@RequestParam UUID unidadeId,
                         @RequestParam(name = "tituloIds") List<UUID> tituloIds,
                         @RequestParam(required = false) BigDecimal acrescimo,
                         @RequestParam(required = false) BigDecimal desconto,
                         @RequestParam(required = false) BigDecimal entrada,
                         @RequestParam(defaultValue = "1") int parcelas,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                         LocalDate primeiroVencimento,
                         @RequestParam(required = false) String motivoDesconto,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        Acordo acordo = acordos.fechar(unidadeId, tituloIds, acrescimo, desconto, entrada,
                parcelas, primeiroVencimento, motivoDesconto, observacao);
        redirect.addFlashAttribute("aviso", "Acordo " + acordo.getNumero()
                + " fechado. Os documentos antigos saíram da cobrança e as parcelas"
                + " já estão no contas a receber.");
        return "redirect:/acordos/" + acordo.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model) {
        acordos.conferirCumprimento(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("acordo", acordos.acordo(id));
        model.addAttribute("documentos", acordos.documentosDo(id));
        model.addAttribute("parcelas", acordos.parcelasDo(id));
        model.addAttribute("jaPago", acordos.jaPagoDe(id));
        model.addAttribute("hoje", LocalDate.now());
        return "acordo";
    }

    @PostMapping("/{id}/quebrar")
    public String quebrar(@PathVariable UUID id, @RequestParam(required = false) String motivo,
                          RedirectAttributes redirect) {
        acordos.quebrar(id, motivo);
        redirect.addFlashAttribute("aviso",
                "Acordo quebrado. Os documentos originais voltaram para a cobrança.");
        return "redirect:/acordos/" + id;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/acordos";
    }
}
