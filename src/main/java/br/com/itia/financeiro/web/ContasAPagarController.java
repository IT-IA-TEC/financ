package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Conciliacao;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.Execucao;
import br.com.itia.financeiro.dominio.GrupoDeNatureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.OrigemDoRegistro;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import br.com.itia.financeiro.servico.FiltroDeContas;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.DocumentoServico;
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
import java.util.Map;
import java.util.UUID;

/**
 * O contas a pagar de cada empresa.
 */
@Controller
@RequestMapping("/pagar")
public class ContasAPagarController {

    private final ContasAPagar contas;
    private final DocumentoServico documentos;
    private final ContextoEmpresa contexto;

    public ContasAPagarController(ContasAPagar contas, DocumentoServico documentos,
                                  ContextoEmpresa contexto) {
        this.contas = contas;
        this.documentos = documentos;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam(required = false, defaultValue = "todas") String visao,
                         @RequestParam(required = false) List<String> f_situacao,
                         @RequestParam Map<String, String> parametros,
                         Model model) {
        // Empresa nova ja nasce com plano gerencial e centros de custo proprios.
        contas.prepararEmpresa();

        FiltroDeContas filtro = new FiltroDeContas(parametros, f_situacao);
        List<Obrigacao> lista = contas.lista(visao, filtro);

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("obrigacoes", lista);
        model.addAttribute("resumo", contas.resumo(lista));
        model.addAttribute("custoPorCentro", contas.custoPorCentro(lista));
        model.addAttribute("contagem", contas.contagemDasVisoes());
        model.addAttribute("favorecidos", contas.favorecidosAtivos());
        model.addAttribute("centros", contas.centrosAtivos());
        model.addAttribute("naturezas", contas.naturezasAtivas());
        model.addAttribute("contasFinanceiras", contas.contasAtivas());
        model.addAttribute("bens", contas.bensAtivos());
        model.addAttribute("empresasDoGrupo", contas.empresasDoGrupo());
        model.addAttribute("tipos", TipoDeOperacao.values());
        model.addAttribute("situacoesDaColuna",
                List.of("em aberto", "parcial", "liquidada", "vencida", "cancelada"));
        model.addAttribute("filtro", filtro);
        model.addAttribute("visao", visao);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("competencia", YearMonth.now().toString());
        return "pagar";
    }

