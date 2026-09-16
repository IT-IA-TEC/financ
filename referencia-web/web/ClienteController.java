package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.ColunaCarteira;
import br.com.itia.financeiro.servico.CarteiraServico;
import br.com.itia.financeiro.servico.ColunasServico;
import br.com.itia.financeiro.servico.FiltroCarteira;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final CarteiraServico carteira;
    private final ColunasServico colunas;
    private final ContextoEmpresa contexto;

    public ClienteController(CarteiraServico carteira, ColunasServico colunas,
                             ContextoEmpresa contexto) {
        this.carteira = carteira;
        this.colunas = colunas;
        this.contexto = contexto;
    }

    @GetMapping
    public String listar(@RequestParam org.springframework.util.MultiValueMap<String, String> parametros,
                         Model model) {
        // Uma coluna pode vir com varios valores marcados (a situacao, por
        // exemplo). Junta tudo num texto so, separado por barra.
        java.util.Map<String, String> juntos = new java.util.HashMap<>();
        parametros.forEach((chave, valores) -> juntos.put(chave, String.join("|", valores)));
        FiltroCarteira filtro = FiltroCarteira.de(juntos);
        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("filtro", filtro);
        model.addAttribute("situacoes", ColunaCarteira.situacoes());
        model.addAttribute("linhas", carteira.carteira(filtro));
        java.util.List<ColunaCarteira> escolhidas = colunas.escolhidas();
        model.addAttribute("colunas", escolhidas);
        model.addAttribute("colunasDaPessoa",
                escolhidas.stream().filter(ColunaCarteira::ehDaPessoa).toList());
        model.addAttribute("colunasDaUnidade",
                escolhidas.stream().filter(c -> !c.ehDaPessoa()).toList());
        model.addAttribute("catalogoPessoa", ColunaCarteira.daPessoa());
        model.addAttribute("catalogoUnidade", ColunaCarteira.daUnidade());
        model.addAttribute("hoje", LocalDate.now());
        return "clientes";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome,
                            @RequestParam(required = false) String cpf,
                            @RequestParam(required = false) String whatsapp,
                            @RequestParam(required = false) String telefone,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) java.util.List<String> unidadeNome,
                            @RequestParam(required = false) java.util.List<String> unidadeDocumento,
                            @RequestParam(required = false) java.util.List<String> unidadeCodigo,
                            @RequestParam(required = false, defaultValue = "false") boolean confirmado,
                            @RequestParam(required = false) UUID pagadorExistente,
                            RedirectAttributes redirect) {

        java.util.List<CarteiraServico.UnidadeInformada> unidades =
                montarUnidades(unidadeNome, unidadeDocumento, unidadeCodigo);

        // Somar as unidades a quem ja existe, depois da pessoa confirmar.
        if (confirmado && pagadorExistente != null) {
            carteira.somarUnidades(pagadorExistente, unidades);
            redirect.addFlashAttribute("aviso",
                    "Unidades somadas ao cliente que já existia. Nenhum cadastro repetido foi criado.");
            return "redirect:/clientes";
        }

        // CPF ja cadastrado: em vez de duplicar a pessoa, pergunta antes.
        var jaExiste = carteira.pessoaComEsseCpf(cpf);
        if (jaExiste.isPresent() && !confirmado) {
            redirect.addFlashAttribute("pendente", true);
            redirect.addFlashAttribute("pendenteNome", nome.trim());
            redirect.addFlashAttribute("pendenteCpf", vazioViraNulo(cpf));
            redirect.addFlashAttribute("pendenteWhatsapp", vazioViraNulo(whatsapp));
            redirect.addFlashAttribute("pendenteEmail", vazioViraNulo(email));
            redirect.addFlashAttribute("pendenteUnidades", unidades);
            redirect.addFlashAttribute("existente", jaExiste.get());
            return "redirect:/clientes";
        }

        carteira.cadastrar(nome.trim(), vazioViraNulo(cpf), vazioViraNulo(whatsapp),
                vazioViraNulo(telefone), vazioViraNulo(email), unidades);
        redirect.addFlashAttribute("aviso", "Cliente cadastrado.");
        return "redirect:/clientes";
    }

    /** Junta as tres listas da tela numa lista de unidades. */
    private java.util.List<CarteiraServico.UnidadeInformada> montarUnidades(
            java.util.List<String> nomes, java.util.List<String> documentos,
            java.util.List<String> codigos) {
        java.util.List<CarteiraServico.UnidadeInformada> lista = new java.util.ArrayList<>();
        int quantas = Math.max(nomes == null ? 0 : nomes.size(),
                Math.max(documentos == null ? 0 : documentos.size(),
                         codigos == null ? 0 : codigos.size()));
        for (int i = 0; i < quantas; i++) {
            lista.add(new CarteiraServico.UnidadeInformada(
                    pegar(nomes, i), pegar(documentos, i), pegar(codigos, i)));
        }
        return lista;
    }

    private String pegar(java.util.List<String> lista, int posicao) {
        if (lista == null || posicao >= lista.size()) {
            return null;
        }
        return vazioViraNulo(lista.get(posicao));
    }

    @PostMapping("/colunas")
    public String salvarColunas(@RequestParam(required = false) java.util.List<String> coluna,
                                RedirectAttributes redirect) {
        colunas.salvar(coluna);
        redirect.addFlashAttribute("aviso", "Colunas atualizadas.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/unidades")
    public String adicionarUnidade(@PathVariable UUID id,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String documento,
                                   @RequestParam(required = false) String codigo,
                                   RedirectAttributes redirect) {
        carteira.adicionarUnidade(carteira.pagador(id), nome.trim(), vazioViraNulo(documento),
                vazioViraNulo(codigo), null, null);
        redirect.addFlashAttribute("aviso", "Unidade adicionada.");
        return "redirect:/clientes";
    }

    private String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
