package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Evento;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import br.com.itia.financeiro.servico.PainelServico;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * O painel: a posição de hoje.
 *
 * Todos os números são calculados na hora, a partir do que está lançado.
 * Nenhum é digitado, e é por isso que eles não podem divergir do restante do
 * sistema.
 */
@Component
public class TelaPainel implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final PainelServico painel;
    private final FinanceiroServico financeiro;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    /**
     * Os valores começam escondidos toda vez que o sistema abre: quem quiser
     * ver clica no olho. Serve para abrir o painel na frente de gente.
     */
    private boolean valoresAbertos;

    public TelaPainel(PainelServico painel, FinanceiroServico financeiro,
                      ContextoEmpresa contexto, @Lazy Janela janela) {
        this.painel = painel;
        this.financeiro = financeiro;
        this.contexto = contexto;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "painel";
    }

    @Override
    public Node montar() {
        PainelServico.Resumo resumo = painel.resumoDoMes();
        List<Titulo> emAberto = financeiro.titulosEmAberto();
        List<Evento> eventos = financeiro.ultimosEventos();
        LocalDate hoje = LocalDate.now();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("painel",
                contexto.exigirEmpresa().getNome(),
                "Posição de hoje. Todos os números são calculados na hora, nenhum é digitado.",
                olho(),
                Pecas.botao("Lançar título", () -> janela.reclamar(
                        "O lançamento de título entra na próxima leva da conversão.")),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Em aberto", escondendo(Pecas.dinheiro(resumo.emAberto())),
                        resumo.titulosEmAberto() + " títulos", true, false),
                Pecas.quadro("Vencido", escondendo(Pecas.dinheiro(resumo.vencido())),
                        "já passou do vencimento", false, true),
                Pecas.quadro("Recebido no mês", escondendo(Pecas.dinheiro(resumo.recebidoNoMes())),
                        "do dia 1 até hoje"),
                Pecas.quadro("Clientes ativos", String.valueOf(resumo.clientesAtivos()),
                        "nesta empresa"))));


        tela.getChildren().add(Pecas.secao("A receber"));
        tela.getChildren().add(emAberto.isEmpty()
                ? Pecas.vazio("Nenhum título em aberto. Comece lançando o primeiro.")
                : tabelaDeTitulos(emAberto, hoje));

        if (!eventos.isEmpty()) {
            tela.getChildren().add(Pecas.secao("Últimos movimentos"));
            tela.getChildren().add(listaDeEventos(eventos));
        }
        return tela;
    }

    /** O olho que mostra e esconde os valores do painel. */
    private Node olho() {
        javafx.scene.layout.StackPane botao = new javafx.scene.layout.StackPane(
                valoresAbertos
                        ? br.com.itia.financeiro.marca.Icones.olhoAberto(18,
                                javafx.scene.paint.Color.web("#111114"))
                        : br.com.itia.financeiro.marca.Icones.olhoFechado(18,
                                javafx.scene.paint.Color.web("#111114")));
        botao.getStyleClass().add("olho-dos-valores");
        javafx.scene.control.Tooltip.install(botao, new javafx.scene.control.Tooltip(
                valoresAbertos ? "Esconder os valores" : "Mostrar os valores"));
        botao.setOnMouseClicked(clique -> {
            valoresAbertos = !valoresAbertos;
            janela.atualizar();
        });
        return botao;
    }

    /** Enquanto o olho está fechado, o valor vira pontinhos. */
    private String escondendo(String valor) {
        return valoresAbertos ? valor : "••••••";
    }

    private Node tabelaDeTitulos(List<Titulo> titulos, LocalDate hoje) {
        return Tabela.de(titulos)
                .coluna("Nº", t -> String.valueOf(t.getNumero()), 0.5)
                .coluna("Cliente", t -> t.getCliente().getRazaoSocial(), 2)
                .coluna("Descrição", Titulo::getDescricao, 2)
                .coluna("Vencimento", t -> Pecas.data(t.getVencimento()))
                .valor("Valor", t -> escondendo(Pecas.numero(t.getValor())))
                .valor("Saldo", t -> escondendo(Pecas.numero(t.getSaldo())))
                .comMarca(t -> t.estaVencido(hoje)
                                ? t.diasDeAtraso(hoje) + " dias"
                                : t.getSituacao().name().toLowerCase(),
                        t -> t.estaVencido(hoje) ? "s-vencido"
                                : "s-" + t.getSituacao().name().toLowerCase())
                .quandoVazia("Nenhum título em aberto. Comece lançando o primeiro.")
                .montar();
    }

    private VBox listaDeEventos(List<Evento> eventos) {
        VBox lista = new VBox(0);
        lista.getStyleClass().add("caixa");
        for (Evento evento : eventos.subList(0, Math.min(12, eventos.size()))) {
            Label quando = new Label(evento.getOcorridoEm().format(QUANDO));
            quando.getStyleClass().add("mono");
            quando.setStyle("-fx-text-fill: #2E2E31; -fx-font-size: 12px;");
            quando.setMinWidth(120);

            Label oQue = new Label(evento.getAcao() + " · " + evento.getEntidade());
            oQue.setStyle("-fx-font-size: 13px;");

            HBox linha = new HBox(12, quando, oQue);
            linha.setStyle("-fx-padding: 8 0 8 0; -fx-border-color: transparent transparent "
                    + "#DCDCE0 transparent; -fx-border-width: 0 0 1 0;");
            lista.getChildren().add(linha);
        }
        return lista;
    }
}
