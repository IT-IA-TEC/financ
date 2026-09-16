package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.TipoAutenticacao;
import br.com.itia.financeiro.dominio.TipoIntegracao;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.IntegracaoServico;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/integracoes")
public class IntegracaoController {

    /** Campos da tela que não são credencial. */
    private static final Set<String> CAMPOS_DA_TELA = Set.of("nome", "ambiente", "baseUrl",
            "observacao", "tipo", "autenticacao", "tempoLimite", "tentativas", "espera",
            "verificarAssinatura");

    private final IntegracaoServico integracoes;
    private final br.com.itia.financeiro.servico.BaixaAutomatica baixa;
    private final ContextoEmpresa contexto;

    public IntegracaoController(IntegracaoServico integracoes,
                                br.com.itia.financeiro.servico.BaixaAutomatica baixa,
                                ContextoEmpresa contexto) {
        this.integracoes = integracoes;
        this.baixa = baixa;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("integracoes", integracoes.daEmpresa());
        model.addAttribute("conectores", Conector.values());
        model.addAttribute("tipos", TipoIntegracao.values());
        model.addAttribute("autenticacoes", TipoAutenticacao.values());
        model.addAttribute("eventos", integracoes.ultimosEventos());
        return "integracoes";
    }

    /** Tenta de novo a baixa dos avisos que ficaram parados. */
    @PostMapping("/reprocessar")
    public String reprocessar(RedirectAttributes redirect) {
        int baixados = baixa.reprocessarParados(contexto.exigirEmpresaId());
        redirect.addFlashAttribute("aviso", baixados == 0
                ? "Nenhum aviso parado virou baixa. O motivo de cada um está na lista."
                : baixados + " aviso(s) viraram baixa agora.");
        return "redirect:/integracoes";
    }

    /** A documentação do módulo, dentro do próprio módulo. */
    @GetMapping("/ajuda")
    public String ajuda(Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("tipos", TipoIntegracao.values());
        model.addAttribute("autenticacoes", TipoAutenticacao.values());
        model.addAttribute("conectores", Conector.values());
        return "integracoes-ajuda";
    }

