package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Documento;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.ItemExecutado;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.ServicoRealizado;
import br.com.itia.financeiro.dominio.SituacaoDoAtendimento;
import br.com.itia.financeiro.dominio.TratamentoDoAtendimento;
import br.com.itia.financeiro.servico.DocumentoServico;
import br.com.itia.financeiro.servico.ServicosRealizados;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * A ficha de um atendimento já realizado.
 *
 * Mostra o que foi feito, para quem, com qual tratamento de preço, e guarda os
 * itens executados e os comprovantes do que foi entregue.
 */
@Component
public class TelaRealizado implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicosRealizados realizados;
    private final DocumentoServico documentos;
    private final Janela janela;

    private UUID qual;

    public TelaRealizado(ServicosRealizados realizados, DocumentoServico documentos,
                         @Lazy Janela janela) {
        this.realizados = realizados;
        this.documentos = documentos;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public Node montar() {
        ServicoRealizado atendimento = realizados.atendimento(qual);
        boolean cancelado = atendimento.getSituacao() == SituacaoDoAtendimento.CANCELADO;

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("catálogo",
                atendimento.getServico() == null ? "Atendimento"
                        : atendimento.getServico().getNome(),
                (atendimento.getPagador() == null ? "" : atendimento.getPagador().getNome())
                        + " · " + Pecas.data(atendimento.getRealizadoEm()),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaRealizados.class)),
                cancelado
                        ? Pecas.botao("Reabrir atendimento", () -> {
                            realizados.reabrir(qual);
                            janela.avisar("Atendimento reaberto.");
                            janela.atualizar();
                        })
                        : Pecas.botaoPerigo("Cancelar atendimento", () -> {
                            realizados.cancelar(qual);
                            janela.avisar("Atendimento cancelado. A cobrança dele também sai.");
                            janela.atualizar();
                        }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Tratamento", atendimento.getTratamento() == null ? ""
                        : atendimento.getTratamento().getRotulo(),
                        "como este atendimento é cobrado", true, false),
                Pecas.quadro("Valor", atendimento.getValorResumido(), "o que vai ser cobrado"),
                Pecas.quadro("Pacote do cliente", atendimento.getContratacao() == null
                        ? "sem pacote" : atendimento.getContratacao().getPacote().getNome(),
                        "de onde veio a cobertura"),
                Pecas.quadro("Situação", atendimento.getSituacao() == null ? ""
                        : atendimento.getSituacao().name().toLowerCase(),
                        cancelado ? "não gera cobrança" : "entra na cobrança"))));


        tela.getChildren().add(Pecas.secao("Dados do atendimento"));
        tela.getChildren().add(dados(atendimento));

        tela.getChildren().add(Pecas.cabecalho("execução", "Itens executados",
                "O que foi feito dentro deste atendimento.",
                Pecas.botao("Adicionar item executado",
                        () -> janelaDoItem(atendimento))));
        tela.getChildren().add(Tabela.de(atendimento.getItens())
                .coluna("Item", i -> i.getItem() == null ? "" : i.getItem().getNome(), 2)
                .valor("Quantidade", i -> Pecas.numero(i.getQuantidade()))
                .coluna("Observação", ItemExecutado::getObservacao, 2.4)
                .quandoVazia("Nenhum item executado registrado.")
                .montar());

        tela.getChildren().add(Pecas.cabecalho("documentos", "Comprovantes",
                "O que comprova a entrega deste atendimento.",
                Pecas.botao("Anexar comprovante", this::anexar)));
        tela.getChildren().add(Tabela.de(realizados.comprovantesDe(qual))
                .coluna("Arquivo", Documento::getNomeArquivo, 2.4)
                .coluna("Tipo", Documento::getTipo, 1)
                .coluna("Tamanho", Documento::getTamanhoLegivel, 0.8)
                .coluna("Quando", d -> d.getAnexadoEm() == null ? ""
                        : d.getAnexadoEm().format(QUANDO), 1.1)
                .quandoVazia("Nenhum comprovante anexado.")
                .montar());
        return tela;
    }

    // ------------------------------------------------------------------ dados

    private VBox dados(ServicoRealizado atendimento) {
        ComboBox<Pagador> cliente = new ComboBox<>();
        cliente.getItems().addAll(realizados.clientes());
        cliente.setConverter(nome(Pagador::getNome));
        if (atendimento.getPagador() != null) {
            cliente.getItems().stream()
                    .filter(c -> c.getId().equals(atendimento.getPagador().getId()))
                    .findFirst().ifPresent(cliente.getSelectionModel()::select);
        }
        cliente.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ClienteEspelho> unidade = new ComboBox<>();
        unidade.getItems().addAll(realizados.unidadesDaEmpresa());
        unidade.setConverter(nome(ClienteEspelho::getRazaoSocial));
        if (atendimento.getUnidade() != null) {
            unidade.getItems().stream()
                    .filter(u -> u.getId().equals(atendimento.getUnidade().getId()))
                    .findFirst().ifPresent(unidade.getSelectionModel()::select);
        }
        unidade.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Servico> servico = new ComboBox<>();
        servico.getItems().addAll(realizados.servicosDisponiveis());
        servico.setConverter(nome(Servico::getNome));
        if (atendimento.getServico() != null) {
            servico.getItems().stream()
                    .filter(s -> s.getId().equals(atendimento.getServico().getId()))
                    .findFirst().ifPresent(servico.getSelectionModel()::select);
        }
        servico.setMaxWidth(Double.MAX_VALUE);

        DatePicker quando = new DatePicker(atendimento.getRealizadoEm());
        quando.setMaxWidth(Double.MAX_VALUE);

        TextField quantidade = new TextField(Pecas.numero(atendimento.getQuantidade()));
        TextField responsavel = new TextField(atendimento.getResponsavel());

        ComboBox<TratamentoDoAtendimento> tratamento = new ComboBox<>();
        tratamento.getItems().addAll(TratamentoDoAtendimento.values());
        tratamento.setConverter(nome(TratamentoDoAtendimento::getRotulo));
        tratamento.getSelectionModel().select(atendimento.getTratamento());
        tratamento.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField(Pecas.numero(atendimento.getValorCobrado()));
        TextField desconto = new TextField(Pecas.numero(atendimento.getDesconto()));

        TextArea observacao = new TextArea(atendimento.getObservacao());
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        HBox linha1 = new HBox(16, Pecas.campo("Cliente atendido", cliente),
                Pecas.campo("Unidade", unidade),
                Pecas.campo("Serviço realizado", servico));
        HBox linha2 = new HBox(16, Pecas.campo("Data", quando),
                Pecas.campo("Quantidade", quantidade),
                Pecas.campo("Responsável", responsavel),
                Pecas.campo("Tratamento", tratamento));
        HBox linha3 = new HBox(16, Pecas.campo("Valor cobrado (R$)", valor),
                Pecas.campo("Desconto (R$)", desconto));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(linha1, linha2, linha3,
                Pecas.campo("Observações", observacao),
                new HBox(Pecas.botao("Salvar atendimento", () -> {
                    if (cliente.getValue() == null || servico.getValue() == null) {
                        janela.reclamar("Escolha o cliente e o serviço.");
                        return;
                    }
                    realizados.salvar(qual, cliente.getValue().getId(),
                            unidade.getValue() == null ? null : unidade.getValue().getId(),
                            servico.getValue().getId(), quando.getValue(),
                            numero(quantidade.getText()), responsavel.getText(),
                            tratamento.getValue() == null ? null
                                    : tratamento.getValue().name(),
                            numero(valor.getText()), numero(desconto.getText()),
                            observacao.getText());
                    janela.avisar("Atendimento salvo.");
                    janela.atualizar();
                })));
    }

    // ------------------------------------------------------------------ itens

    private void janelaDoItem(ServicoRealizado atendimento) {
        ComboBox<ItemDeServico> item = new ComboBox<>();
        if (atendimento.getServico() != null) {
            item.getItems().addAll(atendimento.getServico().getItensAtivos());
        }
        item.setConverter(nome(ItemDeServico::getNome));
        item.getSelectionModel().selectFirst();
        item.setMaxWidth(Double.MAX_VALUE);

        TextField quantidade = new TextField("1");
        TextArea observacao = new TextArea();
        observacao.setPrefRowCount(2);
        observacao.setWrapText(true);

        if (item.getItems().isEmpty()) {
            janela.reclamar("Este serviço não tem itens cadastrados para executar.");
            return;
        }

        JanelaFlutuante.estreita(janela.palco(), "Item executado",
                        "O que foi feito dentro deste atendimento.")
                .com(Pecas.campo("Item do serviço", item),
                        Pecas.campo("Quantidade", quantidade),
                        Pecas.campo("Observação", observacao))
                .acao("Adicionar item executado", () -> {
                    realizados.adicionarItem(qual, item.getValue().getId(),
                            numero(quantidade.getText()), observacao.getText());
                    janela.avisar("Item executado registrado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // ----------------------------------------------------------- comprovantes

    private void anexar() {
        javafx.stage.FileChooser escolher = new javafx.stage.FileChooser();
        escolher.setTitle("Comprovante do atendimento");
        java.io.File arquivo = escolher.showOpenDialog(janela.palco());
        if (arquivo == null) {
            return;
        }
        try {
            byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo.toPath());
            ServicoRealizado atendimento = realizados.atendimento(qual);
            Documento guardado = documentos.anexar(
                    new br.com.itia.financeiro.dominio.ArquivoRecebido(arquivo.getName(),
                            conteudo),
                    atendimento.getPagador() == null ? null : atendimento.getPagador().getId(),
                    atendimento.getUnidade() == null ? null : atendimento.getUnidade().getId(),
                    null, "COMPROVANTE", null, null);
            realizados.guardarComprovante(qual, guardado);
            janela.avisar("Comprovante anexado.");
            janela.atualizar();
        } catch (java.io.IOException naoLeu) {
            janela.reclamar("Não deu para ler o arquivo: " + naoLeu.getMessage());
        }
    }

    // ------------------------------------------------------------------ apoio

    private <T> StringConverter<T> nome(java.util.function.Function<T, String> comoChamar) {
        return new StringConverter<>() {
            @Override
            public String toString(T qualItem) {
                return qualItem == null ? "" : comoChamar.apply(qualItem);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }

    private BigDecimal numero(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
