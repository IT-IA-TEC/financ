package br.com.itia.financeiro.web;

import br.com.itia.financeiro.servico.Analises;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * O livro em forma de análise, e a exportação para a contabilidade.
 */
@Controller
@RequestMapping("/analises")
public class AnaliseController {

    private final Analises analises;
    private final ContextoEmpresa contexto;

    public AnaliseController(Analises analises, ContextoEmpresa contexto) {
        this.analises = analises;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(Model model) {
        var ranking = analises.ranking(10);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("curva", analises.curvaDeRecuperacao(12));
        model.addAttribute("previsao", analises.previsao(6));
        model.addAttribute("ranking", ranking);
        model.addAttribute("concentracao", analises.concentracao(ranking));
        model.addAttribute("hoje", LocalDate.now());
        return "analises";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        String nome = "titulos-" + contexto.exigirEmpresa().getApelido().toLowerCase()
                + "-" + LocalDate.now() + ".csv";
        byte[] conteudo = analises.exportarTitulos().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(conteudo);
    }
}
