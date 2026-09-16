package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Etiqueta;
import br.com.itia.financeiro.servico.EtiquetaServico;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tags: as marcas que a empresa usa para separar o que é dela.
 *
 * Cada tag vale para um escopo (cliente, título, conta), e o sistema mostra
 * quantas vezes cada uma está sendo usada antes de alguém mexer nela.
 */
@Component
public class TelaEtiquetas implements Tela {

    private final EtiquetaServico etiquetas;

    private String ondeEscolhido = "todas";
    private String situacaoEscolhida = "todas";
    private final Janela janela;

    public TelaEtiquetas(EtiquetaServico etiquetas, @Lazy Janela janela) {
        this.etiquetas = etiquetas;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "etiquetas";
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

    /** Peneira a lista pelo que a barra de filtros diz. */
    private java.util.List<Etiqueta> peneirar(java.util.List<Etiqueta> todas) {
        return todas.stream()
                .filter(e -> "todas".equals(ondeEscolhido)
                        || ondeEscolhido.equalsIgnoreCase(e.getEscopo()))
                .filter(e -> "todas".equals(situacaoEscolhida)
                        || ("ativa".equals(situacaoEscolhida) && e.isAtivo())
                        || ("desligada".equals(situacaoEscolhida) && !e.isAtivo()))
                .toList();
    }

    @Override
    public Node montar() {
        List<Etiqueta> lista = peneirar(etiquetas.todas());

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("organização", "Tags",
                "As marcas que a empresa usa para separar o que é dela.",
                Pecas.botao("+ Nova tag", this::abrirNovaTag),
                Pecas.botaoVazado("Criar as primeiras", () -> {
                    int quantas = etiquetas.criarSugestaoInicial();
                    janela.avisar(quantas == 0
                            ? "As tags sugeridas já existem."
                            : quantas + " tags sugeridas criadas.");
                    janela.atualizar();
                }),
                filtro("Onde vale", ondeEscolhido,
                        java.util.List.of("todas", "PESSOA", "UNIDADE", "CONTATO", "TITULO",
                                "TAREFA"),
                        escolhido -> {
                            ondeEscolhido = escolhido;
                            janela.atualizar();
                        }),
                filtro("Situação", situacaoEscolhida,
                        java.util.List.of("todas", "ativa", "desligada"),
                        escolhido -> {
                            situacaoEscolhida = escolhido;
                            janela.atualizar();
                        })));

        tela.getChildren().add(Pecas.secao("Tags cadastradas"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Código", Etiqueta::getCodigo, 0.6)
                .coluna("Nome", Etiqueta::getNome, 1.6)
                .coluna("Para que serve", Etiqueta::getDescricao, 2)
                .coluna("Onde pode ser usada", Etiqueta::getEscopo)
                .valor("Em uso", e -> String.valueOf(etiquetas.quantosUsos(e.getId())))
                .comMarca(e -> e.isAtivo() ? "ativa" : "desligada",
                        e -> e.isAtivo() ? "" : "s-cancelado")
                .aoClicar(this::abrirTag)
                .quandoVazia("Nenhuma tag cadastrada. Use o botão de cima para criar um "
                        + "conjunto inicial de papéis.")
                .montar());

        Label comoEditar = new Label("Clique numa tag para editar.");
        comoEditar.getStyleClass().add("dica");
        tela.getChildren().add(comoEditar);

        return tela;
    }

    /** A tag existente abre para edição, com o código que nunca muda. */
    private void abrirTag(Etiqueta etiqueta) {
        TextField nome = new TextField(etiqueta.getNome());
        TextField descricao = new TextField(etiqueta.getDescricao());
        ComboBox<String> escopo = new ComboBox<>();
        escopo.getItems().addAll("CLIENTE", "TITULO", "CONTA", "GERAL", "PESSOA", "UNIDADE",
                "CONTATO", "TAREFA");
        escopo.getSelectionModel().select(etiqueta.getEscopo());
        escopo.setMaxWidth(Double.MAX_VALUE);

        CheckBox ativa = new CheckBox("ativa");
        ativa.setSelected(etiqueta.isAtivo());

        Label codigo = new Label("Código: " + etiqueta.getCodigo()
                + " · gerado na criação e nunca muda.");
        codigo.getStyleClass().add("dica");

        HBox campos = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Descrição", descricao),
                Pecas.campo("Onde pode ser usada", escopo));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Editar tag", etiqueta.getNome())
                .com(codigo, campos, ativa)
                .acao("Salvar", () -> {
                    etiquetas.salvar(etiqueta.getId(), nome.getText(), descricao.getText(),
                            escopo.getValue(), null, ativa.isSelected());
                    janela.avisar("Tag salva.");
                    janela.ir(TelaEtiquetas.class);
                    return true;
                })
                .abrir();
    }

    /** A criação de tag abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovaTag() {
        TextField nome = new TextField();
        nome.setPromptText("cliente grande");
        TextField descricao = new TextField();
        ComboBox<String> escopo = new ComboBox<>();
        escopo.getItems().addAll("CLIENTE", "TITULO", "CONTA", "GERAL");
        escopo.getSelectionModel().selectFirst();
        escopo.setMaxWidth(Double.MAX_VALUE);

        HBox campos = new HBox(16,
                Pecas.campo("Nome", nome),
                Pecas.campo("Para que serve", descricao),
                Pecas.campo("Onde vale", escopo));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Nova tag",
                        "Uma marca nova para separar o que é da empresa.")
                .com(campos)
                .acao("Criar tag", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome da tag.");
                        return false;
                    }
                    etiquetas.cadastrar(nome.getText().trim(),
                            descricao.getText() == null || descricao.getText().isBlank() ? null
                                    : descricao.getText().trim(),
                            escopo.getValue(), null);
                    janela.avisar("Tag criada.");
                    janela.ir(TelaEtiquetas.class);
                    return true;
                })
                .abrir();
    }
}
