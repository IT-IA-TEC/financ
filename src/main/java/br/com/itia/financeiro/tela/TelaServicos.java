package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Departamento;
import br.com.itia.financeiro.dominio.Servico;
import br.com.itia.financeiro.servico.CatalogoServico;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
 * O catálogo de serviços: o que a empresa vende.
 *
 * O preço de cada serviço tem forma própria (fixo, por hora, por item), e toda
 * mudança de preço fica registrada com data e autor.
 */
@Component
public class TelaServicos implements Tela {

    private final CatalogoServico catalogo;
    private final Janela janela;

    private String busca = "";
    private UUID departamentoEscolhido;
    private String situacaoEscolhida = "todas";

    public TelaServicos(CatalogoServico catalogo, @Lazy Janela janela) {
        this.catalogo = catalogo;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "servicos";
    }

    @Override
    public Node montar() {
        List<Servico> servicos = catalogo.catalogo(busca, departamentoEscolhido,
                "todas".equals(situacaoEscolhida) ? null : situacaoEscolhida);
        List<Departamento> departamentos = catalogo.departamentosAtivos();

        TextField procurar = new TextField(busca);
        procurar.setPromptText("procurar serviço");
        procurar.setPrefWidth(280);
        procurar.setOnAction(acao -> {
            busca = procurar.getText();
            janela.atualizar();
        });

        VBox tela = new VBox(16);
        ComboBox<Departamento> filtroDepartamento = new ComboBox<>();
        filtroDepartamento.getItems().add(null);
        filtroDepartamento.getItems().addAll(departamentos);
        filtroDepartamento.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Departamento qual) {
                return qual == null ? "todos os departamentos" : qual.getNome();
            }