    @PostMapping
    public String lancar(@RequestParam UUID favorecidoId,
                         @RequestParam String descricao,
                         @RequestParam(required = false, defaultValue = "DESPESA") TipoDeOperacao tipo,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissao,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimento,
                         @RequestParam BigDecimal valor,
                         @RequestParam(required = false) UUID contaId,
                         @RequestParam(required = false) UUID empresaPagadoraId,
                         @RequestParam(required = false) String pessoaRelacionada,
                         @RequestParam(required = false) UUID bemId,
                         @RequestParam(required = false) String solicitante,
                         @RequestParam(required = false) String observacao,
                         @RequestParam(required = false, defaultValue = "false") boolean rascunho,
                         @RequestParam(required = false) UUID naturezaId,
                         @RequestParam(required = false) UUID centroId,
                         RedirectAttributes redirect) {
        Obrigacao nova = contas.lancar(favorecidoId, descricao, tipo, emissao, competencia,
                vencimento, valor, contaId, empresaPagadoraId, pessoaRelacionada, null, bemId,
                solicitante, observacao, rascunho, OrigemDoRegistro.MANUAL, null);

        // Classificacao ja no cadastro, quando a pessoa souber: assim a conta
        // nao nasce sem explicacao.
        if (naturezaId != null || centroId != null) {
            contas.adicionarItem(nova.getId(), descricao, BigDecimal.ONE, valor, naturezaId,
                    centroId, pessoaRelacionada, bemId);
        }
        redirect.addFlashAttribute("aviso", rascunho
                ? "Rascunho guardado. Ele fica visível como pendente até alguém completar."
                : "Conta " + nova.getNumero() + " lançada.");
        return "redirect:/pagar/" + nova.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model) {
        Obrigacao obrigacao = contas.obrigacao(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("obrigacao", obrigacao);
        model.addAttribute("pendencias", obrigacao.pendencias());
        model.addAttribute("favorecidos", contas.favorecidosAtivos());
        model.addAttribute("centros", contas.centrosAtivos());
        model.addAttribute("naturezas", contas.naturezasAtivas());
        model.addAttribute("contasFinanceiras", contas.contasAtivas());
        model.addAttribute("bens", contas.bensAtivos());
        model.addAttribute("empresasDoGrupo", contas.empresasDoGrupo());
        model.addAttribute("tipos", TipoDeOperacao.values());
        model.addAttribute("execucoes", Execucao.values());
        model.addAttribute("conciliacoes", Conciliacao.values());
        model.addAttribute("tiposDeDocumento", Documento.TIPOS);
        model.addAttribute("hoje", LocalDate.now());
        return "obrigacao";
    }

    @PostMapping("/{id}")
    public String salvar(@PathVariable UUID id,
                         @RequestParam UUID favorecidoId,
                         @RequestParam String descricao,
                         @RequestParam TipoDeOperacao tipo,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissao,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competencia,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimento,
                         @RequestParam BigDecimal valor,
                         @RequestParam(required = false) UUID contaId,
                         @RequestParam(required = false) UUID empresaPagadoraId,
                         @RequestParam(required = false) String pessoaRelacionada,
                         @RequestParam(required = false) UUID bemId,
                         @RequestParam(required = false) String solicitante,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        contas.salvarDadosGerais(id, favorecidoId, descricao, tipo, emissao, competencia,
                vencimento, valor, contaId, empresaPagadoraId, pessoaRelacionada, null, bemId,
                solicitante, observacao);
        redirect.addFlashAttribute("aviso", "Conta salva.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/completar")
    public String completar(@PathVariable UUID id, RedirectAttributes redirect) {
        contas.marcarCompleta(id);
        redirect.addFlashAttribute("aviso", "Cadastro marcado como completo.");
        return "redirect:/pagar/" + id;
    }

    // ------------------------------------------------------------ composicao

    @PostMapping("/{id}/itens")
    public String adicionarItem(@PathVariable UUID id,
                                @RequestParam String descricao,
                                @RequestParam(required = false) BigDecimal quantidade,
                                @RequestParam BigDecimal valor,
                                @RequestParam(required = false) UUID naturezaId,
                                @RequestParam(required = false) UUID centroId,
                                @RequestParam(required = false) String pessoa,
                                @RequestParam(required = false) UUID bemId,
                                RedirectAttributes redirect) {
        contas.adicionarItem(id, descricao, quantidade, valor, naturezaId, centroId, pessoa,
                bemId);
        redirect.addFlashAttribute("aviso", "Linha adicionada à composição.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/remover")
    public String removerItem(@PathVariable UUID id, @PathVariable UUID itemId,
                              RedirectAttributes redirect) {
        contas.removerItem(id, itemId);
        redirect.addFlashAttribute("aviso", "Linha retirada da composição.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/ratear")
    public String ratear(@PathVariable UUID id,
                         @RequestParam String descricao,
                         @RequestParam(required = false) UUID naturezaId,
                         @RequestParam String criterio,
                         @RequestParam List<UUID> centros,
                         @RequestParam List<BigDecimal> percentuais,
                         RedirectAttributes redirect) {
        contas.ratear(id, descricao, naturezaId, criterio, centros, percentuais);
        redirect.addFlashAttribute("aviso",
                "Rateio aplicado. A soma fecha com o valor da conta.");
        return "redirect:/pagar/" + id;
    }

    // ------------------------------------------------------------- aprovacao

    @PostMapping("/{id}/pedir-aprovacao")
    public String pedirAprovacao(@PathVariable UUID id, RedirectAttributes redirect) {
        contas.pedirAprovacao(id);
        redirect.addFlashAttribute("aviso", "Enviada para aprovação.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable UUID id, RedirectAttributes redirect) {
        contas.aprovar(id);
        redirect.addFlashAttribute("aviso", "Conta aprovada.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/rejeitar")
    public String rejeitar(@PathVariable UUID id, @RequestParam String motivo,
                           RedirectAttributes redirect) {
        contas.rejeitar(id, motivo);
        redirect.addFlashAttribute("aviso", "Conta rejeitada, com o motivo registrado.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/execucao")
    public String execucao(@PathVariable UUID id, @RequestParam Execucao execucao,
                           RedirectAttributes redirect) {
        contas.marcarExecucao(id, execucao);
        redirect.addFlashAttribute("aviso",
                "Andamento no banco: " + execucao.getRotulo()
                        + ". Isso não é a mesma coisa que pagamento confirmado.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/conciliacao")
    public String conciliacao(@PathVariable UUID id, @RequestParam Conciliacao conciliacao,
                              RedirectAttributes redirect) {
        contas.marcarConciliacao(id, conciliacao);
        redirect.addFlashAttribute("aviso", "Conciliação: " + conciliacao.getRotulo() + ".");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, @RequestParam String motivo,
                           RedirectAttributes redirect) {
        contas.cancelar(id, motivo);
        redirect.addFlashAttribute("aviso", "Conta cancelada, com o motivo registrado.");
        return "redirect:/pagar/" + id;
    }

    // ------------------------------------------------------------ pagamentos

    @PostMapping("/{id}/pagamentos")
    public String pagar(@PathVariable UUID id,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pagoEm,
                        @RequestParam BigDecimal valor,
                        @RequestParam(required = false) UUID contaId,
                        @RequestParam(required = false) String forma,
                        @RequestParam(required = false) BigDecimal juros,
                        @RequestParam(required = false) BigDecimal multa,
                        @RequestParam(required = false) BigDecimal desconto,
                        @RequestParam(required = false) BigDecimal retencao,
                        @RequestParam(required = false) String observacao,
                        RedirectAttributes redirect) {
        contas.pagar(id, pagoEm, valor, contaId, forma, juros, multa, desconto, retencao,
                observacao);
        redirect.addFlashAttribute("aviso", "Pagamento registrado.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/pagamentos/{pagamentoId}/estornar")
    public String estornar(@PathVariable UUID id, @PathVariable UUID pagamentoId,
                           @RequestParam String motivo, RedirectAttributes redirect) {
        contas.estornar(id, pagamentoId, motivo);
        redirect.addFlashAttribute("aviso",
                "Pagamento estornado. A obrigação continua, com o histórico inteiro.");
        return "redirect:/pagar/" + id;
    }

    @PostMapping("/{id}/documentos")
    public String anexar(@PathVariable UUID id,
                         @RequestParam MultipartFile arquivo,
                         @RequestParam(required = false, defaultValue = "OUTRO") String tipo,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        Documento documento = documentos.anexar(arquivo, null, null, null, tipo, null,
                observacao);
        documentos.vincularAObrigacao(documento, id);
        redirect.addFlashAttribute("aviso", "Documento anexado à conta.");
        return "redirect:/pagar/" + id;
    }

    // ------------------------------------------------- parcelas e recorrencia

    @PostMapping("/parcelar")
    public String parcelar(@RequestParam UUID favorecidoId,
                           @RequestParam String descricao,
                           @RequestParam(required = false, defaultValue = "DESPESA") TipoDeOperacao tipo,
                           @RequestParam BigDecimal total,
                           @RequestParam int vezes,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate primeiroVencimento,
                           @RequestParam(required = false) UUID naturezaId,
                           @RequestParam(required = false) UUID centroId,
                           RedirectAttributes redirect) {
        contas.parcelar(favorecidoId, descricao, tipo, total, vezes, primeiroVencimento,
                naturezaId, centroId);
        redirect.addFlashAttribute("aviso",
                vezes + " parcelas lançadas. A soma delas fecha com o total.");
        return "redirect:/pagar";
    }

    @PostMapping("/recorrencias")
    public String cadastrarRecorrencia(@RequestParam String descricao,
                                       @RequestParam UUID favorecidoId,
                                       @RequestParam(required = false) UUID naturezaId,
                                       @RequestParam(required = false) UUID centroId,
                                       @RequestParam(required = false, defaultValue = "DESPESA") TipoDeOperacao tipo,
                                       @RequestParam(required = false, defaultValue = "MENSAL") Periodicidade periodicidade,
                                       @RequestParam(required = false, defaultValue = "10") int diaVencimento,
                                       @RequestParam(required = false) BigDecimal valorPrevisto,
                                       @RequestParam(required = false, defaultValue = "false") boolean valorVariavel,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                       RedirectAttributes redirect) {
        contas.cadastrarRecorrencia(descricao, favorecidoId, naturezaId, centroId, tipo,
                periodicidade, diaVencimento, valorPrevisto, valorVariavel, inicio, null);
        redirect.addFlashAttribute("aviso", "Recorrência cadastrada.");
        return "redirect:/pagar";
    }

    @PostMapping("/gerar-recorrentes")
    public String gerarRecorrentes(@RequestParam String competencia,
                                   RedirectAttributes redirect) {
        int criadas = contas.gerarRecorrentes(YearMonth.parse(competencia));
        redirect.addFlashAttribute("aviso", criadas == 0
                ? "Nenhuma conta nova. As recorrências deste mês já estavam geradas."
                : criadas + " conta(s) gerada(s) pelas recorrências.");
        return "redirect:/pagar";
    }

    // -------------------------------------------------------------- cadastros

    @PostMapping("/favorecidos")
    public String cadastrarFavorecido(@RequestParam String nome,
                                      @RequestParam(required = false) String documento,
                                      @RequestParam(required = false, defaultValue = "EMPRESA") String tipo,
                                      @RequestParam(required = false) String chavePix,
                                      RedirectAttributes redirect) {
        contas.cadastrarFavorecido(nome.trim(), documento, tipo, chavePix);
        redirect.addFlashAttribute("aviso", "Favorecido cadastrado.");
        return "redirect:/pagar";
    }

    @PostMapping("/centros")
    public String cadastrarCentro(@RequestParam String nome,
                                  @RequestParam(required = false) String descricao,
                                  RedirectAttributes redirect) {
        contas.cadastrarCentro(nome.trim(), descricao);
        redirect.addFlashAttribute("aviso", "Centro de custo cadastrado.");
        return "redirect:/pagar";
    }

    @PostMapping("/naturezas")
    public String cadastrarNatureza(@RequestParam String codigo,
                                    @RequestParam String nome,
                                    @RequestParam(required = false, defaultValue = "DESPESA") GrupoDeNatureza grupo,
                                    @RequestParam(required = false) UUID paiId,
                                    RedirectAttributes redirect) {
        contas.cadastrarNatureza(codigo.trim(), nome.trim(), grupo, paiId);
        redirect.addFlashAttribute("aviso", "Natureza cadastrada.");
        return "redirect:/pagar";
    }

    @PostMapping("/contas-financeiras")
    public String cadastrarConta(@RequestParam String nome,
                                 @RequestParam(required = false, defaultValue = "CORRENTE") String tipo,
                                 @RequestParam(required = false) String banco,
                                 @RequestParam(required = false) String agencia,
                                 @RequestParam(required = false) String numero,
                                 @RequestParam String titular,
                                 @RequestParam(required = false) BigDecimal saldoInicial,
                                 RedirectAttributes redirect) {
        contas.cadastrarConta(nome.trim(), tipo, banco, agencia, numero, titular.trim(),
                saldoInicial);
        redirect.addFlashAttribute("aviso", "Conta financeira cadastrada.");
        return "redirect:/pagar";
    }

    @PostMapping("/bens")
    public String cadastrarBem(@RequestParam String numeroPatrimonial,
                               @RequestParam String descricao,
                               @RequestParam(required = false) String tipo,
                               @RequestParam(required = false) UUID centroId,
                               @RequestParam(required = false) String responsavel,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aquisicao,
                               @RequestParam(required = false) BigDecimal valor,
                               RedirectAttributes redirect) {
        contas.cadastrarBem(numeroPatrimonial.trim(), descricao.trim(), tipo, centroId,
                responsavel, aquisicao, valor);
        redirect.addFlashAttribute("aviso", "Bem cadastrado.");
        return "redirect:/pagar";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/pagar";
    }
}
