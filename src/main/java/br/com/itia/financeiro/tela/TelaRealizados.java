package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ServicoRealizado;
import br.com.itia.financeiro.servico.ServicosRealizados;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Serviços realizados: o que foi feito para cada cliente.
 *
 * Cada atendimento sabe se estava dentro do pacote, se estourou o combinado ou
 * se foi avulso. É daí que sai a cobrança.
 */
@Component
public class TelaRealizados implements Tela {

    private final ServicosRealizados realizados;

    private String tratamentoEscolhido = "todos";
    private final Janela janela;

    public TelaRealizados(ServicosRealizados realizados, @Lazy Janela janela) {
        this.realizados = realizados;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
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
        List<ServicoRealizado> lista = realizados.lista(null, null,
                "todos".equals(tratamentoEscolhido) ? null : tratamentoEscolhido, null, null);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("serviços realizados", "Serviços realizados",
                "O que foi feito para cada cliente, e como isso entra na cobrança.",
                Pecas.botao("+ Registrar atendimento", this::abrirRegistrar),
                filtro("Tratamento", tratamentoEscolhido,
                        java.util.List.of("todos", "INCLUIDO_NO_PACOTE", "COBRADO_A_PARTE",
                                "COM_DESCONTO", "GRATUITO"),
                        escolhido -> {
                            tratamentoEscolhido = escolhido;
                            janela.atualizar();
                        })));

        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Serviços", () -> janela.ir(TelaServicos.class));
        partes.put("Pacotes", () -> janela.ir(TelaPacotes.class));
        partes.put("Serviços realizados", () -> janela.ir(TelaRealizados.class));
        partes.put("Cobranças", () -> janela.ir(TelaCobrancas.class));
        partes.put("Fechamento", () -> janela.ir(TelaFechamento.class));
        tela.getChildren().add(Pecas.abas("Serviços realizados", partes));

        tela.getChildren().add(Pecas.secao("Atendimentos"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Quando", r -> Pecas.data(r.getRealizadoEm()), 0.8)
                .coluna("Cliente", r -> r.getPagador() == null ? "" : r.getPagador().getNome(), 2)
                .coluna("Serviço", r -> r.getServico() == null ? "" : r.getServico().getNome(), 2)
                .coluna("Responsável", ServicoRealizado::getResponsavel)
                .valor("Quantidade", r -> Pecas.numero(r.getQuantidade()))
                .valor("Itens", r -> String.valueOf(r.getItens().size()))
                .coluna("Como entrou", r -> r.getTratamento() == null ? ""
                        : r.getTratamento().name().toLowerCase().replace('_', ' '), 1.2)
                .valor("Valor", ServicoRealizado::getValorResumido)
                .aoClicar(r -> janela.ir(TelaRealizado.class, r.getId()))
                .quandoVazia("Nenhum atendimento registrado ainda.")
                .montar());
        return tela;
    }

    /** O atendimento novo abre por cima da tela, com os campos da página. */
    private void abrirRegistrar() {
        java.util.List<br.com.itia.financeiro.dominio.Servico> servicos =
                realizados.servicosDisponiveis();
        if (servicos.isEmpty()) {
            janela.reclamar("Nenhum serviço cadastrado ainda. Cadastre em Serviços.");
            return;
        }

        ComboBox<br.com.itia.financeiro.dominio.Pagador> cliente = new ComboBox<>();
        cliente.getItems().addAll(realizados.clientes());
        cliente.setConverter(escolha(br.com.itia.financeiro.dominio.Pagador::getNome, ""));
        cliente.getSelectionModel().selectFirst();
        cliente.setMaxWidth(Double.MAX_VALUE);

        ComboBox<br.com.itia.financeiro.dominio.ClienteEspelho> unidade = new ComboBox<>();
        unidade.getItems().add(null);
        unidade.getItems().addAll(realizados.unidadesDaEmpresa());
        unidade.setConverter(escolha(
                br.com.itia.financeiro.dominio.ClienteEspelho::getRazaoSocial, "todas"));
        unidade.getSelectionModel().selectFirst();
        unidade.setMaxWidth(Double.MAX_VALUE);

        ComboBox<br.com.itia.financeiro.dominio.Servico> servico = new ComboBox<>();
        servico.getItems().addAll(servicos);
        servico.setConverter(escolha(br.com.itia.financeiro.dominio.Servico::getNome, ""));
        servico.getSelectionModel().selectFirst();
        servico.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.DatePicker quando =
                new javafx.scene.control.DatePicker(java.time.LocalDate.now());
        quando.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TextField quantidade = new javafx.scene.control.TextField("1");
        javafx.scene.control.TextField responsavel = new javafx.scene.control.TextField();
        responsavel.setPromptText("quem fez");

        ComboBox<String> tratamento = new ComboBox<>();
        tratamento.getItems().addAll("INCLUIDO_NO_PACOTE", "COBRADO_A_PARTE", "COM_DESCONTO",
                "GRATUITO");
        tratamento.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String qual) {
                return qual == null ? "" : qual.toLowerCase().replace('_', ' ');
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        tratamento.getSelectionModel().selectFirst();
        tratamento.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TextField valor = new javafx.scene.control.TextField("0,00");
        javafx.scene.control.TextField desconto = new javafx.scene.control.TextField("0,00");
        javafx.scene.control.TextArea observacao = new javafx.scene.control.TextArea();
        observacao.setPrefRowCount(3);

        javafx.scene.layout.HBox linha1 = new javafx.scene.layout.HBox(16,
                Pecas.campo("Cliente atendido", cliente), Pecas.campo("Unidade", unidade),
                Pecas.campo("Serviço realizado", servico));
        javafx.scene.layout.HBox linha2 = new javafx.scene.layout.HBox(16,
                Pecas.campo("Data", quando), Pecas.campo("Quantidade", quantidade),
                Pecas.campo("Responsável", responsavel),
                Pecas.campo("Tratamento", tratamento));
        javafx.scene.layout.HBox linha3 = new javafx.scene.layout.HBox(16,
                Pecas.campo("Valor cobrado (R$)", valor),
                Pecas.campo("Desconto (R$)", desconto));
        for (javafx.scene.layout.HBox linha : java.util.List.of(linha1, linha2, linha3)) {
            linha.getChildren().forEach(c ->
                    javafx.scene.layout.HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS));
        }

        JanelaFlutuante.nova(janela.palco(), "Registrar atendimento", "Serviços realizados.")
                .com(linha1, linha2, linha3, Pecas.campo("Observações", observacao))
                .acao("Registrar", () -> {
                    if (cliente.getValue() == null || servico.getValue() == null) {
                        janela.reclamar("Escolha o cliente e o serviço.");
                        return false;
                    }
                    realizados.registrar(cliente.getValue().getId(),
                            unidade.getValue() == null ? null : unidade.getValue().getId(),
                            servico.getValue().getId(), quando.getValue(),
                            TelaTituloNovo.dinheiro(quantidade.getText()),
                            responsavel.getText(), tratamento.getValue(),
                            TelaTituloNovo.dinheiro(valor.getText()),
                            TelaTituloNovo.dinheiro(desconto.getText()),
                            observacao.getText(), java.util.List.of());
                    janela.avisar("Atendimento registrado.");
                    janela.ir(TelaRealizados.class);
                    return true;
                })
                .abrir();
    }

    private <T> javafx.util.StringConverter<T> escolha(
            java.util.function.Function<T, String> comoChama, String quandoNulo) {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(T qual) {
                return qual == null ? quandoNulo : comoChama.apply(qual);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }
}
