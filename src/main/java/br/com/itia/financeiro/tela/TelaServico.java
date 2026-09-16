package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Departamento;
import br.com.itia.financeiro.dominio.FormaDePreco;
import br.com.itia.financeiro.dominio.ItemDeServico;
import br.com.itia.financeiro.dominio.MudancaDePreco;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.dominio.TratamentoDePreco;
import br.com.itia.financeiro.dominio.UnidadeDeCobranca;
import br.com.itia.financeiro.servico.CatalogoServico;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * A ficha de um serviço do catálogo.
 *
 * Quatro partes, como sempre foi: os dados gerais, os itens que compõem o
 * serviço, o preço com a data em que passa a valer, e o histórico de tudo o que
 * já mudou de valor, com quem mudou e por quê.
 */
@Component
public class TelaServico implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CatalogoServico catalogo;
    private final Janela janela;

    private UUID qual;
    private String aba = "dados";

    public TelaServico(CatalogoServico catalogo, @Lazy Janela janela) {
        this.catalogo = catalogo;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public void escolher(UUID id) {
        if (!id.equals(qual)) {
            aba = "dados";
        }
        this.qual = id;
    }

    @Override
    public Node montar() {
        Servico servico = catalogo.servico(qual);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("catálogo", servico.getNome(),
                servico.getCodigo() + (servico.getDescricao() == null ? ""
                        : " · " + servico.getDescricao()),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaServicos.class)),
                Pecas.botaoVazado("Duplicar", () -> {
                    Servico copia = catalogo.duplicar(qual);
                    janela.avisar("Serviço copiado. A cópia já está no catálogo.");
                    janela.ir(TelaServico.class, copia.getId());
                }),
                servico.isAtivo()
                        ? Pecas.botaoPerigo("Inativar", () -> {
                            catalogo.inativar(qual);
                            janela.avisar("Serviço inativado. Ele não entra em atendimento novo.");
                            janela.atualizar();
                        })
                        : Pecas.botao("Reativar", () -> {
                            catalogo.reativar(qual);
                            janela.avisar("Serviço reativado.");
                            janela.atualizar();
                        })));

        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Dados gerais", () -> trocar("dados"));
        partes.put("Itens do serviço", () -> trocar("itens"));
        partes.put("Preços", () -> trocar("precos"));
        partes.put("Histórico", () -> trocar("historico"));
        tela.getChildren().add(Pecas.abas(switch (aba) {
            case "itens" -> "Itens do serviço";
            case "precos" -> "Preços";
            case "historico" -> "Histórico";
            default -> "Dados gerais";
        }, partes));

        switch (aba) {
            case "itens" -> itens(tela, servico);
            case "precos" -> precos(tela, servico);
            case "historico" -> historico(tela);
            default -> dadosGerais(tela, servico);
        }
        return tela;
    }

    private void trocar(String qualAba) {
        this.aba = qualAba;
        janela.atualizar();
    }

    // ------------------------------------------------------------ dados gerais

    private void dadosGerais(VBox tela, Servico servico) {
        TextField nome = new TextField(servico.getNome());
        TextField responsavel = new TextField(servico.getResponsavelPadrao());
        responsavel.setPromptText("quem costuma executar");

        ComboBox<Departamento> departamento = new ComboBox<>();
        departamento.getItems().addAll(catalogo.departamentosAtivos());
        departamento.setConverter(nome(Departamento::getNome));
        if (servico.getDepartamento() != null) {
            departamento.getItems().stream()
                    .filter(d -> d.getId().equals(servico.getDepartamento().getId()))
                    .findFirst().ifPresent(departamento.getSelectionModel()::select);
        }
        departamento.setMaxWidth(Double.MAX_VALUE);

        CheckBox ativo = new CheckBox("Serviço ativo");
        ativo.setSelected(servico.isAtivo());

        TextArea descricao = new TextArea(servico.getDescricao());
        descricao.setPrefRowCount(3);
        descricao.setWrapText(true);

        HBox linha = new HBox(16, Pecas.campo("Código", new Label(servico.getCodigo())),
                Pecas.campo("Nome do serviço", nome),
                Pecas.campo("Departamento", departamento),
                Pecas.campo("Responsável padrão", responsavel));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        tela.getChildren().add(Pecas.secao("Dados gerais"));
        tela.getChildren().add(Pecas.caixa(linha, Pecas.campo("Descrição", descricao), ativo,
                new HBox(12,
                        Pecas.botao("Salvar", () -> {
                            catalogo.salvarDadosGerais(qual, nome.getText(), descricao.getText(),
                                    departamento.getValue() == null ? null
                                            : departamento.getValue().getId(),
                                    responsavel.getText(), ativo.isSelected());
                            janela.avisar("Dados do serviço salvos.");
                            janela.atualizar();
                        }),
                        Pecas.botaoVazado("Gerenciar departamentos", this::gerenciarDepartamentos)
                )));
    }

    // ------------------------------------------------------------------- itens

    private void itens(VBox tela, Servico servico) {
        tela.getChildren().add(Pecas.cabecalho("item do serviço", "Itens do serviço",
                "O que faz parte da execução. O item pode estar incluído no preço ou ter "
                        + "cobrança própria.",
                Pecas.botao("Adicionar item", () -> janelaDoItem(null))));

        tela.getChildren().add(Tabela.de(servico.getItens())
                .coluna("Ordem", i -> String.valueOf(i.getOrdem()), 0.4)
                .coluna("Item", ItemDeServico::getNome, 2)
                .coluna("Obrigatório", i -> i.isObrigatorio() ? "sim" : "não", 0.7)
                .valor("Quantidade padrão", i -> Pecas.numero(i.getQuantidadePadrao()))
                .coluna("Tratamento do preço", i -> i.getTratamentoPreco() == null ? ""
                        : i.getTratamentoPreco().getRotulo(), 1.4)
                .valor("Valor", i -> Pecas.numero(i.getValor()))
                .comMarca(i -> i.isAtivo() ? "ativo" : "inativo",
                        i -> i.isAtivo() ? "" : "s-cancelado")
                .aoClicar(this::janelaDoItem)
                .quandoVazia("Nenhum item cadastrado. O serviço é cobrado inteiro.")
                .montar());

        Label dica = new Label("Clique num item para editar.");
        dica.getStyleClass().add("dica");
        tela.getChildren().add(dica);

        tela.getChildren().add(Pecas.secao("Como o total fica"));
        tela.getChildren().add(Tabela.de(servico.getItensAtivos())
                .coluna("Parte", ItemDeServico::getNome, 2)
                .coluna("Tratamento", i -> i.getTratamentoPreco() == null ? ""
                        : i.getTratamentoPreco().getRotulo(), 1.4)
                .valor("Entra no total", i -> i.getTratamentoPreco() != null
                        && i.getTratamentoPreco().cobraSeparado()
                        ? Pecas.numero(i.getValorTotalPadrao()) : "incluído")
                .quandoVazia("Sem itens: o total é o preço do serviço.")
                .montar());

        Label total = new Label("Total padrão do serviço: "
                + Pecas.dinheiro(servico.getTotalPadrao()));
        total.getStyleClass().add("linha-apoio");
        tela.getChildren().add(total);
    }

    /** A janela de criar ou editar um item, igual à de sempre. */
    private void janelaDoItem(ItemDeServico item) {
        boolean novo = item == null;

        TextField nome = new TextField(novo ? "" : item.getNome());
        nome.setPromptText("nome do item");

        CheckBox obrigatorio = new CheckBox("Faz parte de toda execução");
        obrigatorio.setSelected(!novo && item.isObrigatorio());

        TextField quantidade = new TextField(novo ? "1" : Pecas.numero(item.getQuantidadePadrao()));

        ComboBox<TratamentoDePreco> tratamento = new ComboBox<>();
        tratamento.getItems().addAll(TratamentoDePreco.values());
        tratamento.setConverter(nome(TratamentoDePreco::getRotulo));
        tratamento.getSelectionModel().select(novo ? TratamentoDePreco.INCLUIDO
                : item.getTratamentoPreco());
        tratamento.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField(novo ? "0,00" : Pecas.numero(item.getValor()));

        ComboBox<UnidadeDeCobranca> unidade = new ComboBox<>();
        unidade.getItems().addAll(catalogo.unidadesAtivas());
        unidade.setConverter(nome(UnidadeDeCobranca::getNome));
        if (!novo && item.getUnidade() != null) {
            unidade.getItems().stream()
                    .filter(u -> u.getId().equals(item.getUnidade().getId()))
                    .findFirst().ifPresent(unidade.getSelectionModel()::select);
        }
        unidade.setMaxWidth(Double.MAX_VALUE);

        TextField ordem = new TextField(novo ? "" : String.valueOf(item.getOrdem()));
        CheckBox ativo = new CheckBox("Item ativo");
        ativo.setSelected(novo || item.isAtivo());

        TextArea descricao = new TextArea(novo ? "" : item.getDescricao());
        descricao.setPrefRowCount(2);
        descricao.setWrapText(true);

        TextField justificativa = new TextField();
        justificativa.setPromptText("por que o valor mudou");

        HBox linha1 = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Quantidade padrão", quantidade),
                Pecas.campo("Tratamento do preço", tratamento));
        HBox linha2 = new HBox(16, Pecas.campo("Valor, se tiver cobrança própria", valor),
                Pecas.campo("Unidade", unidade));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante caixa = JanelaFlutuante.nova(janela.palco(),
                novo ? "Novo item" : "Item",
                "O item entra na execução do serviço. Se tiver cobrança própria, o valor dele "
                        + "soma no total.");
        caixa.com(linha1, linha2, Pecas.campo("Descrição", descricao), obrigatorio);
        if (!novo) {
            HBox linha3 = new HBox(16, Pecas.campo("Ordem", ordem),
                    Pecas.campo("Justificativa, se mudar o valor", justificativa));
            linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
            caixa.com(linha3, ativo);
        }
        caixa.acao(novo ? "Adicionar item" : "Salvar item", () -> {
            if (nome.getText() == null || nome.getText().isBlank()) {
                janela.reclamar("Escreva o nome do item.");
                return false;
            }
            if (novo) {
                catalogo.adicionarItem(qual, nome.getText(), descricao.getText(),
                        obrigatorio.isSelected(), numero(quantidade.getText()),
                        tratamento.getValue(), numero(valor.getText()),
                        unidade.getValue() == null ? null : unidade.getValue().getId());
                janela.avisar("Item incluído no serviço.");
            } else {
                catalogo.salvarItem(qual, item.getId(), nome.getText(), descricao.getText(),
                        obrigatorio.isSelected(), numero(quantidade.getText()),
                        tratamento.getValue(), numero(valor.getText()),
                        unidade.getValue() == null ? null : unidade.getValue().getId(),
                        inteiro(ordem.getText(), item.getOrdem()), ativo.isSelected(),
                        justificativa.getText());
                janela.avisar("Item salvo.");
            }
            janela.atualizar();
            return true;
        });
        if (!novo) {
            caixa.outraAcao("Tirar do serviço", () -> {
                catalogo.inativarItem(qual, item.getId());
                janela.avisar("Item tirado do serviço.");
                caixa.fechar();
                janela.atualizar();
            });
        }
        caixa.abrir();
    }

    // ------------------------------------------------------------------ preços

    private void precos(VBox tela, Servico servico) {
        ComboBox<FormaDePreco> forma = new ComboBox<>();
        forma.getItems().addAll(FormaDePreco.values());
        forma.setConverter(nome(FormaDePreco::getRotulo));
        forma.getSelectionModel().select(servico.getFormaPreco());
        forma.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField(Pecas.numero(servico.getValor()));

        ComboBox<UnidadeDeCobranca> unidade = new ComboBox<>();
        unidade.getItems().addAll(catalogo.unidadesAtivas());
        unidade.setConverter(nome(UnidadeDeCobranca::getNome));
        if (servico.getUnidade() != null) {
            unidade.getItems().stream()
                    .filter(u -> u.getId().equals(servico.getUnidade().getId()))
                    .findFirst().ifPresent(unidade.getSelectionModel()::select);
        }
        unidade.setMaxWidth(Double.MAX_VALUE);

        DatePicker vigencia = new DatePicker(servico.getVigenciaInicio() == null
                ? LocalDate.now() : servico.getVigenciaInicio());
        vigencia.setMaxWidth(Double.MAX_VALUE);

        TextField justificativa = new TextField();
        justificativa.setPromptText("por que o preço mudou");

        HBox linha = new HBox(16, Pecas.campo("Como o preço é calculado", forma),
                Pecas.campo("Valor do serviço", valor),
                Pecas.campo("Unidade de cobrança", unidade),
                Pecas.campo("Vale a partir de", vigencia));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        Label explicacao = new Label(servico.getFormaPreco() == null ? ""
                : servico.getFormaPreco().getComoFunciona());
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);

        tela.getChildren().add(Pecas.secao("Preço do serviço"));
        tela.getChildren().add(Pecas.caixa(linha, explicacao,
                Pecas.campo("Justificativa da alteração", justificativa),
                new HBox(12,
                        Pecas.botao("Salvar preço", () -> {
                            catalogo.salvarPreco(qual, forma.getValue(),
                                    numero(valor.getText()),
                                    unidade.getValue() == null ? null
                                            : unidade.getValue().getId(),
                                    vigencia.getValue(), justificativa.getText());
                            janela.avisar("Preço salvo. O histórico guardou a mudança.");
                            janela.atualizar();
                        }),
                        Pecas.botaoVazado("Gerenciar unidades", this::gerenciarUnidades))));
    }

    // --------------------------------------------------------------- histórico

    private void historico(VBox tela) {
        List<MudancaDePreco> mudancas = catalogo.historicoDe(qual);
        tela.getChildren().add(Pecas.secao("Tudo o que já mudou de valor"));
        tela.getChildren().add(Tabela.de(mudancas)
                .coluna("Quando", m -> m.getQuando() == null ? ""
                        : m.getQuando().format(QUANDO), 1.1)
                .coluna("O que mudou", MudancaDePreco::getOQueMudou, 1.8)
                .valor("De", m -> Pecas.numero(m.getValorAnterior()))
                .valor("Para", m -> Pecas.numero(m.getValorNovo()))
                .coluna("Vale desde", m -> Pecas.data(m.getVigenciaInicio()), 0.9)
                .coluna("Justificativa", MudancaDePreco::getJustificativa, 2)
                .coluna("Quem", MudancaDePreco::getQuem, 1)
                .quandoVazia("Nenhuma mudança de preço registrada ainda.")
                .montar());
    }

    // -------------------------------------------------------------- cadastros

    private void gerenciarDepartamentos() {
        TextField nome = new TextField();
        nome.setPromptText("nome do departamento");
        TextArea descricao = new TextArea();
        descricao.setPrefRowCount(2);

        VBox lista = new VBox(6);
        for (Departamento departamento : catalogo.departamentos()) {
            Label linha = new Label(departamento.getNome()
                    + (departamento.isAtivo() ? "" : " · inativo"));
            lista.getChildren().add(linha);
        }

        JanelaFlutuante.estreita(janela.palco(), "Departamentos",
                        "O departamento diz de quem é o serviço dentro da casa.")
                .com(Pecas.secao("Departamentos cadastrados"), Pecas.caixa(lista),
                        Pecas.secao("Novo departamento"),
                        Pecas.campo("Nome", nome), Pecas.campo("Descrição", descricao))
                .acao("Cadastrar", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome do departamento.");
                        return false;
                    }
                    catalogo.cadastrarDepartamento(nome.getText(), descricao.getText());
                    janela.avisar("Departamento cadastrado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void gerenciarUnidades() {
        TextField nome = new TextField();
        nome.setPromptText("hora, mês, documento");

        VBox lista = new VBox(6);
        for (UnidadeDeCobranca unidade : catalogo.unidades()) {
            lista.getChildren().add(new Label(unidade.getNome()
                    + (unidade.isAtivo() ? "" : " · inativa")));
        }

        JanelaFlutuante.estreita(janela.palco(), "Unidades de cobrança",
                        "A unidade diz em cima de quê o preço é cobrado.")
                .com(Pecas.secao("Unidades cadastradas"), Pecas.caixa(lista),
                        Pecas.secao("Nova unidade"), Pecas.campo("Nome", nome))
                .acao("Cadastrar", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome da unidade.");
                        return false;
                    }
                    catalogo.cadastrarUnidade(nome.getText());
                    janela.avisar("Unidade cadastrada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
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

    private int inteiro(String texto, int seNaoDer) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return seNaoDer;
        }
    }
}
