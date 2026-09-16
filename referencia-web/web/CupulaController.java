package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Tarefa;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Cupula;
import br.com.itia.financeiro.servico.TarefasDeFora;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Cúpula: as tarefas de quem trabalha aqui.
 */
@Controller
@RequestMapping("/tarefas")
public class CupulaController {

    private final Cupula cupula;
    private final TarefasDeFora deFora;
    private final ContextoEmpresa contexto;

    public CupulaController(Cupula cupula, TarefasDeFora deFora, ContextoEmpresa contexto) {
        this.cupula = cupula;
        this.deFora = deFora;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(@RequestParam(required = false, defaultValue = "central") String aba,
                       @RequestParam(required = false) UUID tarefa,
                       Model model) {
        LocalDate hoje = LocalDate.now();

        List<Tarefa> lista = switch (aba) {
            case "recebidas" -> cupula.recebidas();
            case "enviadas" -> cupula.enviadas();
            case "faco_parte" -> cupula.facoParte();
            case "do_setor" -> cupula.doSetor();
            default -> cupula.central();
        };

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("aba", aba);
        model.addAttribute("lista", lista);
        model.addAttribute("colunas", cupula.emColunas(lista));
        model.addAttribute("resumo", cupula.resumo(hoje));
        model.addAttribute("pessoas", cupula.pessoas());
        model.addAttribute("prioridades", Tarefa.PRIORIDADES);
        model.addAttribute("tipos", Tarefa.TIPOS);
        model.addAttribute("fontes", deFora.todas());
        model.addAttribute("semDono", cupula.semDono());
        model.addAttribute("hoje", hoje);
        model.addAttribute("eu", contexto.autor());

        if (tarefa != null) {
            Tarefa aberta = cupula.abrir(tarefa);
            model.addAttribute("tarefa", aberta);
            model.addAttribute("fio", cupula.fioDe(tarefa));
            model.addAttribute("filhas", cupula.filhasDe(tarefa));
            model.addAttribute("acompanhantes", cupula.quemAcompanha(tarefa));
        }
        return "tarefas";
    }

    @PostMapping
    public String criar(@RequestParam(required = false, defaultValue = "TAREFA") String tipo,
                        @RequestParam String titulo,
                        @RequestParam(required = false) String descricao,
                        @RequestParam(required = false, defaultValue = "NORMAL") String prioridade,
                        @RequestParam(required = false) String responsavel,
                        @RequestParam(required = false) String setor,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
                        @RequestParam(required = false) UUID paiId,
                        @RequestParam(required = false) List<String> acompanhantes,
                        @RequestParam(required = false) List<String> ticketTitulo,
                        @RequestParam(required = false) List<String> ticketResponsavel,
                        @RequestParam(required = false) List<String> ticketSetor,
                        @RequestParam(required = false) List<String> ticketPrazo,
                        RedirectAttributes redirect) {
        if ("DEMANDA".equals(tipo)) {
            Tarefa demanda = cupula.criarDemanda(titulo, descricao, prioridade, setor, prazo,
                    tickets(ticketTitulo, ticketResponsavel, ticketSetor, ticketPrazo),
                    acompanhantes);
            redirect.addFlashAttribute("aviso", "Demanda " + demanda.getCodigo()
                    + " criada, com os tickets abertos.");
            return "redirect:/tarefas?aba=enviadas&tarefa=" + demanda.getId();
        }

        Tarefa nova = cupula.criar(tipo, titulo, descricao, prioridade, responsavel, setor, prazo,
                paiId, acompanhantes);
        redirect.addFlashAttribute("aviso", "Pronto: " + nova.getCodigo() + " · "
                + nova.getRotuloDoTipo() + (nova.doSetor()
                ? " na fila do setor " + nova.getSetor() + "."
                : " com " + nova.getResponsavel() + "."));
        return "redirect:/tarefas?aba=enviadas&tarefa=" + nova.getId();
    }

    /** Junta as linhas soltas do formulário da demanda em um ticket cada. */
    private List<Cupula.Ticket> tickets(List<String> titulos, List<String> responsaveis,
                                        List<String> setores, List<String> prazos) {
        if (titulos == null) {
            return List.of();
        }
        List<Cupula.Ticket> lista = new ArrayList<>();
        for (int i = 0; i < titulos.size(); i++) {
            lista.add(new Cupula.Ticket(titulos.get(i), item(responsaveis, i), item(setores, i),
                    data(item(prazos, i))));
        }
        return lista;
    }

    private String item(List<String> lista, int posicao) {
        return lista == null || posicao >= lista.size() ? null : lista.get(posicao);
    }

    private LocalDate data(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (RuntimeException naoEData) {
            throw new IllegalArgumentException("Data do ticket em formato inválido: " + texto);
        }
    }

    @PostMapping("/{id}/pegar")
    public String pegar(@PathVariable UUID id, RedirectAttributes redirect) {
        cupula.pegarParaMim(id);
        redirect.addFlashAttribute("aviso", "Tarefa é sua agora.");
        return "redirect:/tarefas?aba=recebidas&tarefa=" + id;
    }

    @PostMapping("/{id}/comecar")
    public String comecar(@PathVariable UUID id, RedirectAttributes redirect) {
        cupula.comecar(id);
        redirect.addFlashAttribute("aviso", "Tarefa em andamento.");
        return "redirect:/tarefas?aba=recebidas&tarefa=" + id;
    }

    @PostMapping("/{id}/terminar")
    public String terminar(@PathVariable UUID id,
                           @RequestParam(required = false) String oQueFoiFeito,
                           RedirectAttributes redirect) {
        cupula.terminar(id, oQueFoiFeito);
        redirect.addFlashAttribute("aviso",
                "Marcada como terminada. Quem pediu vai conferir antes de fechar.");
        return "redirect:/tarefas?aba=recebidas&tarefa=" + id;
    }

    @PostMapping("/{id}/aceitar")
    public String aceitar(@PathVariable UUID id, RedirectAttributes redirect) {
        cupula.aceitar(id);
        redirect.addFlashAttribute("aviso", "Tarefa concluída.");
        return "redirect:/tarefas?aba=enviadas&tarefa=" + id;
    }

    @PostMapping("/{id}/refazer")
    public String refazer(@PathVariable UUID id, @RequestParam String motivo,
                          RedirectAttributes redirect) {
        cupula.mandarRefazer(id, motivo);
        redirect.addFlashAttribute("aviso", "Voltou para quem fez, com o motivo escrito.");
        return "redirect:/tarefas?aba=enviadas&tarefa=" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, @RequestParam String motivo,
                           RedirectAttributes redirect) {
        cupula.cancelar(id, motivo);
        redirect.addFlashAttribute("aviso", "Tarefa cancelada, com o motivo guardado.");
        return "redirect:/tarefas?tarefa=" + id;
    }

    @PostMapping("/{id}/responsavel")
    public String responsavel(@PathVariable UUID id,
                              @RequestParam(required = false) String paraQuem,
                              RedirectAttributes redirect) {
        cupula.trocarResponsavel(id, paraQuem);
        redirect.addFlashAttribute("aviso", "Responsável trocado.");
        return "redirect:/tarefas?tarefa=" + id;
    }

    @PostMapping("/{id}/prazo")
    public String prazo(@PathVariable UUID id,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate novo,
                        RedirectAttributes redirect) {
        cupula.mudarPrazo(id, novo);
        redirect.addFlashAttribute("aviso", "Prazo alterado.");
        return "redirect:/tarefas?tarefa=" + id;
    }

    @PostMapping("/{id}/cobrar")
    public String cobrar(@PathVariable UUID id, @RequestParam(required = false) String texto,
                         RedirectAttributes redirect) {
        cupula.cobrar(id, texto);
        redirect.addFlashAttribute("aviso", "Cobrança registrada no fio da tarefa.");
        return "redirect:/tarefas?aba=enviadas&tarefa=" + id;
    }

    @PostMapping("/{id}/comentar")
    public String comentar(@PathVariable UUID id, @RequestParam String texto,
                           @RequestParam(required = false, defaultValue = "COMENTARIO")
                           String tipo, RedirectAttributes redirect) {
        cupula.comentar(id, texto, tipo);
        redirect.addFlashAttribute("aviso", "Escrito no fio da tarefa.");
        return "redirect:/tarefas?tarefa=" + id;
    }

    @PostMapping("/{id}/acompanhar")
    public String acompanhar(@PathVariable UUID id, @RequestParam String quem,
                             RedirectAttributes redirect) {
        cupula.acrescentarAcompanhante(id, quem);
        redirect.addFlashAttribute("aviso", quem + " passou a acompanhar esta tarefa.");
        return "redirect:/tarefas?tarefa=" + id;
    }

    /** Puxa agora as tarefas de uma fonte de fora. */
    @PostMapping("/fontes/{id}/puxar")
    public String puxar(@PathVariable UUID id, RedirectAttributes redirect) {
        TarefasDeFora.Puxada puxada = deFora.puxar(id);
        if (puxada.deuCerto()) {
            redirect.addFlashAttribute("aviso", "Busca feita: " + puxada.resumo() + ".");
        } else {
            redirect.addFlashAttribute("erro", puxada.erro());
        }
        return "redirect:/tarefas";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/tarefas";
    }
}