    @PostMapping
    public String criar(@RequestParam Conector conector,
                        @RequestParam(required = false) TipoIntegracao tipo,
                        @RequestParam(required = false) TipoAutenticacao autenticacao,
                        @RequestParam String nome,
                        RedirectAttributes redirect) {
        Integracao nova = integracoes.criar(conector, tipo, autenticacao, nome.trim());
        redirect.addFlashAttribute("aviso",
                "Integração criada. Preencha o acesso, confira as operações e teste antes de ligar.");
        return "redirect:/integracoes/" + nova.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model, HttpServletRequest pedido) {
        Integracao integracao = integracoes.buscar(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("integracao", integracao);
        model.addAttribute("valores", integracoes.paraMostrar(integracao));
        model.addAttribute("faltando", integracoes.faltando(integracao));
        model.addAttribute("operacoes", integracoes.operacoesDe(id));
        model.addAttribute("eventos", integracoes.eventosDe(id));
        model.addAttribute("tipos", TipoIntegracao.values());
        model.addAttribute("autenticacoes", TipoAutenticacao.values());
        model.addAttribute("enderecoWebhook", enderecoDe(pedido) + integracao.getCaminhoDoWebhook());
        return "integracao";
    }

    @PostMapping("/{id}")
    public String salvar(@PathVariable UUID id,
                         @RequestParam String nome,
                         @RequestParam(required = false) String ambiente,
                         @RequestParam(required = false) String baseUrl,
                         @RequestParam(required = false) String observacao,
                         @RequestParam(required = false) TipoIntegracao tipo,
                         @RequestParam(required = false) TipoAutenticacao autenticacao,
                         @RequestParam(required = false) Integer tempoLimite,
                         @RequestParam(required = false) Integer tentativas,
                         @RequestParam(required = false) Integer espera,
                         @RequestParam(required = false, defaultValue = "false") boolean verificarAssinatura,
                         @RequestParam Map<String, String> tudo,
                         RedirectAttributes redirect) {
        Map<String, String> credenciais = new HashMap<>(tudo);
        credenciais.keySet().removeAll(CAMPOS_DA_TELA);
        integracoes.salvar(id, nome.trim(), ambiente, baseUrl, observacao, tipo, autenticacao,
                tempoLimite, tentativas, espera, verificarAssinatura, credenciais);
        redirect.addFlashAttribute("aviso", "Configuração salva.");
        return "redirect:/integracoes/" + id;
    }

    // ---------------------------------------------------------------- operacoes

    @PostMapping("/{id}/operacoes")
    public String criarOperacao(@PathVariable UUID id,
                                @RequestParam String nome,
                                @RequestParam String verbo,
                                @RequestParam String caminho,
                                @RequestParam(required = false) String paraQue,
                                RedirectAttributes redirect) {
        integracoes.criarOperacao(id, nome.trim(), verbo, caminho.trim(), paraQue);
        redirect.addFlashAttribute("aviso", "Operação cadastrada.");
        return "redirect:/integracoes/" + id;
    }

    @PostMapping("/{id}/operacoes/{operacaoId}/apagar")
    public String apagarOperacao(@PathVariable UUID id, @PathVariable UUID operacaoId,
                                 RedirectAttributes redirect) {
        integracoes.apagarOperacao(id, operacaoId);
        redirect.addFlashAttribute("aviso", "Operação removida.");
        return "redirect:/integracoes/" + id;
    }

    @PostMapping("/{id}/operacoes/{operacaoId}/executar")
    public String executar(@PathVariable UUID id, @PathVariable UUID operacaoId,
                           @RequestParam(required = false) String corpo,
                           RedirectAttributes redirect) {
        integracoes.executar(id, operacaoId, corpo);
        redirect.addFlashAttribute("aviso",
                "Chamada feita. O que voltou está no movimento, aqui embaixo.");
        return "redirect:/integracoes/" + id;
    }

    // ------------------------------------------------------------------ estado

    @PostMapping("/{id}/testar")
    public String testar(@PathVariable UUID id, RedirectAttributes redirect) {
        boolean respondeu = integracoes.testar(id);
        redirect.addFlashAttribute(respondeu ? "aviso" : "erro",
                respondeu ? "A outra ponta respondeu." : "A outra ponta não respondeu.");
        return "redirect:/integracoes/" + id;
    }

    @PostMapping("/{id}/ligar")
    public String ligar(@PathVariable UUID id, RedirectAttributes redirect) {
        integracoes.ligar(id);
        redirect.addFlashAttribute("aviso", "Integração ligada.");
        return "redirect:/integracoes/" + id;
    }

    @PostMapping("/{id}/desligar")
    public String desligar(@PathVariable UUID id, RedirectAttributes redirect) {
        integracoes.desligar(id);
        redirect.addFlashAttribute("aviso", "Integração desligada. Nada entra nem sai por ela.");
        return "redirect:/integracoes/" + id;
    }

    @PostMapping("/{id}/trocar-segredo")
    public String trocarSegredo(@PathVariable UUID id, RedirectAttributes redirect) {
        integracoes.trocarSegredo(id);
        redirect.addFlashAttribute("aviso",
                "Endereço de recebimento trocado. Avise o outro sistema, porque o antigo parou.");
        return "redirect:/integracoes/" + id;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, HttpServletRequest pedido,
                           RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        // Se quem falhou foi a propria lista, mandar de volta para ela criaria
        // um laco sem fim. Nesse caso a pessoa vai para o painel com o recado.
        boolean falhouNaLista = "/integracoes".equals(pedido.getRequestURI());
        return falhouNaLista ? "redirect:/" : "redirect:/integracoes";
    }

    private String enderecoDe(HttpServletRequest pedido) {
        String porta = (pedido.getServerPort() == 80 || pedido.getServerPort() == 443)
                ? "" : ":" + pedido.getServerPort();
        return pedido.getScheme() + "://" + pedido.getServerName() + porta;
    }
}
