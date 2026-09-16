package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ExecucaoDoFluxo;
import br.com.itia.financeiro.dominio.Fluxo;
import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import br.com.itia.financeiro.dominio.PassoDoFluxo;
import br.com.itia.financeiro.servico.Fluxos;
import br.com.itia.financeiro.servico.Mensagens;
import javafx.geometry.Pos;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fluxos: quando acontecer isso, faça aquilo.
 *
 * Cada fluxo tem um gatilho, condições e ações. Tudo o que ele faz fica
 * registrado, para ninguém precisar adivinhar por que uma mensagem saiu.
 */
@Component
public class TelaFluxos implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Fluxos fluxos;
    private final Mensagens mensagens;
    private final Janela janela;

    public TelaFluxos(Fluxos fluxos, Mensagens mensagens, @Lazy Janela janela) {
        this.fluxos = fluxos;
        this.mensagens = mensagens;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        List<Fluxo> lista = fluxos.todos();
        List<ExecucaoDoFluxo> execucoes = fluxos.ultimasExecucoes();

        long ligados = lista.stream().filter(Fluxo::isAtivo).count();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("quando, se, faça", "Fluxos",
                "Quando acontecer isso, faça aquilo. Tudo o que o fluxo faz fica registrado.",
                Pecas.botao("+ Novo fluxo", this::abrirNovoFluxo),
                Pecas.botaoVazado("Desligar um fluxo", this::abrirDesligar),
                Pecas.botaoVazado("Rodar agora", () -> {
                    int quantos = fluxos.rodarHoje();
                    janela.avisar(quantos == 0 ? "Nenhum fluxo tinha o que fazer hoje."
                            : quantos + " fluxo(s) rodaram agora.");
                    janela.atualizar();
                }),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Ligados", String.valueOf(ligados), "rodam sozinhos de manhã",
                        true, false),
                Pecas.quadro("Montados", String.valueOf(lista.size()), "no total"),
                Pecas.quadro("Ações recentes", String.valueOf(execucoes.size()),
                        "nas últimas rodadas"),
                Pecas.quadro("Rodada", "08h20", "logo depois da régua"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Fluxos"));

        tela.getChildren().add(Pecas.secao("Fluxos cadastrados"));
        if (lista.isEmpty()) {
            tela.getChildren().add(Pecas.vazio("Nenhum fluxo montado ainda."));
        }
        for (Fluxo fluxo : lista) {
            tela.getChildren().add(cartaoDoFluxo(fluxo));
        }

        tela.getChildren().add(Pecas.secao("O que os fluxos já fizeram"));
        tela.getChildren().add(Tabela.de(execucoes)
                .coluna("Quando", e -> e.getOcorridoEm() == null ? ""
                        : e.getOcorridoEm().format(QUANDO))
                .coluna("O que aconteceu", ExecucaoDoFluxo::getResultado, 3)
                .quandoVazia("Nenhum fluxo rodou ainda.")
                .montar());
        return tela;
    }

    /** A criação de fluxo abre por cima da tela, sem ocupar espaço na página. */
    private void abrirNovoFluxo() {
        TextField nome = new TextField();
        nome.setPromptText("avisar quando o pagamento entrar");
        TextField descricao = new TextField();
        descricao.setPromptText("o que este fluxo faz");

        ComboBox<String> gatilho = new ComboBox<>();
        gatilho.getItems().addAll("PAGAMENTO_ENTROU", "ACORDO_QUEBRADO", "CLIENTE_RESPONDEU",
                "DEPOIS_DE_VENCER", "ANTES_DE_VENCER");
        gatilho.getSelectionModel().selectFirst();
        gatilho.setMaxWidth(Double.MAX_VALUE);

        TextField dias = new TextField("0");

        HBox campos = new HBox(16,
                Pecas.campo("Nome", nome),
                Pecas.campo("Para que serve", descricao),
                Pecas.campo("Quando", gatilho),
                Pecas.campo("Dias", dias));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Novo fluxo",
                        "Quando acontecer isso, o sistema faz aquilo sozinho.")
                .com(campos)
                .acao("Criar o fluxo", () -> {
            if (nome.getText() == null || nome.getText().isBlank()) {
                janela.reclamar("Escreva o nome do fluxo.");
                return false;
            }
            fluxos.salvar(null, nome.getText().trim(),
                    descricao.getText() == null || descricao.getText().isBlank() ? null
                            : descricao.getText().trim(),
                    gatilho.getValue(), inteiro(dias.getText()), true);
            janela.avisar("Fluxo criado. Acrescente as condições e as ações dele.");
            janela.ir(TelaFluxos.class);
            return true;
                })
                .abrir();
    }

    /** O desligamento também abre por cima, em vez de ficar solto na página. */
    private void abrirDesligar() {
        List<Fluxo> lista = fluxos.todos();
        ComboBox<Fluxo> qual = new ComboBox<>();
        qual.getItems().addAll(lista.stream().filter(Fluxo::isAtivo).toList());
        qual.setConverter(new StringConverter<>() {
            @Override
            public String toString(Fluxo fluxo) {
                return fluxo == null ? "" : fluxo.getNome();
            }

            @Override
            public Fluxo fromString(String texto) {
                return null;
            }
        });
        qual.getSelectionModel().selectFirst();
        qual.setMaxWidth(Double.MAX_VALUE);

        HBox linha = new HBox(16, Pecas.campo("Fluxo", qual));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Desligar um fluxo",
                        "Ele para de agir, mas o histórico fica.")
                .com(linha)
                .acao("Desligar", () -> {
                    if (qual.getValue() == null) {
                        janela.reclamar("Escolha o fluxo que vai ser desligado.");
                        return false;
                    }
                    fluxos.desativar(qual.getValue().getId());
                    janela.avisar("Fluxo desligado. Ele para de agir, mas o histórico fica.");
                    janela.ir(TelaFluxos.class);
                    return true;
                })
                .abrir();
    }

    /** Um fluxo com a receita dele: as condições e as ações, na ordem. */
    private VBox cartaoDoFluxo(Fluxo fluxo) {
        Label nome = new Label(fluxo.getNome());
        nome.getStyleClass().add("titulo-item");

        Label quando = new Label(fluxo.getQuandoLegivel());
        quando.getStyleClass().add("dica");

        Label detalhe = new Label(fluxo.getDescricao() == null ? "" : fluxo.getDescricao());
        detalhe.getStyleClass().add("dica");
        detalhe.setWrapText(true);

        Label situacao = new Label(fluxo.isAtivo() ? "ligado" : "desligado");
        situacao.getStyleClass().addAll("marca-situacao",
                fluxo.isAtivo() ? "s-pago" : "s-cancelado");

        VBox esquerda = new VBox(3, nome, quando, detalhe);
        HBox cabeca = new HBox(12, esquerda, Pecas.empurrar(), situacao);
        cabeca.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(esquerda, Priority.ALWAYS);

        VBox receita = new VBox(6);
        List<PassoDoFluxo> passos = fluxos.passosDe(fluxo.getId());
        for (PassoDoFluxo passo : passos) {
            Label tipo = new Label(passo.ehCondicao() ? "se" : "faça");
            tipo.getStyleClass().add("marca-situacao");

            Label resumo = new Label(passo.getResumo());
            resumo.setWrapText(true);

            HBox linha = new HBox(10, tipo, resumo, Pecas.empurrar(),
                    Pecas.botaoVazado("tirar", () -> {
                        fluxos.apagarPasso(fluxo.getId(), passo.getId());
                        janela.avisar("Passo tirado do fluxo.");
                        janela.ir(TelaFluxos.class);
                    }));
            linha.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(resumo, Priority.ALWAYS);
            receita.getChildren().add(linha);
        }
        if (passos.isEmpty()) {
            Label nada = new Label("este fluxo ainda não faz nada: acrescente uma ação");
            nada.getStyleClass().add("dica");
            receita.getChildren().add(nada);
        }

        HBox botoes = new HBox(12,
                Pecas.botaoVazado("+ Condição", () -> abrirCondicao(fluxo)),
                Pecas.botao("+ Ação", () -> abrirAcao(fluxo)));
        if (fluxo.isAtivo()) {
            botoes.getChildren().add(Pecas.botaoPerigo("Desligar", () -> {
                fluxos.desativar(fluxo.getId());
                janela.avisar("Fluxo desligado.");
                janela.ir(TelaFluxos.class);
            }));
        }
        return Pecas.caixa(cabeca, receita, botoes);
    }

    /** A condição: o fluxo só faz se isso for verdade. */
    private void abrirCondicao(Fluxo fluxo) {
        ComboBox<String> campo = new ComboBox<>();
        campo.getItems().addAll("VALOR_EM_ABERTO", "DIAS_ATRASO", "SITUACAO_DO_CASO",
                "TOM_DO_CLIENTE");
        campo.setConverter(legivel());
        campo.getSelectionModel().selectFirst();
        campo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> operador = new ComboBox<>();
        operador.getItems().addAll("MAIOR", "MENOR", "IGUAL", "DIFERENTE");
        operador.setConverter(legivel());
        operador.getSelectionModel().selectFirst();
        operador.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField();
        valor.setPromptText("500");

        HBox campos = new HBox(16, Pecas.campo("O que conferir", campo),
                Pecas.campo("Como", operador), Pecas.campo("Valor", valor));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Nova condição", "Só faz se.")
                .com(campos)
                .acao("Acrescentar", () -> {
                    if (valor.getText() == null || valor.getText().isBlank()) {
                        janela.reclamar("Escreva o valor da condição.");
                        return false;
                    }
                    fluxos.acrescentarCondicao(fluxo.getId(), campo.getValue(),
                            operador.getValue(), valor.getText().trim());
                    janela.avisar("Condição acrescentada.");
                    janela.ir(TelaFluxos.class);
                    return true;
                })
                .abrir();
    }

    /** A ação: o que o fluxo faz quando as condições batem. */
    private void abrirAcao(Fluxo fluxo) {
        ComboBox<String> acao = new ComboBox<>();
        acao.getItems().addAll("MANDAR_MENSAGEM", "MARCAR_CASO", "ANOTAR_PROXIMA_ACAO",
                "REGISTRAR_OBSERVACAO");
        acao.setConverter(legivel());
        acao.getSelectionModel().selectFirst();
        acao.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ModeloDeMensagem> modelo = new ComboBox<>();
        modelo.getItems().add(null);
        modelo.getItems().addAll(mensagens.modelosAtivos());
        modelo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModeloDeMensagem qual) {
                return qual == null ? "não é mensagem" : qual.getNome();
            }

            @Override
            public ModeloDeMensagem fromString(String texto) {
                return null;
            }
        });
        modelo.getSelectionModel().selectFirst();
        modelo.setMaxWidth(Double.MAX_VALUE);

        TextField dias = new TextField("0");

        TextField texto = new TextField();
        texto.setPromptText("para marcar o caso use: EM_COBRANCA, PROMESSA, EM_ACORDO, "
                + "CONTESTADO, PARADO ou JURIDICO");

        HBox campos = new HBox(16, Pecas.campo("O que fazer", acao),
                Pecas.campo("Texto da mensagem", modelo),
                Pecas.campo("Em quantos dias", dias));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Nova ação", "Então faça.")
                .com(campos, Pecas.campo("O que escrever", texto))
                .acao("Acrescentar", () -> {
                    fluxos.acrescentarAcao(fluxo.getId(), acao.getValue(),
                            modelo.getValue() == null ? null : modelo.getValue().getId(),
                            texto.getText(), inteiro(dias.getText()));
                    janela.avisar("Ação acrescentada.");
                    janela.ir(TelaFluxos.class);
                    return true;
                })
                .abrir();
    }

    /** Troca o nome de máquina pelo nome de gente na lista de escolha. */
    private StringConverter<String> legivel() {
        return new StringConverter<>() {
            @Override
            public String toString(String qual) {
                return qual == null ? "" : qual.toLowerCase().replace('_', ' ');
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        };
    }

    private int inteiro(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return 0;
        }
    }
}
