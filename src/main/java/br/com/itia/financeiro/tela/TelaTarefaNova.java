package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Tarefa;
import br.com.itia.financeiro.servico.Cupula;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pedir uma tarefa para alguém, ou abrir para o setor inteiro.
 *
 * Deixar "quem faz" em branco abre a tarefa ao setor: ela fica na fila de todos
 * até alguém pegar.
 */
@Component
public class TelaTarefaNova implements Tela {

    private final Cupula cupula;
    private final TelaTarefas lista;
    private final Janela janela;

    public TelaTarefaNova(Cupula cupula, TelaTarefas lista, @Lazy Janela janela) {
        this.cupula = cupula;
        this.lista = lista;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cupula";
    }

    @Override
    public Node montar() {
        TextField titulo = new TextField();
        titulo.setPromptText("Conferir os comprovantes de setembro");

        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll(Tarefa.TIPOS);
        tipo.getSelectionModel().select(lista.tipoEscolhido());
        tipo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> quemFaz = new ComboBox<>();
        quemFaz.setEditable(true);
        quemFaz.getItems().add("");
        quemFaz.getItems().addAll(cupula.pessoas());
        quemFaz.setMaxWidth(Double.MAX_VALUE);

        TextField setor = new TextField("Financeiro");

        ComboBox<String> prioridade = new ComboBox<>();
        prioridade.getItems().addAll(Tarefa.PRIORIDADES);
        prioridade.getSelectionModel().select("NORMAL");
        prioridade.setMaxWidth(Double.MAX_VALUE);

        DatePicker prazo = new DatePicker();
        prazo.setMaxWidth(Double.MAX_VALUE);

        TextArea detalhe = new TextArea();
        detalhe.setPromptText("o que quem for fazer precisa saber");
        detalhe.setPrefRowCount(3);

        Label dica = new Label("Deixe \"quem faz\" em branco para abrir a tarefa ao setor "
                + "inteiro: ela fica na fila de todos até alguém pegar.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        // Os tickets da demanda: cada linha vira uma tarefa filha, com dono e
        // prazo próprios. Só aparecem quando o tipo escolhido é demanda.
        VBox tickets = new VBox(10);
        java.util.List<HBox> linhasDeTicket = new java.util.ArrayList<>();
        VBox blocoDeTickets = new VBox(10, Pecas.secao("Tickets desta demanda"), tickets,
                new HBox(Pecas.botaoVazado("+ Adicionar", () -> {
                    HBox linha = linhaDeTicket();
                    linhasDeTicket.add(linha);
                    tickets.getChildren().add(linha);
                })));
        HBox primeiro = linhaDeTicket();
        linhasDeTicket.add(primeiro);
        tickets.getChildren().add(primeiro);

        blocoDeTickets.setVisible("DEMANDA".equals(tipo.getValue()));
        blocoDeTickets.setManaged(blocoDeTickets.isVisible());
        tipo.setOnAction(acao -> {
            blocoDeTickets.setVisible("DEMANDA".equals(tipo.getValue()));
            blocoDeTickets.setManaged(blocoDeTickets.isVisible());
        });

        VBox tela = new VBox(16, Pecas.cabecalho("pedir para alguém", "Nova tarefa", null));
        tela.getChildren().add(Pecas.caixa(dica,
                linha(Pecas.campo("O que precisa ser feito", titulo), Pecas.campo("Tipo", tipo)),
                linha(Pecas.campo("Quem faz", quemFaz), Pecas.campo("Setor", setor),
                        Pecas.campo("Para quando", prazo),
                        Pecas.campo("Prioridade", prioridade)),
                Pecas.campo("Detalhe", detalhe),
                blocoDeTickets,
                new HBox(12,
                        Pecas.botao("Criar", () -> criar(tipo.getValue(), titulo.getText(),
                                detalhe.getText(), prioridade.getValue(),
                                quemFaz.getEditor().getText(), setor.getText(),
                                prazo.getValue(), ticketsDigitados(linhasDeTicket))),
                        Pecas.botaoVazado("Cancelar", () -> janela.ir(TelaTarefas.class)))));
        return tela;
    }

    private HBox linha(Node... campos) {
        HBox linha = new HBox(16, campos);
        for (Node campo : campos) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return linha;
    }

    /** Uma linha de ticket: o que é, quem faz, de que setor e para quando. */
    private HBox linhaDeTicket() {
        TextField oQue = new TextField();
        oQue.setPromptText("o que precisa ser feito neste ticket");

        ComboBox<String> quem = new ComboBox<>();
        quem.setEditable(true);
        quem.getItems().add("");
        quem.getItems().addAll(cupula.pessoas());
        quem.setMaxWidth(Double.MAX_VALUE);

        TextField setor = new TextField();
        setor.setPromptText("herda o setor da demanda");

        DatePicker prazo = new DatePicker();
        prazo.setMaxWidth(Double.MAX_VALUE);

        return linha(Pecas.campo("O que é", oQue), Pecas.campo("Quem faz", quem),
                Pecas.campo("Setor", setor), Pecas.campo("Para quando", prazo));
    }

    private List<Cupula.Ticket> ticketsDigitados(java.util.List<HBox> linhas) {
        List<Cupula.Ticket> tickets = new java.util.ArrayList<>();
        for (HBox linha : linhas) {
            String oQue = textoDoCampo(linha, 0);
            String quem = escolhaDoCampo(linha, 1);
            String setor = textoDoCampo(linha, 2);
            java.time.LocalDate prazo = dataDoCampo(linha, 3);
            if (oQue != null) {
                tickets.add(new Cupula.Ticket(oQue, quem, setor, prazo));
            }
        }
        return tickets;
    }

    private String textoDoCampo(HBox linha, int posicao) {
        VBox caixa = (VBox) linha.getChildren().get(posicao);
        TextField campo = (TextField) caixa.getChildren().get(1);
        return campo.getText() == null || campo.getText().isBlank() ? null
                : campo.getText().trim();
    }

    @SuppressWarnings("unchecked")
    private String escolhaDoCampo(HBox linha, int posicao) {
        VBox caixa = (VBox) linha.getChildren().get(posicao);
        ComboBox<String> campo = (ComboBox<String>) caixa.getChildren().get(1);
        String texto = campo.getEditor().getText();
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private java.time.LocalDate dataDoCampo(HBox linha, int posicao) {
        VBox caixa = (VBox) linha.getChildren().get(posicao);
        DatePicker campo = (DatePicker) caixa.getChildren().get(1);
        return campo.getValue();
    }

    private void criar(String tipo, String titulo, String detalhe, String prioridade,
                       String quemFaz, String setor, java.time.LocalDate prazo,
                       List<Cupula.Ticket> tickets) {
        if (titulo == null || titulo.isBlank()) {
            janela.reclamar("Escreva o que precisa ser feito.");
            return;
        }
        if ("DEMANDA".equals(tipo)) {
            Tarefa demanda = cupula.criarDemanda(titulo.trim(),
                    detalhe == null || detalhe.isBlank() ? null : detalhe.trim(),
                    prioridade, setor == null || setor.isBlank() ? null : setor.trim(),
                    prazo, tickets, List.of());
            janela.avisar("Demanda " + demanda.getCodigo() + " aberta com "
                    + tickets.size() + " ticket(s).");
            lista.abrirEsta(demanda.getId());
            janela.ir(TelaTarefas.class);
            return;
        }
        Tarefa nova = cupula.criar(tipo, titulo.trim(),
                detalhe == null || detalhe.isBlank() ? null : detalhe.trim(),
                prioridade,
                quemFaz == null || quemFaz.isBlank() ? null : quemFaz.trim(),
                setor == null || setor.isBlank() ? null : setor.trim(),
                prazo, null, List.of());
        janela.avisar("Pronto: " + nova.getCodigo() + " · " + nova.getRotuloDoTipo()
                + (nova.doSetor() ? " na fila do setor " + nova.getSetor() + "."
                : " com " + nova.getResponsavel() + "."));
        lista.abrirEsta(nova.getId());
        janela.ir(TelaTarefas.class);
    }
}
