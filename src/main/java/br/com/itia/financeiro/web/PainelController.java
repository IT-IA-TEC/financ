package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import br.com.itia.financeiro.servico.PainelServico;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class PainelController {

    private final PainelServico painel;
    private final FinanceiroServico financeiro;
    private final ContextoEmpresa contexto;

    public PainelController(PainelServico painel, FinanceiroServico financeiro,
                            ContextoEmpresa contexto) {
        this.painel = painel;
        this.financeiro = financeiro;
        this.contexto = contexto;
    }

    @GetMapping("/")
    public String abrir(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("resumo", painel.resumoDoMes());
        model.addAttribute("emAberto", financeiro.titulosEmAberto());
        model.addAttribute("eventos", financeiro.ultimosEventos());
        model.addAttribute("hoje", LocalDate.now());
        return "painel";
    }
}
