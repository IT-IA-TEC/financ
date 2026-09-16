package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.AgenteIa;
import br.com.itia.financeiro.dominio.FonteDaFicha;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.LinhaDaFicha;
import br.com.itia.financeiro.dominio.PermissaoDaIa;
import br.com.itia.financeiro.dominio.UsoDaIa;
import br.com.itia.financeiro.servico.FichaDoCaso;
import br.com.itia.financeiro.servico.Inteligencia;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inteligência: o lugar de plugar um modelo de IA com coleira curta.
 *
 * Cada coisa que o modelo pode ver e cada coisa que ele pode fazer é uma chave
 * separada, e a tela mostra o que o porteiro deixaria passar com as chaves de
 * agora. Há também a lista do que ele nunca faz, que nenhuma chave libera.
 */
@Component
public class TelaInteligencia implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Inteligencia inteligencia;
    private final FichaDoCaso ficha;
    private final Janela janela;

    public TelaInteligencia(Inteligencia inteligencia, FichaDoCaso ficha, @Lazy Janela janela) {
        this.inteligencia = inteligencia;
        this.ficha = ficha;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "inteligencia";
    }

    @Override
    public Node montar() {
        AgenteIa agente = inteligencia.daEmpresa();
        Map<PermissaoDaIa, Boolean> chaves = inteligencia.chaves();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("modelo com coleira curta", "Inteligência",
                "Um modelo de IA só faz o que a chave dele deixa. E há coisas que nenhuma "
                        + "chave libera.",
                Pecas.quadrosDoTopo(
                Pecas.quadro("Situação", agente.isAtivo() ? "ligado" : "desligado",
                        agente.getModoLegivel(), true, false),
                Pecas.quadro("Chaves ligadas",
                        String.valueOf(chaves.values().stream()
                                .filter(Boolean.TRUE::equals).count()),
                        "de " + chaves.size() + " possíveis"),
                Pecas.quadro("Usos", String.valueOf(inteligencia.quantosUsos()),
                        inteligencia.quantasAcoes() + " viraram ação"),
                Pecas.quadro("Teto", agente.getTetoValor() == null
                                || agente.getTetoValor().signum() == 0 ? "sem teto"
                                : Pecas.dinheiro(agente.getTetoValor()),
                        agente.getTetoMensagensDia() + " mensagem(ns) por dia"))));

        tela.getChildren().add(Pecas.aviso("O modelo não tem acesso a nada por padrão. Cada "
                + "coisa que ele pode ver e cada coisa que ele pode fazer é uma chave ligada "
                + "aqui, e a chave é conferida duas vezes: na hora de montar o que vai para ele "
                + "(o que está desligado nem chega lá) e na hora de fazer (sem chave, a ação é "
                + "recusada e o motivo fica gravado)."));

        tela.getChildren().add(Pecas.secao("O que ele nunca faz, com chave nenhuma"));
        tela.getChildren().add(nuncaFaz());

        tela.getChildren().add(Pecas.secao("Como ele está ligado"));
        tela.getChildren().add(comoEstaLigado(agente));

        tela.getChildren().add(Pecas.secao("O que ele pode ver"));
        tela.getChildren().add(tabelaDeChaves(PermissaoDaIa.leituras(), chaves, "Pode ver"));

        tela.getChildren().add(Pecas.secao("O que ele pode fazer"));
        tela.getChildren().add(tabelaDeChaves(PermissaoDaIa.acoes(), chaves, "Pode fazer"));

        tela.getChildren().add(Pecas.secao("O que aconteceria agora, se o modelo tentasse"));
        tela.getChildren().add(travas());

        tela.getChildren().add(Pecas.cabecalho("ficha do caso",
                "A ficha do caso desta empresa",
                "As linhas que aparecem na mesa de cobrança, na ordem que você escolher. Cada "
                        + "número sai do que está gravado: nenhuma linha é digitada por alguém.",
                Pecas.botao("+ Acrescentar linha", this::abrirNovaLinha)));
        tela.getChildren().add(linhasDaFicha());

        tela.getChildren().add(Pecas.secao("O que o modelo leu e fez"));
        tela.getChildren().add(usos());
        return tela;
    }

    // ------------------------------------------------------------- nunca faz

    private VBox nuncaFaz() {
        FlowPane lista = new FlowPane(8, 8);
        for (String proibido : PermissaoDaIa.NUNCA) {
            Label marca = new Label(proibido);
            marca.getStyleClass().addAll("marca-situacao", "s-vencido");
            lista.getChildren().add(marca);
        }
        Label explicacao = new Label("Isto não é configuração: nenhuma chave libera o que está "
                + "nessa lista.");
        explicacao.getStyleClass().add("dica");
        return Pecas.caixa(lista, explicacao);
    }

    // --------------------------------------------------------- configuração

    private VBox comoEstaLigado(AgenteIa agente) {
        ComboBox<Integracao> ligacao = new ComboBox<>();
        ligacao.getItems().add(null);
        ligacao.getItems().addAll(inteligencia.integracoesDisponiveis());
        ligacao.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integracao qual) {
                return qual == null ? "nenhuma escolhida" : qual.getNome();
            }

            @Override
            public Integracao fromString(String texto) {
                return null;
            }
        });
        ligacao.getSelectionModel().select(inteligencia.integracoesDisponiveis().stream()
                .filter(i -> i.getId().equals(agente.getIntegracaoId())).findFirst()
                .orElse(null));
        ligacao.setMaxWidth(Double.MAX_VALUE);

        TextField modelo = new TextField(agente.getModelo());
        modelo.setPromptText("como está na documentação do provedor");

        ComboBox<String> modo = new ComboBox<>();
        modo.getItems().addAll(AgenteIa.MODOS);
        modo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String qual) {
                if (qual == null) {
                    return "";
                }
                return switch (qual) {
                    case "RESPONDE_COM_REVISAO" -> "escreve, alguém revisa e manda";
                    case "RESPONDE_SOZINHO" -> "responde sozinho";
                    default -> "só sugere, não fala com ninguém";
                };
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        modo.getSelectionModel().select(agente.getModo() == null ? "SO_SUGERE" : agente.getModo());
        modo.setMaxWidth(Double.MAX_VALUE);

        TextField teto = new TextField(Pecas.numero(agente.getTetoValor()));
        TextField porDia = new TextField(String.valueOf(agente.getTetoMensagensDia()));

        CheckBox ativo = new CheckBox("ligado");
        ativo.setSelected(agente.isAtivo());

        TextArea instrucao = new TextArea(agente.getInstrucao());
        instrucao.setPromptText("como falar, o que nunca oferecer, o que sempre lembrar");
        instrucao.setPrefRowCount(4);
        instrucao.setWrapText(true);

        HBox primeira = new HBox(16, Pecas.campo("Modelo (integração)", ligacao),
                Pecas.campo("Nome do modelo", modelo), Pecas.campo("Até onde ele vai", modo));
        HBox segunda = new HBox(16,
                Pecas.campo("Teto de valor que ele pode propor", teto),
                Pecas.campo("Mensagens por dia, por cliente", porDia));
        primeira.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        segunda.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(primeira, segunda, ativo,
                Pecas.campo("O que esta empresa quer dele", instrucao),
                new HBox(Pecas.botao("Guardar", () -> {
                    BigDecimal quanto = TelaTituloNovo.dinheiro(teto.getText());
                    int quantas;
                    try {
                        quantas = Integer.parseInt(porDia.getText().trim());
                    } catch (RuntimeException naoEhNumero) {
                        quantas = agente.getTetoMensagensDia();
                    }
                    inteligencia.salvar(ligacao.getValue() == null ? null
                                    : ligacao.getValue().getId(), modelo.getText(),
                            ativo.isSelected(), modo.getValue(), instrucao.getText(),
                            quanto == null ? BigDecimal.ZERO : quanto, quantas);
                    janela.avisar("Configuração guardada.");
                    janela.ir(TelaInteligencia.class);
                })));
    }

    // ------------------------------------------------------------- as chaves

    private VBox tabelaDeChaves(List<PermissaoDaIa> quais, Map<PermissaoDaIa, Boolean> chaves,
                                String titulo) {
        VBox lista = new VBox(10);
        Label cabeca = new Label("LIGA  ·  " + titulo.toUpperCase() + "  ·  O QUE É  ·  POR QUÊ");
        cabeca.getStyleClass().add("titulo-secao");
        lista.getChildren().add(cabeca);

        for (PermissaoDaIa chave : quais) {
            CheckBox marca = new CheckBox(chave.getRotulo());
            marca.setSelected(Boolean.TRUE.equals(chaves.get(chave)));
            marca.setOnAction(acao -> {
                inteligencia.mexerNaChave(chave, marca.isSelected());
                janela.atualizar();
            });

            Label oQueE = new Label(chave.getExplicacao());
            oQueE.getStyleClass().add("texto");
            oQueE.setWrapText(true);

            Label porQue = new Label(chave.getMotivo());
            porQue.getStyleClass().add("dica");
            porQue.setWrapText(true);

            lista.getChildren().add(new VBox(3, marca, oQueE, porQue));
        }
        return Pecas.caixa(lista);
    }

    private Node travas() {
        UUID alguem = inteligencia.alguemParaTestar();
        Label explicacao = new Label("Esta tabela não executa nada: ela pergunta ao porteiro, "
                + "ação por ação, o que ele deixaria passar com as chaves de agora.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);

        if (alguem == null) {
            return Pecas.caixa(explicacao,
                    Pecas.vazio("Sem cliente cadastrado ainda, não dá para perguntar ao "
                            + "porteiro."));
        }
        List<Inteligencia.Trava> travas = inteligencia.testarTravas(alguem, BigDecimal.ZERO);
        return Pecas.caixa(explicacao, Tabela.de(travas)
                .coluna("Se o modelo tentar", t -> t.acao().getRotulo(), 2)
                .coluna("Motivo", t -> t.motivo() == null ? "chave ligada e dentro dos tetos"
                        : t.motivo(), 3)
                .comMarca(t -> t.deixa() ? "deixa" : "barra",
                        t -> t.deixa() ? "s-pago" : "s-vencido")
                .quandoVazia("Nada para conferir.")
                .montar());
    }

    // ---------------------------------------------------------- ficha do caso

    private VBox linhasDaFicha() {
        List<LinhaDaFicha> linhas = ficha.configuracao();
        VBox todas = new VBox(8);
        for (LinhaDaFicha linha : linhas) {
            TextField ordem = new TextField(String.valueOf(linha.getOrdem()));
            ordem.setPrefWidth(70);

            TextField rotulo = new TextField(linha.getRotulo());

            Label origem = new Label(linha.daFonte().getExplicacao());
            origem.getStyleClass().add("dica");
            origem.setWrapText(true);

            CheckBox mostra = new CheckBox("mostra");
            mostra.setSelected(linha.isAtiva());

            HBox campos = new HBox(12, Pecas.campo("Ordem", ordem),
                    Pecas.campo("Como aparece", rotulo), mostra,
                    Pecas.empurrar(),
                    Pecas.botaoVazado("Guardar", () -> {
                        ficha.ajustar(linha.getId(), inteiro(ordem.getText(), linha.getOrdem()),
                                rotulo.getText(), mostra.isSelected());
                        janela.avisar("Linha guardada.");
                        janela.ir(TelaInteligencia.class);
                    }),
                    Pecas.botaoPerigo("Tirar", () -> {
                        ficha.tirar(linha.getId());
                        janela.avisar("Linha tirada da ficha.");
                        janela.ir(TelaInteligencia.class);
                    }));
            campos.setAlignment(Pos.BOTTOM_LEFT);
            HBox.setHgrow(campos.getChildren().get(1), Priority.ALWAYS);

            todas.getChildren().add(Pecas.caixa(campos, origem));
        }
        if (linhas.isEmpty()) {
            todas.getChildren().add(Pecas.vazio("Nenhuma linha configurada."));
        }
        return todas;
    }

    private void abrirNovaLinha() {
        ComboBox<FonteDaFicha> fonte = new ComboBox<>();
        fonte.getItems().addAll(FonteDaFicha.todas());
        fonte.setConverter(new StringConverter<>() {
            @Override
            public String toString(FonteDaFicha qual) {
                return qual == null ? "" : qual.getRotuloPadrao() + " · " + qual.getExplicacao();
            }

            @Override
            public FonteDaFicha fromString(String texto) {
                return null;
            }
        });
        fonte.getSelectionModel().selectFirst();
        fonte.setMaxWidth(Double.MAX_VALUE);

        TextField rotulo = new TextField();
        rotulo.setPromptText("como você quer chamar esta linha");

        JanelaFlutuante.estreita(janela.palco(), "Acrescentar linha",
                        "Mais uma linha na ficha que aparece na mesa de cobrança.")
                .com(Pecas.campo("De onde sai", fonte), Pecas.campo("Como aparece", rotulo))
                .acao("Acrescentar", () -> {
                    if (fonte.getValue() == null) {
                        janela.reclamar("Escolha de onde sai o número.");
                        return false;
                    }
                    ficha.acrescentar(fonte.getValue(), rotulo.getText());
                    janela.avisar("Linha acrescentada.");
                    janela.ir(TelaInteligencia.class);
                    return true;
                })
                .abrir();
    }

    // ----------------------------------------------------------------- usos

    private Node usos() {
        List<UsoDaIa> usos = inteligencia.ultimosUsos();
        return Tabela.de(usos)
                .coluna("Quando", u -> u.getOcorridoEm() == null ? ""
                        : u.getOcorridoEm().format(QUANDO), 0.9)
                .coluna("O que foi", u -> u.getTipo() == null ? ""
                        : u.getTipo().toLowerCase().replace('_', ' '), 1.2)
                .coluna("Chaves usadas", UsoDaIa::getPermissoes, 2)
                .coluna("Resultado", u -> u.getRecusa() != null ? u.getRecusa()
                        : u.getResposta(), 2.4)
                .comMarca(u -> u.getRecusa() != null ? "recusado"
                                : u.isExecutada() ? "feito" : "lido",
                        u -> u.getRecusa() != null ? "s-vencido"
                                : u.isExecutada() ? "s-pago" : "s-aberto")
                .quandoVazia("Nenhum uso registrado ainda.")
                .montar();
    }

    private int inteiro(String texto, int padrao) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return padrao;
        }
    }
}
