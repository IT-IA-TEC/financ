package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.Regras;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * A porta de entrada (escolher a empresa) e a engrenagem (configurar, criar e
 * excluir empresa).
 */
@Controller
public class EmpresaController {

    private final FinanceiroServico financeiro;
    private final Acessos acessos;
    private final Regras regras;
    private final ContextoEmpresa contexto;

    public EmpresaController(FinanceiroServico financeiro, Acessos acessos, Regras regras,
                             ContextoEmpresa contexto) {
        this.financeiro = financeiro;
        this.acessos = acessos;
        this.regras = regras;
        this.contexto = contexto;
    }

    /** As regras que esta empresa liga ou desliga. */
    @GetMapping("/empresas/regras")
    public String regras(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("regra", regras.daEmpresa());
        return "regras";
    }

    @PostMapping("/empresas/regras")
    public String salvarRegras(
            @RequestParam(required = false, defaultValue = "false") boolean pixIdentificador,
            @RequestParam(required = false, defaultValue = "false") boolean pixAutomatico,
            @RequestParam(required = false, defaultValue = "false") boolean agenteSeIdentifica,
            @RequestParam(required = false, defaultValue = "false") boolean cobrarJuros,
            @RequestParam(required = false) java.math.BigDecimal jurosAoMes,
            @RequestParam(required = false) java.math.BigDecimal multaPorAtraso,
            @RequestParam(required = false, defaultValue = "0") int carenciaDias,
            RedirectAttributes redirect) {
        regras.salvar(pixIdentificador, pixAutomatico, agenteSeIdentifica, cobrarJuros,
                jurosAoMes, multaPorAtraso, carenciaDias);
        redirect.addFlashAttribute("aviso",
                "Regras salvas. Elas valem daqui em diante, sem mexer no que já foi lançado.");
        return "redirect:/empresas/regras";
    }

    // ------------------------------------------------------------- escolher

    @GetMapping("/empresas")
    public String escolher(Model model) {
        model.addAttribute("empresas", financeiro.empresasAtivas());
        model.addAttribute("movimento", financeiro.movimentoPorEmpresa());
        return "empresas";
    }

    @GetMapping("/empresas/{id}/entrar")
    public String entrar(@PathVariable UUID id) {
        contexto.escolher(id);
        // A alcada e por empresa: ao trocar de empresa, o cracha muda junto.
        if (contexto.getUsuarioId() != null) {
            contexto.assumirPapel(
                    acessos.crachaDe(contexto.getUsuarioId(), id).getPapel());
        }
        return "redirect:/";
    }

    /** Quem cuida desta empresa, e com que alçada. Só diretor mexe. */
    @GetMapping("/empresas/pessoas")
    public String pessoas(Model model) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        var lista = acessos.daEmpresa(contexto.exigirEmpresaId());
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("crachas", lista);
        model.addAttribute("pessoas", acessos.pessoas(lista));
        model.addAttribute("papeis", UsuarioEmpresa.Papel.values());
        return "empresas-pessoas";
    }

    @PostMapping("/empresas/pessoas/{usuarioId}")
    public String mudarPapel(@PathVariable UUID usuarioId,
                             @RequestParam UsuarioEmpresa.Papel papel,
                             RedirectAttributes redirect) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        acessos.mudarPapel(usuarioId, contexto.exigirEmpresaId(), papel,
                contexto.getUsuarioId());
        redirect.addFlashAttribute("aviso", "Alçada alterada.");
        return "redirect:/empresas/pessoas";
    }

    // ---------------------------------------------------------- engrenagem

    @GetMapping("/empresas/configurar")
    public String configurar(Model model) {
        model.addAttribute("empresas", financeiro.todasAsEmpresas());
        model.addAttribute("movimento", financeiro.movimentoPorEmpresa());
        return "empresas-configurar";
    }

    @PostMapping("/empresas")
    public String cadastrar(@RequestParam String apelido,
                            @RequestParam String nome,
                            @RequestParam(required = false) String cnpj,
                            @RequestParam(required = false) String chavePix,
                            RedirectAttributes redirect) {
        Empresa empresa = financeiro.cadastrarEmpresa(limpar(apelido).toLowerCase(),
                limpar(nome), limpar(cnpj), limpar(chavePix));
        redirect.addFlashAttribute("aviso", "Empresa " + empresa.getNome() + " cadastrada.");
        return "redirect:/empresas/configurar";
    }

    @PostMapping("/empresas/{id}")
    public String salvar(@PathVariable UUID id,
                         @RequestParam String apelido,
                         @RequestParam String nome,
                         @RequestParam(required = false) String cnpj,
                         @RequestParam(required = false) String chavePix,
                         RedirectAttributes redirect) {
        financeiro.atualizarEmpresa(id, limpar(apelido).toLowerCase(), limpar(nome),
                limpar(cnpj), limpar(chavePix));
        redirect.addFlashAttribute("aviso", "Dados de " + limpar(nome) + " salvos.");
        return "redirect:/empresas/configurar";
    }

    @PostMapping("/empresas/{id}/excluir")
    public String excluir(@PathVariable UUID id, RedirectAttributes redirect) {
        Empresa empresa = financeiro.empresa(id);
        financeiro.excluirEmpresa(id);
        redirect.addFlashAttribute("aviso",
                "Empresa " + empresa.getNome() + " excluida.");
        return "redirect:/empresas/configurar";
    }

    @PostMapping("/empresas/{id}/desativar")
    public String desativar(@PathVariable UUID id, RedirectAttributes redirect) {
        financeiro.desativarEmpresa(id);
        redirect.addFlashAttribute("aviso",
                "Empresa desativada. Ela some da porta de entrada, e nada do que ela tem e perdido.");
        return "redirect:/empresas/configurar";
    }

    @PostMapping("/empresas/{id}/reativar")
    public String reativar(@PathVariable UUID id, RedirectAttributes redirect) {
        financeiro.reativarEmpresa(id);
        redirect.addFlashAttribute("aviso", "Empresa reativada.");
        return "redirect:/empresas/configurar";
    }

    /** Problema aqui volta para a propria engrenagem, com o recado na tela. */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/empresas/configurar";
    }

    private String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
