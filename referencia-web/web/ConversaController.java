package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.PermissaoDaIa;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.Conversas;
import br.com.itia.financeiro.servico.FerramentasDaMesa;
import br.com.itia.financeiro.servico.FichaDoCaso;
import br.com.itia.financeiro.servico.Inteligencia;
import br.com.itia.financeiro.servico.MesaDeCobranca;
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
import java.util.List;
import java.util.UUID;

/**
 * A mesa de cobrança: a fila, o caso e o cliente numa tela só.
 */
@Controller
@RequestMapping("/conversas")
public class ConversaController {

    private final Conversas conversas;
    private final MesaDeCobranca mesa;
    private final FerramentasDaMesa ferramentas;
    private final FichaDoCaso ficha;
    private final Inteligencia inteligencia;
    private final ContextoEmpresa contexto;

    public ConversaController(Conversas conversas, MesaDeCobranca mesa,
                              FerramentasDaMesa ferramentas, FichaDoCaso ficha,
                              Inteligencia inteligencia, ContextoEmpresa contexto) {
        this.conversas = conversas;
        this.mesa = mesa;
        this.ferramentas = ferramentas;
        this.ficha = ficha;
        this.inteligencia = inteligencia;
        this.contexto = contexto;
    }

    @GetMapping
    public String tela(@RequestParam(required = false) UUID cliente,
                       @RequestParam(required = false, defaultValue = "dia") String recorte,
                       @RequestParam(required = false) String busca,
                       @RequestParam(required = false, defaultValue = "resumo") String aba,
                       Model model) {
        LocalDate hoje = LocalDate.now();
        List<MesaDeCobranca.NaFila> toda = mesa.fila(hoje);
        List<MesaDeCobranca.NaFila> fila = mesa.peneirar(toda, recorte, busca);

        UUID escolhido = cliente;
        if (escolhido == null && !fila.isEmpty()) {
            escolhido = fila.get(0).unidadeId();
        }

        model.addAttribute("empresa", contexto.exigirEmpresa());
        model.addAttribute("fila", fila);
        model.addAttribute("recortes", mesa.recortes(toda));
        model.addAttribute("recorte", recorte);
        model.addAttribute("busca", busca);
        model.addAttribute("aba", aba);
        model.addAttribute("semDono", conversas.semDono());
        model.addAttribute("canalLigado", conversas.canalDeWhatsApp().isPresent());
        model.addAttribute("situacoes", CasoDeCobranca.SITUACOES);
        model.addAttribute("clientes", conversas.todosOsClientes());
        model.addAttribute("hoje", hoje);
        model.addAttribute("eu", contexto.autor());

        if (escolhido != null) {
            List<Titulo> emAberto = ferramentas.emAberto(escolhido);
            model.addAttribute("escolhido", escolhido);
            model.addAttribute("cliente", conversas.cliente(escolhido));
            model.addAttribute("mensagens", conversas.conversa(escolhido));
            model.addAttribute("titulos", emAberto);
            model.addAttribute("emAberto", emAberto.stream().map(Titulo::getSaldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            model.addAttribute("linhasDaFicha", ficha.de(escolhido, hoje));
            model.addAttribute("copiloto", mesa.copiloto(escolhido, hoje));
            model.addAttribute("comandos", ferramentas.comandos(escolhido, hoje));
            model.addAttribute("historia", mesa.historia(escolhido));
            model.addAttribute("pagamentos", ferramentas.pagamentos(escolhido));
            model.addAttribute("acordos", ferramentas.acordosDe(escolhido));
            model.addAttribute("unidades", ferramentas.unidadesDoMesmoDono(escolhido));
            model.addAttribute("caso", inteligencia.caso(escolhido));
            model.addAttribute("iaPronta", inteligencia.prontoParaTrabalhar());
        }
        return "conversas";
    }

    /** Link antigo, de quando a conversa tinha tela própria. */
    @GetMapping("/{unidadeId}")
    public String daConversa(@PathVariable UUID unidadeId) {
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    // ------------------------------------------------------------------ falar

    @PostMapping("/nova")
    public String nova(@RequestParam(required = false) UUID unidadeId,
                       @RequestParam(required = false) String numero,
                       @RequestParam String texto,
                       RedirectAttributes redirect) {
        var mensagem = conversas.comecar(unidadeId, numero, texto);
        redirect.addFlashAttribute("aviso", switch (mensagem.getSituacao()) {
            case "NA_FILA" -> "Conversa começada. A mensagem está na fila, com o texto pronto.";
            case "FALHOU" -> "Conversa começada, mas o canal não aceitou a mensagem: "
                    + mensagem.getMotivo();
            default -> "Conversa começada e mensagem enviada.";
        });
        return unidadeId == null ? "redirect:/conversas"
                : "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/responder")
    public String responder(@PathVariable UUID unidadeId,
                            @RequestParam String texto,
                            @RequestParam(required = false) String destino,
                            RedirectAttributes redirect) {
        var mensagem = conversas.responder(unidadeId, texto, destino);
        redirect.addFlashAttribute("aviso", mensagem.naFila()
                ? "Mensagem guardada na fila. O canal do WhatsApp ainda não está ligado."
                : ("FALHOU".equals(mensagem.getSituacao())
                ? "O canal não aceitou a mensagem: " + mensagem.getMotivo()
                : "Mensagem enviada."));
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/anotar")
    public String anotar(@PathVariable UUID unidadeId, @RequestParam String texto,
                         RedirectAttributes redirect) {
        conversas.anotarResposta(unidadeId, texto);
        redirect.addFlashAttribute("aviso", "Resposta do cliente registrada na conversa.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    // ------------------------------------------------------------- ferramentas

    @PostMapping("/{unidadeId}/assumir")
    public String assumir(@PathVariable UUID unidadeId, RedirectAttributes redirect) {
        mesa.assumir(unidadeId);
        redirect.addFlashAttribute("aviso", "Você assumiu este atendimento.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/promessa")
    public String promessa(@PathVariable UUID unidadeId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                           LocalDate quando,
                           @RequestParam(required = false) BigDecimal valor,
                           RedirectAttributes redirect) {
        mesa.anotarPromessa(unidadeId, quando, valor);
        redirect.addFlashAttribute("aviso",
                "Promessa anotada. A cobrança automática segura até essa data.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/ligacao")
    public String ligacao(@PathVariable UUID unidadeId,
                          @RequestParam(required = false) String texto,
                          RedirectAttributes redirect) {
        ferramentas.registrarLigacao(unidadeId, texto);
        redirect.addFlashAttribute("aviso", "Ligação registrada na história do cliente.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/nota")
    public String nota(@PathVariable UUID unidadeId, @RequestParam String texto,
                       RedirectAttributes redirect) {
        ferramentas.notaInterna(unidadeId, texto);
        redirect.addFlashAttribute("aviso",
                "Nota guardada. Ela fica só para a equipe, o cliente não vê.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/retorno")
    public String retorno(@PathVariable UUID unidadeId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                          LocalDate quando,
                          @RequestParam(required = false) String oQueFazer,
                          RedirectAttributes redirect) {
        ferramentas.agendarRetorno(unidadeId, quando, oQueFazer);
        redirect.addFlashAttribute("aviso", "Retorno agendado. Ele aparece na esteira no dia.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/pausar")
    public String pausar(@PathVariable UUID unidadeId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                         LocalDate ate,
                         @RequestParam(required = false) String motivo,
                         RedirectAttributes redirect) {
        ferramentas.pausarRegua(unidadeId, ate, motivo);
        redirect.addFlashAttribute("aviso",
                "Régua pausada. Nenhuma cobrança automática sai para este cliente até lá.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/religar")
    public String religar(@PathVariable UUID unidadeId, RedirectAttributes redirect) {
        ferramentas.voltarACobrar(unidadeId);
        redirect.addFlashAttribute("aviso", "Régua religada para este cliente.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/bloqueio")
    public String bloqueio(@PathVariable UUID unidadeId,
                           @RequestParam(required = false) String motivo,
                           RedirectAttributes redirect) {
        ferramentas.pedirBloqueio(unidadeId, motivo);
        redirect.addFlashAttribute("aviso",
                "Pedido de bloqueio registrado, com o motivo e quem pediu.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/transferir")
    public String transferir(@PathVariable UUID unidadeId, @RequestParam String paraQuem,
                             RedirectAttributes redirect) {
        ferramentas.transferir(unidadeId, paraQuem);
        redirect.addFlashAttribute("aviso", "Caso passado para " + paraQuem + ".");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/simular")
    public String simular(@PathVariable UUID unidadeId,
                          @RequestParam(defaultValue = "3") int vezes,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate primeiro,
                          RedirectAttributes redirect) {
        redirect.addFlashAttribute("rascunho",
                ferramentas.simular(unidadeId, vezes, primeiro, LocalDate.now()));
        redirect.addFlashAttribute("aviso",
                "Simulação pronta na caixa de escrever. Nada foi criado ainda.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{unidadeId}/finalizar")
    public String finalizar(@PathVariable UUID unidadeId,
                            @RequestParam String situacao,
                            @RequestParam(required = false) String combinado,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate proximaData,
                            @RequestParam(required = false) String proximaAcao,
                            RedirectAttributes redirect) {
        mesa.finalizar(unidadeId, situacao, combinado, proximaData, proximaAcao);
        redirect.addFlashAttribute("aviso", "Atendimento fechado e registrado na história.");
        return "redirect:/conversas";
    }

    /** Pede ao modelo uma leitura do caso, se ele estiver ligado e com chave. */
    @PostMapping("/{unidadeId}/ia")
    public String pedirIa(@PathVariable UUID unidadeId,
                          @RequestParam(required = false) String oQuePrecisa,
                          RedirectAttributes redirect) {
        Inteligencia.Resposta resposta = inteligencia.pedirLeitura(unidadeId, oQuePrecisa);
        if (resposta.deuCerto()) {
            redirect.addFlashAttribute("leituraDaIa", resposta.texto());
            String recusa = inteligencia.podeFazer(PermissaoDaIa.SUGERIR_RESPOSTA, unidadeId,
                    null);
            if (recusa == null) {
                redirect.addFlashAttribute("rascunho", resposta.texto());
            }
            redirect.addFlashAttribute("aviso", "O modelo leu o caso. Confira antes de usar.");
        } else {
            redirect.addFlashAttribute("erro", resposta.recusa());
        }
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @PostMapping("/{mensagemId}/dono")
    public String darDono(@PathVariable UUID mensagemId, @RequestParam UUID unidadeId,
                          RedirectAttributes redirect) {
        conversas.darDono(mensagemId, unidadeId);
        redirect.addFlashAttribute("aviso", "Mensagem ligada ao cliente.");
        return "redirect:/conversas?cliente=" + unidadeId;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String problema(RuntimeException erro, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", erro.getMessage());
        return "redirect:/conversas";
    }
}
