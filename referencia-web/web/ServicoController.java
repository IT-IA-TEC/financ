package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.FormaDePreco;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.TratamentoDePreco;
import br.com.itia.financeiro.servico.CatalogoServico;
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
import java.util.UUID;

@Controller
@RequestMapping("/servicos")
public class ServicoController {

    private final CatalogoServico catalogo;
    private final ContextoEmpresa contexto;

    public ServicoController(CatalogoServico catalogo, ContextoEmpresa contexto) {
        this.catalogo = catalogo;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) UUID departamento,
                         @RequestParam(required = false) String situacao,
                         Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("servicos", catalogo.catalogo(busca, departamento, situacao));
        model.addAttribute("departamentos", catalogo.departamentos());
        model.addAttribute("departamentosAtivos", catalogo.departamentosAtivos());
        model.addAttribute("busca", busca);
        model.addAttribute("departamentoEscolhido", departamento);
        model.addAttribute("situacaoEscolhida", situacao);
        return "servicos";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam(required = false) String descricao,
                            @RequestParam(required = false) UUID departamentoId,
                            @RequestParam(required = false) String responsavelPadrao,
                            RedirectAttributes redirect) {
        Servico novo = catalogo.cadastrar(nome.trim(), descricao, departamentoId, responsavelPadrao);
        redirect.addFlashAttribute("aviso",
                "Serviço " + novo.getCodigo() + " cadastrado. Agora defina os itens e o preço.");
        return "redirect:/servicos/" + novo.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id,
                        @RequestParam(required = false, defaultValue = "gerais") String aba,
                        Model model) {
        Servico servico = catalogo.servico(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("servico", servico);
        model.addAttribute("aba", aba);
        model.addAttribute("departamentosAtivos", catalogo.departamentosAtivos());
        model.addAttribute("departamentos", catalogo.departamentos());
        model.addAttribute("unidadesAtivas", catalogo.unidadesAtivas());
        model.addAttribute("unidades", catalogo.unidades());
        model.addAttribute("formas", FormaDePreco.values());
        model.addAttribute("tratamentos", TratamentoDePreco.values());
        model.addAttribute("historico", catalogo.historicoDe(id));
        model.addAttribute("hoje", LocalDate.now());
        return "servico";
    }

    @PostMapping("/{id}/gerais")
    public String salvarGerais(@PathVariable UUID id,
                               @RequestParam String nome,
                               @RequestParam(required = false) String descricao,
                               @RequestParam(required = false) UUID departamentoId,
                               @RequestParam(required = false) String responsavelPadrao,
                               @RequestParam(required = false, defaultValue = "false") boolean ativo,
                               RedirectAttributes redirect) {
        catalogo.salvarDadosGerais(id, nome.trim(), descricao, departamentoId,
                responsavelPadrao, ativo);
        redirect.addFlashAttribute("aviso", "Dados do serviço salvos.");
        return "redirect:/servicos/" + id;
    }

    @PostMapping("/{id}/duplicar")
    public String duplicar(@PathVariable UUID id, RedirectAttributes redirect) {
        Servico copia = catalogo.duplicar(id);
        redirect.addFlashAttribute("aviso",
                "Cópia criada como " + copia.getCodigo()
                        + ". Ela nasce inativa: confira o preço antes de liberar.");
        return "redirect:/servicos/" + copia.getId();
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable UUID id, RedirectAttributes redirect) {
        catalogo.inativar(id);
        redirect.addFlashAttribute("aviso",
                "Serviço inativado. Ele some dos novos registros e o histórico fica intacto.");
        return "redirect:/servicos/" + id;
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable UUID id, RedirectAttributes redirect) {
        catalogo.reativar(id);
        redirect.addFlashAttribute("aviso", "Serviço reativado.");
        return "redirect:/servicos/" + id;
    }

    // ------------------------------------------------------------------ itens

    @PostMapping("/{id}/itens")
    public String adicionarItem(@PathVariable UUID id,
                                @RequestParam String nome,
                                @RequestParam(required = false) String descricao,
                                @RequestParam(required = false, defaultValue = "false") boolean obrigatorio,
                                @RequestParam(required = false) BigDecimal quantidadePadrao,
                                @RequestParam TratamentoDePreco tratamento,
                                @RequestParam(required = false) BigDecimal valor,
                                @RequestParam(required = false) UUID unidadeId,
                                RedirectAttributes redirect) {
        catalogo.adicionarItem(id, nome.trim(), descricao, obrigatorio, quantidadePadrao,
                tratamento, valor, unidadeId);
        redirect.addFlashAttribute("aviso", "Item adicionado.");
        return "redirect:/servicos/" + id + "?aba=itens";
    }

    @PostMapping("/{id}/itens/{itemId}")
    public String salvarItem(@PathVariable UUID id, @PathVariable UUID itemId,
                             @RequestParam String nome,
                             @RequestParam(required = false) String descricao,
                             @RequestParam(required = false, defaultValue = "false") boolean obrigatorio,
                             @RequestParam(required = false) BigDecimal quantidadePadrao,
                             @RequestParam TratamentoDePreco tratamento,
                             @RequestParam(required = false) BigDecimal valor,
                             @RequestParam(required = false) UUID unidadeId,
                             @RequestParam(required = false, defaultValue = "1") int ordem,
                             @RequestParam(required = false, defaultValue = "false") boolean ativo,
                             @RequestParam(required = false) String justificativa,
                             RedirectAttributes redirect) {
        catalogo.salvarItem(id, itemId, nome.trim(), descricao, obrigatorio, quantidadePadrao,
                tratamento, valor, unidadeId, ordem, ativo, justificativa);
        redirect.addFlashAttribute("aviso", "Item salvo.");
        return "redirect:/servicos/" + id + "?aba=itens";
    }

    @PostMapping("/{id}/itens/{itemId}/inativar")
    public String inativarItem(@PathVariable UUID id, @PathVariable UUID itemId,
                               RedirectAttributes redirect) {
        catalogo.inativarItem(id, itemId);
        redirect.addFlashAttribute("aviso", "Item inativado.");
        return "redirect:/servicos/" + id + "?aba=itens";
    }

    // ----------------------------------------------------------------- precos

    @PostMapping("/{id}/precos")
    public String salvarPreco(@PathVariable UUID id,
                              @RequestParam FormaDePreco forma,
                              @RequestParam(required = false) BigDecimal valor,
                              @RequestParam(required = false) UUID unidadeId,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenciaInicio,
                              @RequestParam(required = false) String justificativa,
                              RedirectAttributes redirect) {
        catalogo.salvarPreco(id, forma, valor, unidadeId, vigenciaInicio, justificativa);
        redirect.addFlashAttribute("aviso",
                "Preço salvo. O que já foi registrado antes continua com o preço de antes.");
        return "redirect:/servicos/" + id + "?aba=precos";
    }

    // ------------------------------------------------------- cadastros de apoio

    @PostMapping("/departamentos")
    public String cadastrarDepartamento(@RequestParam String nome,
                                        @RequestParam(required = false) String descricao,
                                        @RequestParam(required = false) String voltarPara,
                                        RedirectAttributes redirect) {
        catalogo.cadastrarDepartamento(nome.trim(), descricao);
        redirect.addFlashAttribute("aviso", "Departamento cadastrado.");
        return "redirect:" + destino(voltarPara);
    }

    @PostMapping("/departamentos/{id}")
    public String salvarDepartamento(@PathVariable UUID id,
                                     @RequestParam String nome,
                                     @RequestParam(required = false) String descricao,
                                     @RequestParam(required = false, defaultValue = "false") boolean ativo,
                                     @RequestParam(required = false) String voltarPara,
                                     RedirectAttributes redirect) {
        catalogo.salvarDepartamento(id, nome.trim(), descricao, ativo);
        redirect.addFlashAttribute("aviso", "Departamento salvo.");
        return "redirect:" + destino(voltarPara);
    }

    @PostMapping("/unidades")
    public String cadastrarUnidade(@RequestParam String nome,
                                   @RequestParam(required = false) String voltarPara,
                                   RedirectAttributes redirect) {
        catalogo.cadastrarUnidade(nome.trim());
        redirect.addFlashAttribute("aviso", "Unidade cadastrada.");
        return "redirect:" + destino(voltarPara);
    }

    @PostMapping("/unidades/{id}")
    public String salvarUnidade(@PathVariable UUID id,
                                @RequestParam String nome,
                                @RequestParam(required = false, defaultValue = "false") boolean ativo,
                                @RequestParam(required = false) String voltarPara,
                                RedirectAttributes redirect) {
        catalogo.salvarUnidade(id, nome.trim(), ativo);
        redirect.addFlashAttribute("aviso", "Unidade salva.");
        return "redirect:" + destino(voltarPara);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/servicos";
    }

    /** Só volta para dentro do próprio sistema. */
    private String destino(String voltarPara) {
        if (voltarPara != null && voltarPara.startsWith("/servicos")) {
            return voltarPara;
        }
        return "/servicos";
    }
}