            @Override
            public Departamento fromString(String texto) {
                return null;
            }
        });
        filtroDepartamento.getSelectionModel().select(departamentos.stream()
                .filter(d -> d.getId().equals(departamentoEscolhido)).findFirst().orElse(null));
        filtroDepartamento.setOnAction(acao -> {
            departamentoEscolhido = filtroDepartamento.getValue() == null ? null
                    : filtroDepartamento.getValue().getId();
            janela.atualizar();
        });

        ComboBox<String> filtroSituacao = new ComboBox<>();
        filtroSituacao.getItems().addAll("todas", "ativo", "inativo");
        filtroSituacao.getSelectionModel().select(situacaoEscolhida);
        filtroSituacao.setOnAction(acao -> {
            situacaoEscolhida = filtroSituacao.getValue();
            janela.atualizar();
        });

        tela.getChildren().add(Pecas.cabecalho("cadastro de apoio", "Serviços",
                "O que a empresa vende, com o preço de cada um.",
                Pecas.botaoVazado("Gerenciar departamentos", this::abrirDepartamentos),
                Pecas.botaoVazado("Gerenciar unidades", this::abrirUnidades),
                Pecas.botao("+ Novo serviço", this::abrirNovoServico),
                filtroDepartamento, filtroSituacao, procurar,
                Pecas.botaoVazado("Buscar", () -> {
                    busca = procurar.getText();
                    janela.atualizar();
                })));
        tela.getChildren().add(abas());

        tela.getChildren().add(Pecas.secao("Serviços cadastrados"));
        tela.getChildren().add(Tabela.de(servicos)
                .coluna("Código", Servico::getCodigo, 0.6)
                .coluna("Nome", Servico::getNome, 2)
                .coluna("Departamento", s -> s.getDepartamento() == null ? ""
                        : s.getDepartamento().getNome(), 1.2)
                .valor("Itens", s -> String.valueOf(s.getItensAtivos().size()))
                .coluna("Forma de cobrança", s -> s.getFormaPreco() == null ? ""
                        : s.getFormaPreco().getRotulo(), 1.4)
                .coluna("Preço", Servico::getPrecoResumido, 1.2)
                .comMarca(s -> s.isAtivo() ? "ativo" : "inativo",
                        s -> s.isAtivo() ? "" : "s-cancelado")
                .aoClicar(qual -> janela.ir(TelaServico.class, qual.getId()))
                .quandoVazia("Nenhum serviço cadastrado ainda.")
                .montar());

        return tela;
    }

    private Node abas() {
        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Serviços", () -> janela.ir(TelaServicos.class));
        partes.put("Pacotes", () -> janela.ir(TelaPacotes.class));
        partes.put("Serviços realizados", () -> janela.ir(TelaRealizados.class));
        partes.put("Cobranças", () -> janela.ir(TelaCobrancas.class));
        partes.put("Fechamento", () -> janela.ir(TelaFechamento.class));
        return Pecas.abas("Serviços", partes);
    }

    /** O serviço novo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoServico() {
        List<Departamento> departamentos = catalogo.departamentosAtivos();
        TextField nome = new TextField();
        nome.setPromptText("escrituração fiscal");
        TextField descricao = new TextField();

        ComboBox<Departamento> departamento = new ComboBox<>();
        departamento.getItems().add(null);
        departamento.getItems().addAll(departamentos);
        departamento.setConverter(new StringConverter<>() {
            @Override
            public String toString(Departamento qual) {
                return qual == null ? "sem departamento" : qual.getNome();
            }

            @Override
            public Departamento fromString(String texto) {
                return null;
            }
        });
        departamento.getSelectionModel().selectFirst();
        departamento.setMaxWidth(Double.MAX_VALUE);

        TextField responsavel = new TextField();
        responsavel.setPromptText("quem costuma fazer");

        HBox campos = new HBox(16,
                Pecas.campo("Nome", nome),
                Pecas.campo("Descrição", descricao),
                Pecas.campo("Departamento", departamento),
                Pecas.campo("Responsável padrão", responsavel));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Novo serviço",
                        "O que a empresa vende, com o preço de cada um.")
                .com(campos)
                .acao("Cadastrar serviço", () -> {
            if (nome.getText() == null || nome.getText().isBlank()) {
                janela.reclamar("Escreva o nome do serviço.");
                return false;
            }
            Servico novo = catalogo.cadastrar(nome.getText().trim(),
                    vazioViraNulo(descricao.getText()),
                    departamento.getValue() == null ? null : departamento.getValue().getId(),
                    vazioViraNulo(responsavel.getText()));
            janela.avisar("Serviço " + novo.getCodigo() + " cadastrado.");
            janela.ir(TelaServicos.class);
            return true;
                })
                .abrir();
    }

    private String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** Os departamentos: cadastro de apoio do catálogo. */
    private void abrirDepartamentos() {
        VBox lista = new VBox(10);
        for (br.com.itia.financeiro.dominio.Departamento departamento : catalogo.departamentos()) {
            TextField nome = new TextField(departamento.getNome());
            TextField descricao = new TextField(departamento.getDescricao());
            javafx.scene.control.CheckBox ativo =
                    new javafx.scene.control.CheckBox("ativo");
            ativo.setSelected(departamento.isAtivo());

            HBox linha = new HBox(12, Pecas.campo("Nome", nome),
                    Pecas.campo("Descrição", descricao), ativo,
                    Pecas.botaoVazado("Salvar", () -> {
                        catalogo.salvarDepartamento(departamento.getId(), nome.getText(),
                                descricao.getText(), ativo.isSelected());
                        janela.avisar("Departamento salvo.");
                        janela.ir(TelaServicos.class);
                    }));
            linha.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
            HBox.setHgrow(linha.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(linha.getChildren().get(1), Priority.ALWAYS);
            lista.getChildren().add(linha);
        }
        if (catalogo.departamentos().isEmpty()) {
            lista.getChildren().add(Pecas.vazio("Nenhum departamento cadastrado ainda."));
        }

        TextField novoNome = new TextField();
        novoNome.setPromptText("Fiscal, Contábil, Pessoal");
        TextField novaDescricao = new TextField();

        HBox novo = new HBox(12, Pecas.campo("Nome", novoNome),
                Pecas.campo("Descrição", novaDescricao));
        novo.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Departamentos", "Cadastro de apoio.")
                .com(lista, Pecas.secao("Novo departamento"), novo)
                .acao("Cadastrar departamento", () -> {
                    if (novoNome.getText() == null || novoNome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome do departamento.");
                        return false;
                    }
                    catalogo.cadastrarDepartamento(novoNome.getText().trim(),
                            novaDescricao.getText());
                    janela.avisar("Departamento cadastrado.");
                    janela.ir(TelaServicos.class);
                    return true;
                })
                .abrir();
    }

    /** As unidades de cobrança: por que unidade o serviço é cobrado. */
    private void abrirUnidades() {
        VBox lista = new VBox(10);
        for (br.com.itia.financeiro.dominio.UnidadeDeCobranca unidade : catalogo.unidades()) {
            TextField nome = new TextField(unidade.getNome());
            javafx.scene.control.CheckBox ativa = new javafx.scene.control.CheckBox("ativa");
            ativa.setSelected(unidade.isAtivo());

            HBox linha = new HBox(12, Pecas.campo("Nome", nome), ativa,
                    Pecas.botaoVazado("Salvar", () -> {
                        catalogo.salvarUnidade(unidade.getId(), nome.getText(),
                                ativa.isSelected());
                        janela.avisar("Unidade salva.");
                        janela.ir(TelaServicos.class);
                    }));
            linha.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
            HBox.setHgrow(linha.getChildren().get(0), Priority.ALWAYS);
            lista.getChildren().add(linha);
        }
        if (catalogo.unidades().isEmpty()) {
            lista.getChildren().add(Pecas.vazio("Nenhuma unidade cadastrada ainda."));
        }

        TextField nova = new TextField();
        nova.setPromptText("serviço, hora, atendimento, documento");

        Label dica = new Label("Por que unidade o serviço é cobrado: serviço, hora, "
                + "atendimento, documento ou o que a empresa usar.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.nova(janela.palco(), "Unidades de cobrança", "Cadastro de apoio.")
                .com(dica, lista, Pecas.secao("Nova unidade"), Pecas.campo("Nome", nova))
                .acao("Cadastrar unidade", () -> {
                    if (nova.getText() == null || nova.getText().isBlank()) {
                        janela.reclamar("Escreva o nome da unidade.");
                        return false;
                    }
                    catalogo.cadastrarUnidade(nova.getText().trim());
                    janela.avisar("Unidade cadastrada.");
                    janela.ir(TelaServicos.class);
                    return true;
                })
                .abrir();
    }
}
