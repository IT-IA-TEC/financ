package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.ServicoRealizado;
import br.com.itia.financeiro.dominio.TratamentoDoAtendimento;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.DocumentoServico;
import br.com.itia.financeiro.servico.ServicosRealizados;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A aba de serviços realizados, dentro do módulo Pacotes e serviços.
 */
@Controller
@RequestMapping("/realizados")
public class RealizadoController {

    private final ServicosRealizados realizados;
    private final DocumentoServico documentos;
    private final ContextoEmpresa contexto;

    public RealizadoController(ServicosRealizados realizados, DocumentoServico documentos,
                               ContextoEmpresa contexto) {
        this.realizados = realizados;
        this.documentos = documentos;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) UUID cliente,
                         @RequestParam(required = false) UUID servico,
                         @RequestParam(required = false) String tratamento,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                         Model model) {
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("atendimentos", realizados.lista(cliente, servico, tratamento, de, ate));
        model.addAttribute("clientes", realizados.clientes());
        model.addAttribute("unidades", realizados.unidadesDaEmpresa());
        model.addAttribute("servicos", realizados.servicosDisponiveis());
        model.addAttribute("tratamentos", TratamentoDoAtendimento.values());
        model.addAttribute("clienteEscolhido", cliente);
        model.addAttribute("servicoEscolhido", servico);
        model.addAttribute("tratamentoEscolhido", tratamento);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        model.addAttribute("hoje", LocalDate.now());
        return "realizados";
    }

    @PostMapping
    public String registrar(@RequestParam UUID pagadorId,
                            @RequestParam(required = false) UUID unidadeId,
                            @RequestParam UUID servicoId,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate realizadoEm,
                            @RequestParam(required = false) BigDecimal quantidade,
                            @RequestParam(required = false) String responsavel,
                            @RequestParam(required = false, defaultValue = "PELO_PACOTE") String tratamento,
                            @RequestParam(required = false) BigDecimal valorCobrado,
                            @RequestParam(required = false) BigDecimal desconto,
                            @RequestParam(required = false) String observacao,
                            @RequestParam(required = false) List<UUID> itens,
                            RedirectAttributes redirect) {
        ServicoRealizado novo = realizados.registrar(pagadorId, unidadeId, servicoId, realizadoEm,
                quantidade, responsavel, tratamento, valorCobrado, desconto, observacao, itens);
        redirect.addFlashAttribute("aviso",
                "Atendimento registrado como " + novo.getTratamento().getRotulo().toLowerCase() + ".");
        return "redirect:/realizados/" + novo.getId();
    }

    @GetMapping("/{id}")
    public String abrir(@PathVariable UUID id, Model model) {
        ServicoRealizado atendimento = realizados.atendimento(id);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("atendimento", atendimento);
        model.addAttribute("comprovantes", realizados.comprovantesDe(id));
        model.addAttribute("clientes", realizados.clientes());
        model.addAttribute("unidades", realizados.unidadesDaEmpresa());
        model.addAttribute("servicos", realizados.servicosDisponiveis());
        model.addAttribute("tratamentos", TratamentoDoAtendimento.values());
        model.addAttribute("cobertura", realizados.conferir(
                atendimento.getPagador().getId(), atendimento.getServico().getId(),
                atendimento.getRealizadoEm(), atendimento.getQuantidade(), id));
        model.addAttribute("tiposDeDocumento", Documento.TIPOS);
        model.addAttribute("hoje", LocalDate.now());
        return "realizado";
    }

    @PostMapping("/{id}")
    public String salvar(@PathVariable UUID id,
                         @RequestParam UUID pagadorId,
                         @RequestParam(required = false) UUID unidadeId,
                         @RequestParam UUID servicoId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate realizadoEm,
                         @RequestParam(required = false) BigDecimal quantidade,
                         @RequestParam(required = false) String responsavel,
                         @RequestParam(required = false, defaultValue = "PELO_PACOTE") String tratamento,
                         @RequestParam(required = false) BigDecimal valorCobrado,
                         @RequestParam(required = false) BigDecimal desconto,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        realizados.salvar(id, pagadorId, unidadeId, servicoId, realizadoEm, quantidade,
                responsavel, tratamento, valorCobrado, desconto, observacao);
        redirect.addFlashAttribute("aviso", "Atendimento salvo.");
        return "redirect:/realizados/" + id;
    }

    @PostMapping("/{id}/itens")
    public String adicionarItem(@PathVariable UUID id,
                                @RequestParam UUID itemId,
                                @RequestParam(required = false) BigDecimal quantidade,
                                @RequestParam(required = false) String observacao,
                                RedirectAttributes redirect) {
        realizados.adicionarItem(id, itemId, quantidade, observacao);
        redirect.addFlashAttribute("aviso", "Item executado registrado.");
        return "redirect:/realizados/" + id;
    }

    @PostMapping("/{id}/comprovantes")
    public String anexar(@PathVariable UUID id,
                         @RequestParam MultipartFile arquivo,
                         @RequestParam(required = false, defaultValue = "COMPROVANTE DE PAGAMENTO") String tipo,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        ServicoRealizado atendimento = realizados.atendimento(id);
        Documento documento = documentos.anexar(arquivo, atendimento.getPagador().getId(),
                atendimento.getUnidade() == null ? null : atendimento.getUnidade().getId(),
                null, tipo, null, observacao);
        realizados.guardarComprovante(id, documento);
        redirect.addFlashAttribute("aviso", "Comprovante anexado.");
        return "redirect:/realizados/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, RedirectAttributes redirect) {
        realizados.cancelar(id);
        redirect.addFlashAttribute("aviso",
                "Atendimento cancelado. Ele deixa de contar contra o limite do pacote.");
        return "redirect:/realizados/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable UUID id, RedirectAttributes redirect) {
        realizados.reabrir(id);
        redirect.addFlashAttribute("aviso", "Atendimento reaberto.");
        return "redirect:/realizados/" + id;
    }

    /** O aviso que a tela mostra antes de gravar: entra no pacote ou vira cobrança. */
    @GetMapping("/cobertura")
    @ResponseBody
    public Map<String, Object> cobertura(@RequestParam UUID pagadorId,
                                         @RequestParam UUID servicoId,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
                                         @RequestParam(required = false) BigDecimal quantidade) {
        ServicosRealizados.Cobertura cobertura = realizados.conferir(pagadorId, servicoId, dia,
                quantidade, null);
        return Map.of(
                "temPacote", cobertura.isTemPacote(),
                "explicacao", cobertura.getExplicacao(),
                "tratamento", cobertura.getTratamentoSugeridoNome(),
                "precisaDeAvaliacao", cobertura.isPrecisaDeAvaliacao());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/realizados";
    }
}
