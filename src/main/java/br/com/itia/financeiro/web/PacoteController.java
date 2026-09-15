package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.AbrangenciaDoPacote;
import br.com.itia.financeiro.dominio.Pacote;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.PeriodoDoLimite;
import br.com.itia.financeiro.dominio.SituacaoDaContratacao;
import br.com.itia.financeiro.dominio.TratamentoDoExcedente;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.PacoteServico;
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

/**
 * A aba de pacotes, dentro do módulo Pacotes e serviços.
 */
@Controller
@RequestMapping("/pacotes")
public class PacoteController {

    private final PacoteServico pacotes;
    private final ContextoEmpresa contexto;

    public PacoteController(PacoteServico pacotes, ContextoEmpresa contexto) {
        this.pacotes = pacotes;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) String situacao,
                         Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("pacotes", pacotes.lista(busca, situacao));
        model.addAttribute("busca", busca);
        model.addAttribute("situacaoEscolhida", situacao);
        model.addAttribute("periodicidades", Periodicidade.values());
        return "pacotes";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam(required = false) String descricao,
                            @RequestParam(required = false) BigDecimal valor,
                            @RequestParam(required = false) Periodicidade periodicidade,
                            @RequestParam(required = false) String periodicidadeOutra,
                            RedirectAttributes redirect) {
        Pacote novo = pacotes.cadastrar(nome.trim(), descricao, valor, periodicidade,
                periodicidadeOutra);
        redirect.addFlashAttribute("aviso",
                "Pacote " + novo.getCodigo() + " cadastrado. Agora monte a composição.");
        return "redirect:/pacotes/" + novo.getId() + "?aba=composicao";
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id,
                        @RequestParam(required = false, defaultValue = "dados") String aba,
                        Model model) {
        Pacote pacote = pacotes.pacote(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("pacote", pacote);
        model.addAttribute("aba", aba);
        model.addAttribute("servicosDisponiveis", pacotes.servicosDisponiveis());
        model.addAttribute("clientes", pacotes.clientes());
        model.addAttribute("unidades", pacotes.unidadesDaEmpresa());
        model.addAttribute("contratacoes", pacotes.contratacoesDo(id));
        model.addAttribute("periodicidades", Periodicidade.values());
        model.addAttribute("abrangencias", AbrangenciaDoPacote.values());
        model.addAttribute("periodos", PeriodoDoLimite.values());
        model.addAttribute("excedentes", TratamentoDoExcedente.values());
        model.addAttribute("situacoesContratacao", SituacaoDaContratacao.values());
        model.addAttribute("hoje", LocalDate.now());
        return "pacote";
    }

    @PostMapping("/{id}/dados")
    public String salvarDados(@PathVariable UUID id,
                              @RequestParam String nome,
                              @RequestParam(required = false) String descricao,
                              @RequestParam(required = false) BigDecimal valor,
                              @RequestParam Periodicidade periodicidade,
                              @RequestParam(required = false) String periodicidadeOutra,
                              @RequestParam(required = false, defaultValue = "false") boolean ativo,
                              RedirectAttributes redirect) {
        pacotes.salvarDados(id, nome.trim(), descricao, valor, periodicidade,
                periodicidadeOutra, ativo);
        redirect.addFlashAttribute("aviso", "Dados do pacote salvos.");
        return "redirect:/pacotes/" + id;
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable UUID id, RedirectAttributes redirect) {
        pacotes.inativar(id);
        redirect.addFlashAttribute("aviso",
                "Pacote inativado. Ele some das novas contratações e o que já foi contratado fica.");
        return "redirect:/pacotes/" + id;
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable UUID id, RedirectAttributes redirect) {
        pacotes.reativar(id);
        redirect.addFlashAttribute("aviso", "Pacote reativado.");
        return "redirect:/pacotes/" + id;
    }

    // ------------------------------------------------------------- composicao

    @PostMapping("/{id}/composicao")
    public String adicionarLinha(@PathVariable UUID id,
                                 @RequestParam UUID servicoId,
                                 @RequestParam AbrangenciaDoPacote abrangencia,
                                 @RequestParam(required = false) UUID itemId,
                                 @RequestParam(required = false, defaultValue = "false") boolean ilimitado,
                                 @RequestParam(required = false) BigDecimal quantidadeIncluida,
                                 @RequestParam PeriodoDoLimite periodoLimite,
                                 @RequestParam TratamentoDoExcedente excedente,
                                 @RequestParam(required = false) String observacao,
                                 RedirectAttributes redirect) {
        pacotes.adicionarLinha(id, servicoId, abrangencia, itemId, ilimitado, quantidadeIncluida,
                periodoLimite, excedente, observacao);
        redirect.addFlashAttribute("aviso", "Serviço incluído no pacote.");
        return "redirect:/pacotes/" + id + "?aba=composicao";
    }

    @PostMapping("/{id}/composicao/{linhaId}")
    public String salvarLinha(@PathVariable UUID id, @PathVariable UUID linhaId,
                              @RequestParam UUID servicoId,
                              @RequestParam AbrangenciaDoPacote abrangencia,
                              @RequestParam(required = false) UUID itemId,
                              @RequestParam(required = false, defaultValue = "false") boolean ilimitado,
                              @RequestParam(required = false) BigDecimal quantidadeIncluida,
                              @RequestParam PeriodoDoLimite periodoLimite,
                              @RequestParam TratamentoDoExcedente excedente,
                              @RequestParam(required = false) String observacao,
                              @RequestParam(required = false, defaultValue = "1") int ordem,
                              RedirectAttributes redirect) {
        pacotes.salvarLinha(id, linhaId, servicoId, abrangencia, itemId, ilimitado,
                quantidadeIncluida, periodoLimite, excedente, observacao, ordem);
        redirect.addFlashAttribute("aviso", "Linha da composição salva.");
        return "redirect:/pacotes/" + id + "?aba=composicao";
    }

    @PostMapping("/{id}/composicao/{linhaId}/remover")
    public String removerLinha(@PathVariable UUID id, @PathVariable UUID linhaId,
                               RedirectAttributes redirect) {
        pacotes.removerLinha(id, linhaId);
        redirect.addFlashAttribute("aviso", "Linha retirada do pacote.");
        return "redirect:/pacotes/" + id + "?aba=composicao";
    }

    // ----------------------------------------------------------- contratacoes

    @PostMapping("/{id}/contratacoes")
    public String contratar(@PathVariable UUID id,
                            @RequestParam UUID pagadorId,
                            @RequestParam(required = false) UUID unidadeId,
                            @RequestParam(required = false) BigDecimal valorAcordado,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                            @RequestParam(required = false) String observacao,
                            RedirectAttributes redirect) {
        pacotes.contratar(id, pagadorId, unidadeId, valorAcordado, inicio, fim, observacao);
        redirect.addFlashAttribute("aviso", "Contratação registrada.");
        return "redirect:/pacotes/" + id + "?aba=contratacoes";
    }

    @PostMapping("/{id}/contratacoes/{contratacaoId}")
    public String salvarContratacao(@PathVariable UUID id, @PathVariable UUID contratacaoId,
                                    @RequestParam UUID pagadorId,
                                    @RequestParam(required = false) UUID unidadeId,
                                    @RequestParam(required = false) BigDecimal valorAcordado,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                    @RequestParam SituacaoDaContratacao situacao,
                                    @RequestParam(required = false) String observacao,
                                    RedirectAttributes redirect) {
        pacotes.salvarContratacao(contratacaoId, id, pagadorId, unidadeId, valorAcordado,
                inicio, fim, situacao, observacao);
        redirect.addFlashAttribute("aviso", "Contratação salva.");
        return "redirect:/pacotes/" + id + "?aba=contratacoes";
    }

    @PostMapping("/{id}/contratacoes/{contratacaoId}/encerrar")
    public String encerrar(@PathVariable UUID id, @PathVariable UUID contratacaoId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate quando,
                           RedirectAttributes redirect) {
        pacotes.encerrarContratacao(contratacaoId, quando);
        redirect.addFlashAttribute("aviso", "Contratação encerrada.");
        return "redirect:/pacotes/" + id + "?aba=contratacoes";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/pacotes";
    }
}
