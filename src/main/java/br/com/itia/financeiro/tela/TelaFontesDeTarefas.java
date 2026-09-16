package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.FonteDeTarefas;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.servico.TarefasDeFora;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * De onde vêm as tarefas de outros sistemas.
 *
 * Cada fonte diz qual ligação usar, qual operação traz a lista e como cada
 * campo de lá vira tarefa aqui. A mesma tarefa nunca entra duas vezes: a chave
 * é o identificador de lá.
 */
@Component
public class TelaFontesDeTarefas implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final TarefasDeFora deFora;
    private final Janela janela;

    public TelaFontesDeTarefas(TarefasDeFora deFora, @Lazy Janela janela) {
        this.deFora = deFora;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cupula";
    }

    @Override
    public Node montar() {
        List<FonteDeTarefas> fontes = deFora.todas();

        VBox tela = new VBox(16, Pecas.cabecalho("de onde as demandas chegam",
                "De onde vêm as tarefas",
                "Tarefas de outros sistemas entram aqui. A mesma tarefa nunca entra duas vezes.",
                Pecas.botao("+ Nova fonte", this::abrirNovaFonte),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaTarefas.class))));

        tela.getChildren().add(Pecas.secao("Fontes cadastradas"));
        if (fontes.isEmpty()) {
            tela.getChildren().add(Pecas.vazio("Nenhuma fonte cadastrada. Cadastre a primeira "
                    + "no botão de cima."));
        }
        for (FonteDeTarefas fonte : fontes) {
            tela.getChildren().add(cartaoDaFonte(fonte));
        }
        return tela;
    }

    /** Cada fonte aberta para ajuste, com todos os campos que a página tinha. */
    private VBox cartaoDaFonte(FonteDeTarefas fonte) {
        TextField nome = new TextField(fonte.getNome());

        List<Integracao> ligacoes = deFora.integracoesPossiveis();
        ComboBox<Integracao> ligacao = new ComboBox<>();
        ligacao.getItems().addAll(ligacoes);
        ligacao.setConverter(nomeDaLigacao());
        ligacao.getSelectionModel().select(ligacoes.stream()
                .filter(i -> i.getId().equals(fonte.getIntegracaoId())).findFirst().orElse(null));
        ligacao.setMaxWidth(Double.MAX_VALUE);

        ComboBox<OperacaoIntegracao> operacao = new ComboBox<>();
        operacao.setConverter(nomeDaOperacao());
        operacao.setMaxWidth(Double.MAX_VALUE);
        atualizarOperacoes(operacao, ligacao.getValue());
        operacao.getSelectionModel().select(operacao.getItems().stream()
                .filter(o -> o.getId().equals(fonte.getOperacaoId())).findFirst().orElse(null));
        ligacao.setOnAction(acao -> atualizarOperacoes(operacao, ligacao.getValue()));

        TextField campoId = new TextField(fonte.getCampoId());
        TextField campoTitulo = new TextField(fonte.getCampoTitulo());
        TextField campoDescricao = new TextField(fonte.getCampoDescricao());
        TextField campoSituacao = new TextField(fonte.getCampoSituacao());
        TextField campoResponsavel = new TextField(fonte.getCampoResponsavel());
        TextField campoPrazo = new TextField(fonte.getCampoPrazo());
        TextField campoPrioridade = new TextField(fonte.getCampoPrioridade());
        TextField campoLink = new TextField(fonte.getCampoLink());
        TextField setor = new TextField(fonte.getSetorPadrao());

        CheckBox ativa = new CheckBox("ativa");
        ativa.setSelected(fonte.isAtiva());

        HBox linha1 = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Ligação", ligacao),
                Pecas.campo("Operação que traz a lista", operacao),
                Pecas.campo("Setor daqui", setor));
        HBox linha2 = new HBox(16, Pecas.campo("Identificador", campoId),
                Pecas.campo("Título", campoTitulo), Pecas.campo("Detalhe", campoDescricao),
                Pecas.campo("Situação", campoSituacao));
        HBox linha3 = new HBox(16, Pecas.campo("Quem faz", campoResponsavel),
                Pecas.campo("Prazo", campoPrazo),
                Pecas.campo("Prioridade", campoPrioridade),
                Pecas.campo("Link para abrir lá", campoLink));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        Label ultima = new Label(fonte.getUltimaPuxada() == null ? "nunca buscou"
                : "última busca em " + fonte.getUltimaPuxada().format(QUANDO)
                        + (fonte.getUltimoResultado() == null ? ""
                                : "  ·  " + fonte.getUltimoResultado()));
        ultima.getStyleClass().add("dica");
        ultima.setWrapText(true);

        HBox botoes = new HBox(12, ativa, Pecas.empurrar(),
                Pecas.botaoVazado("Buscar agora", () -> puxar(fonte)),
                Pecas.botao("Guardar", () -> {
                    if (ligacao.getValue() == null || operacao.getValue() == null) {
                        janela.reclamar("Escolha a ligação e a operação que traz as tarefas.");
                        return;
                    }
                    deFora.salvar(fonte.getId(), nome.getText(), ligacao.getValue().getId(),
                            operacao.getValue().getId(), campoId.getText(),
                            campoTitulo.getText(), campoDescricao.getText(),
                            campoSituacao.getText(), campoResponsavel.getText(),
                            campoPrazo.getText(), campoPrioridade.getText(),
                            campoLink.getText(), fonte.getFiltro(), setor.getText(),
                            ativa.isSelected());
                    janela.avisar("Fonte guardada.");
                    janela.ir(TelaFontesDeTarefas.class);
                }));
        botoes.setAlignment(Pos.CENTER_LEFT);

        return Pecas.caixa(linha1, linha2, linha3, ultima, botoes);
    }

    /** A fonte nova abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovaFonte() {
        List<Integracao> ligacoes = deFora.integracoesPossiveis();
        if (ligacoes.isEmpty()) {
            janela.reclamar("Nenhuma ligação de API cadastrada. Crie uma em Integrações "
                    + "antes de apontar a fonte das tarefas.");
            return;
        }

        TextField nome = new TextField();
        nome.setPromptText("tarefas do sistema matriz");

        ComboBox<Integracao> ligacao = new ComboBox<>();
        ligacao.getItems().addAll(ligacoes);
        ligacao.setConverter(nomeDaLigacao());
        ligacao.getSelectionModel().selectFirst();
        ligacao.setMaxWidth(Double.MAX_VALUE);

        ComboBox<OperacaoIntegracao> operacao = new ComboBox<>();
        operacao.setConverter(nomeDaOperacao());
        operacao.setMaxWidth(Double.MAX_VALUE);
        atualizarOperacoes(operacao, ligacao.getValue());
        ligacao.setOnAction(acao -> atualizarOperacoes(operacao, ligacao.getValue()));

        TextField setor = new TextField("Financeiro");
        TextField campoId = new TextField("id");
        TextField campoTitulo = new TextField("titulo");
        TextField campoDescricao = new TextField("descricao");
        TextField campoSituacao = new TextField("situacao");
        TextField campoResponsavel = new TextField("responsavel");
        TextField campoPrazo = new TextField("prazo");
        TextField campoPrioridade = new TextField("prioridade");
        TextField campoLink = new TextField("link");

        CheckBox ativa = new CheckBox("já começar ativa");
        ativa.setSelected(true);

        HBox linha1 = new HBox(16, Pecas.campo("Nome", nome), Pecas.campo("Ligação", ligacao),
                Pecas.campo("Operação que traz a lista", operacao),
                Pecas.campo("Setor daqui", setor));
        HBox linha2 = new HBox(16, Pecas.campo("Campo do identificador", campoId),
                Pecas.campo("Campo do título", campoTitulo),
                Pecas.campo("Campo do detalhe", campoDescricao),
                Pecas.campo("Campo da situação", campoSituacao));
        HBox linha3 = new HBox(16, Pecas.campo("Campo de quem faz", campoResponsavel),
                Pecas.campo("Campo do prazo", campoPrazo),
                Pecas.campo("Campo da prioridade", campoPrioridade),
                Pecas.campo("Campo do link", campoLink));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Nova fonte de tarefas",
                        "De onde as tarefas de outro sistema vão entrar.")
                .com(linha1, linha2, linha3, ativa)
                .acao("Criar fonte", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()
                            || operacao.getValue() == null) {
                        janela.reclamar("Escreva o nome e escolha a operação que busca as "
                                + "tarefas.");
                        return false;
                    }
                    deFora.salvar(null, nome.getText().trim(), ligacao.getValue().getId(),
                            operacao.getValue().getId(), campoId.getText(),
                            campoTitulo.getText(), campoDescricao.getText(),
                            campoSituacao.getText(), campoResponsavel.getText(),
                            campoPrazo.getText(), campoPrioridade.getText(), campoLink.getText(),
                            null, setor.getText(), ativa.isSelected());
                    janela.avisar("Fonte guardada. Use o botão de buscar para trazer agora.");
                    janela.ir(TelaFontesDeTarefas.class);
                    return true;
                })
                .abrir();
    }

    private void atualizarOperacoes(ComboBox<OperacaoIntegracao> operacao, Integracao ligacao) {
        operacao.getItems().clear();
        if (ligacao != null) {
            operacao.getItems().addAll(deFora.operacoesDe(ligacao.getId()));
            operacao.getSelectionModel().selectFirst();
        }
    }

    private void puxar(FonteDeTarefas fonte) {
        TarefasDeFora.Puxada puxada = deFora.puxar(fonte.getId());
        if (puxada.deuCerto()) {
            janela.avisar("Busca feita: " + puxada.resumo() + ".");
        } else {
            janela.reclamar(puxada.erro());
        }
        janela.atualizar();
    }

    private StringConverter<Integracao> nomeDaLigacao() {
        return new StringConverter<>() {
            @Override
            public String toString(Integracao qual) {
                return qual == null ? "" : qual.getNome();
            }

            @Override
            public Integracao fromString(String texto) {
                return null;
            }
        };
    }

    private StringConverter<OperacaoIntegracao> nomeDaOperacao() {
        return new StringConverter<>() {
            @Override
            public String toString(OperacaoIntegracao qual) {
                return qual == null ? "" : qual.getNome();
            }

            @Override
            public OperacaoIntegracao fromString(String texto) {
                return null;
            }
        };
    }
}
