package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.servico.Mensagens;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Os modelos de mensagem: o texto que o sistema manda.
 *
 * Os espaços entre chaves são trocados pelos dados de verdade na hora do envio:
 * {cliente}, {valor}, {vencimento}, {chave_pix} e assim por diante.
 */
@Component
public class TelaModelos implements Tela {

    private final Mensagens mensagens;

    private String canalEscolhido = "todos";
    private String momentoEscolhido = "todos";
    private final Janela janela;

    public TelaModelos(Mensagens mensagens, @Lazy Janela janela) {
        this.mensagens = mensagens;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    /** Uma lista de escolha da barra de filtros. */
    private ComboBox<String> filtro(String oQue, String atual, java.util.List<String> opcoes,
                                    java.util.function.Consumer<String> aoEscolher) {
        ComboBox<String> lista = new ComboBox<>();
        lista.getItems().addAll(opcoes);
        lista.getSelectionModel().select(atual);
        lista.setPromptText(oQue);
        lista.setOnAction(acao -> aoEscolher.accept(lista.getValue()));
        return lista;
    }

    @Override
    public Node montar() {
        mensagens.prepararEmpresa();
        List<ModeloDeMensagem> modelos = mensagens.modelos().stream()
                .filter(m -> "todos".equals(canalEscolhido)
                        || canalEscolhido.equals(m.getCanal()))
                .filter(m -> "todos".equals(momentoEscolhido)
                        || momentoEscolhido.equals(m.getMomento()))
                .toList();

        long antes = modelos.stream()
                .filter(m -> "ANTES_VENCER".equals(m.getMomento())).count();
        long depois = modelos.stream()
                .filter(m -> "APOS_VENCER".equals(m.getMomento())).count();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("o que a empresa diz", "Modelos de mensagem",
                "O que entra entre chaves é trocado pelo dado de verdade na hora do envio.",
                Pecas.botao("+ Novo modelo", this::abrirNovoModelo),
                Pecas.botaoVazado("Desligar um modelo", this::abrirDesligar),
                filtro("Canal", canalEscolhido,
                        java.util.List.of("todos", "WHATSAPP", "EMAIL", "SMS"),
                        escolhido -> {
                            canalEscolhido = escolhido;
                            janela.atualizar();
                        }),
                filtro("Momento", momentoEscolhido,
                        java.util.List.of("todos", "ANTES_VENCER", "NO_VENCIMENTO",
                                "APOS_VENCER"),
                        escolhido -> {
                            momentoEscolhido = escolhido;
                            janela.atualizar();
                        }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Modelos", String.valueOf(modelos.size()), "textos cadastrados",
                        true, false),
                Pecas.quadro("Antes de vencer", String.valueOf(antes), "aviso amigável"),
                Pecas.quadro("Depois de vencer", String.valueOf(depois), "cobrança em atraso"),
                Pecas.quadro("Espaços", String.valueOf(ModeloDeMensagem.ESPACOS.size()),
                        "trechos que o sistema preenche"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Modelos"));

        tela.getChildren().add(Pecas.secao("Modelos"));
        tela.getChildren().add(Tabela.de(modelos)
                .coluna("Nome", ModeloDeMensagem::getNome, 1.2)
                .coluna("Canal", ModeloDeMensagem::getCanal, 0.6)
                .coluna("Tom", ModeloDeMensagem::getTomLegivel, 0.8)
                .coluna("Texto", ModeloDeMensagem::getCorpo, 3)
                .comMarca(m -> m.isAtivo() ? "ativo" : "desligado",
                        m -> m.isAtivo() ? "" : "s-cancelado")
                .quandoVazia("Nenhum modelo cadastrado.")
                .montar());

        return tela;
    }

    /** O modelo novo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoModelo() {
        TextField nome = new TextField();
        nome.setPromptText("aviso de vencimento");

        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().addAll("WHATSAPP", "EMAIL", "SMS");
        canal.getSelectionModel().selectFirst();
        canal.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> tom = new ComboBox<>();
        tom.getItems().addAll("LEMBRETE", "COBRANCA", "FORMAL");
        tom.getSelectionModel().selectFirst();
        tom.setMaxWidth(Double.MAX_VALUE);

        TextField assunto = new TextField();
        assunto.setPromptText("usado só no e-mail");

        ComboBox<String> momento = new ComboBox<>();
        momento.getItems().addAll("ANTES_VENCER", "NO_DIA", "APOS_VENCER", "LIVRE");
        momento.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String qual) {
                if (qual == null) {
                    return "";
                }
                return switch (qual) {
                    case "ANTES_VENCER" -> "antes de vencer";
                    case "NO_DIA" -> "no dia";
                    case "APOS_VENCER" -> "depois de vencer";
                    default -> "qualquer momento";
                };
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        momento.getSelectionModel().selectFirst();
        momento.setMaxWidth(Double.MAX_VALUE);

        TextArea corpo = new TextArea();
        corpo.setPromptText("Oi {cliente}, o documento de {valor} vence em {vencimento}. "
                + "A chave para pagamento é {chave_pix}.");
        corpo.setPrefRowCount(4);

        Label ajuda = new Label("Espaços que dá para usar: {cliente}, {valor}, "
                + "{valor_atualizado}, {vencimento}, {dias_de_atraso}, {chave_pix}, "
                + "{identificador}, {empresa}.");
        ajuda.getStyleClass().add("dica");
        ajuda.setWrapText(true);

        HBox campos = new HBox(16,
                Pecas.campo("Nome", nome),
                Pecas.campo("Canal", canal),
                Pecas.campo("Tom", tom),
                Pecas.campo("Assunto (só para e-mail)", assunto),
                Pecas.campo("Quando usar", momento));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Novo modelo de mensagem",
                        "O que entra entre chaves é trocado pelo dado de verdade no envio.")
                .com(campos, Pecas.campo("Texto da mensagem", corpo), ajuda)
                .acao("Guardar o modelo", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()
                            || corpo.getText() == null || corpo.getText().isBlank()) {
                        janela.reclamar("Escreva o nome e o texto da mensagem.");
                        return false;
                    }
                    mensagens.salvarModelo(null, nome.getText().trim(), canal.getValue(),
                            assunto.getText() == null || assunto.getText().isBlank() ? null
                                    : assunto.getText().trim(),
                            corpo.getText().trim(), tom.getValue(), momento.getValue(), true);
                    janela.avisar("Modelo guardado.");
                    janela.ir(TelaModelos.class);
                    return true;
                })
                .abrir();
    }

    /** O desligamento também abre por cima, em vez de ficar solto na página. */
    private void abrirDesligar() {
        List<ModeloDeMensagem> modelos = mensagens.modelos();
        ComboBox<ModeloDeMensagem> qual = new ComboBox<>();
        qual.getItems().addAll(modelos.stream().filter(ModeloDeMensagem::isAtivo).toList());
        qual.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModeloDeMensagem modelo) {
                return modelo == null ? "" : modelo.getNome();
            }

            @Override
            public ModeloDeMensagem fromString(String texto) {
                return null;
            }
        });
        qual.getSelectionModel().selectFirst();
        qual.setMaxWidth(Double.MAX_VALUE);

        HBox linha = new HBox(16, Pecas.campo("Modelo", qual));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Desligar um modelo",
                        "Quem já usou ele continua registrado.")
                .com(linha)
                .acao("Desligar", () -> {
                    if (qual.getValue() == null) {
                        janela.reclamar("Escolha o modelo que vai ser desligado.");
                        return false;
                    }
                    mensagens.desativarModelo(qual.getValue().getId());
                    janela.avisar("Modelo desligado. Quem já usou ele continua registrado.");
                    janela.ir(TelaModelos.class);
                    return true;
                })
                .abrir();
    }
}
