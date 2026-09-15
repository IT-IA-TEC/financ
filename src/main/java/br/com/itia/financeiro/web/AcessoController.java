package br.com.itia.financeiro.web;

import br.com.itia.financeiro.config.GuardaOndeParou;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entrada e saida do sistema.
 *
 * Por enquanto SEM TRAVA: clicar em entrar ja entra. A conferencia de senha
 * entra depois, junto com a ponte de login do ERP. O lugar onde ela vai ficar
 * ja esta marcado aqui embaixo.
 */
@Controller
public class AcessoController {

    private final ContextoEmpresa contexto;
    private final Acessos acessos;

    public AcessoController(ContextoEmpresa contexto, Acessos acessos) {
        this.contexto = contexto;
        this.acessos = acessos;
    }

    @GetMapping("/entrar")
    public String tela() {
        return "entrar";
    }

    @PostMapping("/entrar")
    public String entrar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String voltarPara,
                         HttpSession sessao) {
        // AQUI entra a conferencia de senha quando o login for para valer.
        contexto.entrar(nome, email);
        // A pessoa passa a ter identidade: e dela que sai a alcada em cada empresa.
        contexto.identificar(acessos.identificar(nome, email).getId());

        // Volta para a tela onde a pessoa estava da ultima vez, quando ela
        // manda um endereco de dentro do proprio sistema.
        if (voltarPara != null && voltarPara.startsWith("/") && !voltarPara.startsWith("//")
                && !voltarPara.startsWith("/entrar")) {
            sessao.setAttribute(GuardaOndeParou.ONDE_PAROU, voltarPara);
            return "redirect:" + voltarPara;
        }
        return "redirect:/empresas";
    }

    @GetMapping("/sair")
    public String sair(HttpSession sessao) {
        contexto.sair();
        sessao.invalidate();
        return "redirect:/entrar";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "perfil";
    }
}
