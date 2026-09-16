package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Esteira;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A esteira de inadimplência: o quadro por faixa de atraso.
 */
@Controller
@RequestMapping("/esteira")
public class EsteiraController {

    private final Esteira esteira;
    private final ContextoEmpresa contexto;

    public EsteiraController(Esteira esteira, ContextoEmpresa contexto) {
        this.esteira = esteira;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        LocalDate hoje = LocalDate.now();
        Map<Esteira.Faixa, List<Esteira.NaEsteira>> quadro = esteira.quadro(hoje);

        Map<String, BigDecimal> totais = new LinkedHashMap<>();
        int quantosVencidos = 0;
        BigDecimal vencido = BigDecimal.ZERO;
        for (Map.Entry<Esteira.Faixa, List<Esteira.NaEsteira>> coluna : quadro.entrySet()) {
            BigDecimal soma = esteira.totalDe(coluna.getValue());
            totais.put(coluna.getKey().chave(), soma);
            if (coluna.getKey().de() > 0) {
                quantosVencidos += coluna.getValue().size();
                vencido = vencido.add(soma);
            }
        }

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("quadro", quadro);
        model.addAttribute("totais", totais);
        model.addAttribute("vencido", vencido);
        model.addAttribute("quantosVencidos", quantosVencidos);
        model.addAttribute("paraHoje", esteira.paraHoje(hoje));
        model.addAttribute("nomes", esteira.nomes());
        model.addAttribute("situacoes", CasoDeCobranca.SITUACOES);
        model.addAttribute("hoje", hoje);
        return "esteira";
    }

    @PostMapping("/{unidadeId}")
    public String anotar(@PathVariable UUID unidadeId,
                         @RequestParam(required = false) String situacao,
                         @RequestParam(required = false) String responsavel,
                         @RequestParam(required = false) String proximaAcao,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate proximaData,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirect) {
        esteira.anotar(unidadeId, situacao, responsavel, proximaAcao, proximaData, observacao);
        redirect.addFlashAttribute("aviso", "Caso atualizado.");
        return "redirect:/esteira";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/esteira";
    }
}
