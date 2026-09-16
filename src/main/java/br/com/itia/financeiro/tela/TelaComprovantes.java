package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Comprovante;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.Comprovantes;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Comprovantes: o que o cliente mandou dizendo que pagou.
 *
 * O sistema lê o valor, a data e o destino, mas a baixa só acontece depois de
 * alguém conferir. Comprovante com destino de outra empresa é recusado.
 */
@Component
public class TelaComprovantes implements Tela {

    private final Comprovantes comprovantes;
    private final ContextoEmpresa contexto;
    private final Janela janela;

    private UUID aberto;

    public TelaComprovantes(Comprovantes comprovantes, ContextoEmpresa contexto,
                            @Lazy Janela janela) {
        this.comprovantes = comprovantes;
        this.contexto = contexto;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "titulos";
    }

    @Override
    public Node montar() {
        List<Comprovante> fila = comprovantes.fila();

        int emAberto = comprovantes.titulosEmAberto().size();
        String chavePix = contexto.exigirEmpresa().getChavePix();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("prova de pagamento", "Comprovantes",
                "O cliente manda, o sistema lê, e alguém confere antes de dar baixa.",
                Pecas.botao("+ Receber comprovante", this::abrirReceber),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Na fila", String.valueOf(comprovantes.esperando().size()),
                        "esperando conferência", true, false),
                Pecas.quadro("Recebidos", String.valueOf(fila.size()), "no total"),
                Pecas.quadro("Títulos em aberto", String.valueOf(emAberto),
                        "podem receber a baixa"),
                Pecas.quadro("Destino conferido", chavePix == null ? "falta" : "sim",
                        chavePix == null ? "cadastre a chave PIX"
                                : "pela chave PIX da empresa"))));

        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Contas a receber", () -> janela.ir(TelaTitulos.class));
        partes.put("Comprovantes", () -> janela.ir(TelaComprovantes.class));
        partes.put("Análises", () -> janela.ir(TelaAnalises.class));
        tela.getChildren().add(Pecas.abas("Comprovantes", partes));

        tela.getChildren().add(Pecas.secao("Fila de comprovantes"));
        tela.getChildren().add(Tabela.de(fila)
                .coluna("Chegou", c -> c.getCriadoEm() == null ? ""
                        : Pecas.data(c.getCriadoEm().toLocalDate()), 0.8)
                .coluna("O que veio", Comprovante::getTexto, 3)
                .coluna("Destino lido", Comprovante::getDestinoLido, 1.4)
                .coluna("Identificador", Comprovante::getIdentificadorLido, 1.2)
                .valor("Data lida", c -> Pecas.data(c.getDataLida()))
                .valor("Valor lido", c -> Pecas.numero(c.getValorLido()))
                .comMarca(Comprovante::getSituacao,
                        c -> "CONFERIDO".equals(c.getSituacao()) ? "s-pago"
                                : "RECUSADO".equals(c.getSituacao()) ? "s-vencido" : "")
                .aoClicar(c -> {
                    aberto = c.getId();
                    janela.atualizar();
                })
                .quandoVazia("Nenhum comprovante na fila.")
                .montar());

        if (aberto != null) {
            tela.getChildren().add(Pecas.secao("Conferir este comprovante"));
            tela.getChildren().add(conferir());
        }
        return tela;
    }


    /** O comprovante que chega por fora entra por aqui, sem sair da tela. */
    private void abrirReceber() {
        ComboBox<br.com.itia.financeiro.dominio.Pagador> cliente = new ComboBox<>();
        cliente.getItems().add(null);
        cliente.getItems().addAll(comprovantes.clientes());
        cliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(br.com.itia.financeiro.dominio.Pagador quem) {
                return quem == null ? "não sei ainda" : quem.getNome();
            }

            @Override
            public br.com.itia.financeiro.dominio.Pagador fromString(String texto) {
                return null;
            }
        });
        cliente.getSelectionModel().selectFirst();
        cliente.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> origem = new ComboBox<>();
        origem.getItems().addAll("DIGITADO", "WHATSAPP", "IMPORTADO");
        origem.setConverter(new StringConverter<>() {
            @Override
            public String toString(String qual) {
                if (qual == null) {
                    return "";
                }
                return switch (qual) {
                    case "WHATSAPP" -> "WhatsApp";
                    case "IMPORTADO" -> "importado";
                    default -> "digitado aqui";
                };
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        origem.getSelectionModel().selectFirst();
        origem.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TextArea texto = new javafx.scene.control.TextArea();
        texto.setPromptText("Cole aqui o comprovante do aplicativo do banco");
        texto.setPrefRowCount(6);

        Label escolhido = new Label("nenhum arquivo escolhido");
        escolhido.getStyleClass().add("dica");
        java.io.File[] arquivo = new java.io.File[1];

        Label dica = new Label("Cole o texto do comprovante e, se tiver, anexe o arquivo. O "
                + "sistema lê valor, data, destino e identificador do texto.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        HBox campos = new HBox(16, Pecas.campo("Cliente", cliente),
                Pecas.campo("De onde veio", origem));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        HBox anexo = new HBox(12, Pecas.botaoVazado("Escolher arquivo", () -> {
            javafx.stage.FileChooser escolher = new javafx.stage.FileChooser();
            escolher.setTitle("Arquivo do comprovante");
            java.io.File qual = escolher.showOpenDialog(janela.palco());
            if (qual != null) {
                arquivo[0] = qual;
                escolhido.setText(qual.getName());
            }
        }), escolhido);
        anexo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        JanelaFlutuante.nova(janela.palco(), "Receber comprovante", "Prova de pagamento.")
                .com(dica, campos, Pecas.campo("Arquivo", anexo),
                        Pecas.campo("Texto do comprovante", texto))
                .acao("Colocar na fila", () -> {
                    br.com.itia.financeiro.dominio.ArquivoRecebido recebido = null;
                    if (arquivo[0] != null) {
                        try {
                            recebido = new br.com.itia.financeiro.dominio.ArquivoRecebido(
                                    arquivo[0].getName(),
                                    java.nio.file.Files.readAllBytes(arquivo[0].toPath()));
                        } catch (java.io.IOException naoLeu) {
                            janela.reclamar("Não deu para ler o arquivo: " + naoLeu.getMessage());
                            return false;
                        }
                    }
                    if ((texto.getText() == null || texto.getText().isBlank())
                            && recebido == null) {
                        janela.reclamar("Cole o texto do comprovante ou anexe o arquivo.");
                        return false;
                    }
                    comprovantes.receber(cliente.getValue() == null ? null
                                    : cliente.getValue().getId(), texto.getText(), recebido,
                            origem.getValue());
                    janela.avisar("Comprovante na fila, esperando conferência.");
                    janela.ir(TelaComprovantes.class);
                    return true;
                })
                .abrir();
    }

    private VBox conferir() {
        Comprovante comprovante = comprovantes.comprovante(aberto);
        List<Titulo> sugestoes = comprovantes.sugestoesPara(comprovante);
        List<Titulo> emAberto = comprovantes.titulosEmAberto();

        Label oQueVeio = new Label(comprovante.getTexto());
        oQueVeio.setWrapText(true);

        ComboBox<Titulo> titulo = new ComboBox<>();
        titulo.getItems().addAll(sugestoes.isEmpty() ? emAberto : sugestoes);
        titulo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Titulo qual) {
                return qual == null ? "" : qual.getNumero() + " · "
                        + qual.getCliente().getRazaoSocial() + " · "
                        + Pecas.dinheiro(qual.getSaldo());
            }

            @Override
            public Titulo fromString(String texto) {
                return null;
            }
        });
        titulo.getSelectionModel().selectFirst();
        titulo.setMaxWidth(Double.MAX_VALUE);

        Label dica = new Label(sugestoes.isEmpty()
                ? "Nenhum título bateu sozinho. Escolha na lista qual documento este "
                        + "comprovante quita."
                : "O sistema achou " + sugestoes.size() + " título(s) parecidos. Confira antes "
                        + "de dar baixa.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        HBox linha = new HBox(16, Pecas.campo("Título que este comprovante quita", titulo));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(oQueVeio, dica, linha, new HBox(12,
                Pecas.botao("Conferir e baixar", () -> {
                    if (titulo.getValue() == null) {
                        janela.reclamar("Escolha o título que este comprovante quita.");
                        return;
                    }
                    comprovantes.conferir(aberto, titulo.getValue().getId(),
                            comprovante.getValorLido());
                    janela.avisar("Comprovante conferido e baixa lançada.");
                    aberto = null;
                    janela.ir(TelaComprovantes.class);
                }),
                Pecas.botaoVazado("Voltar para a fila", () -> {
                    comprovantes.voltarParaFila(aberto);
                    janela.avisar("Comprovante voltou para a fila.");
                    aberto = null;
                    janela.ir(TelaComprovantes.class);
                }),
                Pecas.botaoPerigo("Recusar", () -> {
                    javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
                    motivo.setPromptText("por que este comprovante não vale");
                    JanelaFlutuante.estreita(janela.palco(), "Recusar comprovante",
                                    "O cliente continua devendo e o motivo fica guardado.")
                            .com(Pecas.campo("Motivo", motivo))
                            .acao("Recusar", () -> {
                                comprovantes.recusar(aberto, motivo.getText().isBlank()
                                        ? "sem motivo" : motivo.getText().trim());
                                janela.avisar("Comprovante recusado, com o motivo guardado.");
                                aberto = null;
                                janela.ir(TelaComprovantes.class);
                                return true;
                            })
                            .abrir();
                })));
    }
}
