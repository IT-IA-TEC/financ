package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Cobranca;
import br.com.itia.financeiro.dominio.OrigemDaCobranca;
import br.com.itia.financeiro.dominio.SituacaoDaCobranca;
import br.com.itia.financeiro.servico.Cobrancas;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FechamentoDoMes;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * A aba de cobranças, dentro do módulo Pacotes e serviços.
 *
 * Aqui só entram as cobranças que nasceram de pacote ou de serviço. O resto do
 * contas a receber continua em Títulos.
 */
@Controller
@RequestMapping("/cobrancas")
public class CobrancaController {

    private final Cobrancas cobrancas;
    private final FechamentoDoMes fechamento;
    private final PacoteServico pacotes;
    private final ContextoEmpresa contexto;

    public CobrancaController(Cobrancas cobrancas, FechamentoDoMes fechamento,
                              PacoteServico pacotes, ContextoEmpresa contexto) {
        this.cobrancas = cobrancas;
        this.fechamento = fechamento;
        this.pacotes = pacotes;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String origem,
                         @RequestParam(required = false) String situacao,
                         @RequestParam(required = false) UUID cliente,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                         Model model) {
        // Antes de mostrar, confere no titulo o que ja foi pago.
        cobrancas.conferirPagamentos();

        List<Cobranca> lista = cobrancas.lista(origem, situacao, cliente, de, ate);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("cobrancas", lista);
        model.addAttribute("resumo", cobrancas.resumo(lista));
        model.addAttribute("clientes", pacotes.clientes());
        model.addAttribute("origens", OrigemDaCobranca.values());
        model.addAttribute("situacoes", SituacaoDaCobranca.values());
        model.addAttribute("origemEscolhida", origem);
        model.addAttribute("situacaoEscolhida", situacao);
        model.addAttribute("clienteEscolhido", cliente);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        model.addAttribute("temSistemaDeCobranca", cobrancas.sistemaDeCobranca().isPresent());
        model.addAttribute("competencia", YearMonth.now().toString());
        model.addAttribute("hoje", LocalDate.now());
        return "cobrancas";
    }

    /** A prévia da leva: o que vai sair e o que fica de fora, antes de gerar. */
    @GetMapping("/fechamento")
    public String fechamento(@RequestParam(required = false) String competencia, Model model) {
        YearMonth periodo = competencia == null || competencia.isBlank()
                ? YearMonth.now() : YearMonth.parse(competencia);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("previa", fechamento.previa(periodo));
        model.addAttribute("faturado", fechamento.faturadoDe(periodo));
        model.addAttribute("competencias", fechamento.competencias());
        model.addAttribute("competencia", periodo.toString());
        model.addAttribute("hoje", LocalDate.now());
        return "fechamento";
    }

    @PostMapping("/gerar")
    public String gerar(@RequestParam String competencia,
                        @RequestParam(required = false, defaultValue = "10") int diaDoVencimento,
                        RedirectAttributes redirect) {
        int criadas = fechamento.gerar(YearMonth.parse(competencia), diaDoVencimento);
        redirect.addFlashAttribute("aviso", criadas == 0
                ? "Nenhuma cobrança nova. As contratações deste período já tinham cobrança."
                : criadas + " cobrança(s) de pacote gerada(s) para conferência.");
        return "redirect:/cobrancas/fechamento?competencia=" + competencia;
    }

    @PostMapping("/fechamento/importar")
    public String importar(@RequestParam String competencia,
                           @RequestParam MultipartFile arquivo,
                           @RequestParam(required = false, defaultValue = "true") boolean substituir,
                           RedirectAttributes redirect) {
        FechamentoDoMes.Importacao resultado = fechamento.importar(
                YearMonth.parse(competencia), arquivo, substituir);
        redirect.addFlashAttribute("aviso", resultado.lidas() + " linha(s) lida(s), "
                + resultado.amarradas() + " amarrada(s) a um cliente e "
                + resultado.semDono() + " sem dono."
                + (resultado.recusadas().isEmpty() ? ""
                : " Recusadas: " + String.join("; ", resultado.recusadas())));
        return "redirect:/cobrancas/fechamento?competencia=" + competencia;
    }

    @PostMapping("/fechamento/fechar")
    public String fechar(@RequestParam String competencia, RedirectAttributes redirect) {
        fechamento.fechar(YearMonth.parse(competencia));
        redirect.addFlashAttribute("aviso",
                "Mês fechado. Daqui em diante ele não gera nem recalcula nada.");
        return "redirect:/cobrancas/fechamento?competencia=" + competencia;
    }

    @PostMapping("/fechamento/reabrir")
    public String reabrir(@RequestParam String competencia, @RequestParam String motivo,
                          RedirectAttributes redirect) {
        fechamento.reabrir(YearMonth.parse(competencia), motivo);
        redirect.addFlashAttribute("aviso", "Mês reaberto, com o motivo registrado.");
        return "redirect:/cobrancas/fechamento?competencia=" + competencia;
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model) {
        Cobranca cobranca = cobrancas.cobranca(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("cobranca", cobranca);
        model.addAttribute("temSistemaDeCobranca", cobrancas.sistemaDeCobranca().isPresent());
        model.addAttribute("hoje", LocalDate.now());
        return "cobranca";
    }

    @PostMapping("/{id}")
    public String ajustar(@PathVariable UUID id,
                          @RequestParam BigDecimal valorOriginal,
                          @RequestParam(required = false) BigDecimal desconto,
                          @RequestParam(required = false) String justificativa,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimento,
                          RedirectAttributes redirect) {
        cobrancas.ajustarValor(id, valorOriginal, desconto, justificativa, vencimento);
        redirect.addFlashAttribute("aviso", "Cobrança ajustada.");
        return "redirect:/cobrancas/" + id;
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable UUID id, RedirectAttributes redirect) {
        cobrancas.aprovar(id);
        redirect.addFlashAttribute("aviso",
                "Cobrança aprovada. O título já está lançado no financeiro.");
        return "redirect:/cobrancas/" + id;
    }

    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable UUID id, RedirectAttributes redirect) {
        cobrancas.enviar(id);
        redirect.addFlashAttribute("aviso", "Cobrança enviada ao sistema de cobrança.");
        return "redirect:/cobrancas/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, RedirectAttributes redirect) {
        cobrancas.cancelar(id);
        redirect.addFlashAttribute("aviso", "Cobrança cancelada.");
        return "redirect:/cobrancas/" + id;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/cobrancas";
    }
}
