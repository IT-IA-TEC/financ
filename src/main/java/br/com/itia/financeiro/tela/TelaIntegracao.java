package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.EventoIntegracao;
import br.com.itia.financeiro.dominio.Integracao;
import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import br.com.itia.financeiro.dominio.TipoAutenticacao;
import br.com.itia.financeiro.dominio.TipoIntegracao;
import br.com.itia.financeiro.servico.IntegracaoServico;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uma ligação com o mundo de fora: endereço, chave, o que ela sabe fazer, por
 * onde recebe aviso e tudo o que já passou por ela.
 *
 * O segredo nunca reaparece na tela. Campo de segredo em branco quer dizer
 * "mantém o que já estava".
 */
@Component
public class TelaIntegracao implements TelaDeUmSo {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final IntegracaoServico integracoes;
    private final Janela janela;

    private UUID qual;

    public TelaIntegracao(IntegracaoServico integracoes, @Lazy Janela janela) {
        this.integracoes = integracoes;
        this.janela = janela;
    }

    @Override
    public void escolher(UUID id) {
        this.qual = id;
    }

    @Override
    public String secao() {
        return "integracoes";
    }

    @Override
    public Node montar() {
        Integracao integracao = integracoes.buscar(qual);
        List<String> faltando = integracoes.faltando(integracao);
        Map<String, String> guardado = integracoes.paraMostrar(integracao);

        Label situacao = new Label(integracao.isAtiva() ? "ligada" : "desligada");
        situacao.getStyleClass().addAll("marca-situacao",
                integracao.isAtiva() ? "s-pago" : "s-cancelado");

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("integração", integracao.getNome(),
                integracao.getProvedor() == null ? "" : integracao.getProvedor().name(),
                situacao,
                Pecas.botaoVazado("Testar conexão", this::testar),
                integracao.isAtiva()
                        ? Pecas.botaoPerigo("Desligar", () -> {
                            integracoes.desligar(qual);
                            janela.avisar("Ligação desligada.");
                            janela.ir(TelaIntegracao.class, qual);
                        })
                        : Pecas.botaoVazado("Ligar", () -> {
                            integracoes.ligar(qual);
                            janela.avisar("Ligação ligada.");
                            janela.ir(TelaIntegracao.class, qual);
                        }),
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaIntegracoes.class))));

        if (!faltando.isEmpty()) {
            VBox falta = new VBox(6);
            falta.getStyleClass().add("caixa");
            Label titulo = new Label("Falta preencher para esta ligação funcionar:");
            titulo.getStyleClass().add("dica");
            falta.getChildren().add(titulo);
            for (String item : faltando) {
                falta.getChildren().add(new Label("· " + item));
            }
            tela.getChildren().add(falta);
        }

        if (integracao.getUltimaChecagemEm() != null) {
            Label checagem = new Label("Última checagem: "
                    + integracao.getUltimaChecagemEm().format(QUANDO) + "  ·  "
                    + (Boolean.TRUE.equals(integracao.getUltimaChecagemOk()) ? "respondeu"
                            : "não respondeu: " + integracao.getUltimaChecagemErro()));
            checagem.getStyleClass().add("dica");
            checagem.setWrapText(true);
            tela.getChildren().add(checagem);
        }

        tela.getChildren().add(Pecas.secao("Configuração"));
        tela.getChildren().add(configuracao(integracao, guardado));

        tela.getChildren().add(Pecas.cabecalho("chamadas", "Operações",
                "Cada operação é uma chamada que esta integração pode fazer. Nada sai daqui sem "
                        + "estar nesta lista.",
                Pecas.botao("+ Nova operação", this::abrirNovaOperacao)));
        tela.getChildren().add(operacoes());

        if (integracao.getTipo() != null && integracao.getCaminhoDoWebhook() != null) {
            tela.getChildren().add(Pecas.secao("Endereço de recebimento"));
            tela.getChildren().add(enderecoDeRecebimento(integracao));
        }

        tela.getChildren().add(Pecas.secao("Movimento desta integração"));
        tela.getChildren().add(movimento());
        return tela;
    }

    // ------------------------------------------------------------ configuração

    private VBox configuracao(Integracao integracao, Map<String, String> guardado) {
        TextField nome = new TextField(integracao.getNome());
        TextField ambiente = new TextField(integracao.getAmbiente());
        TextField endereco = new TextField(integracao.getBaseUrl());
        endereco.setPromptText("https://...");

        ComboBox<TipoIntegracao> tipo = new ComboBox<>();
        tipo.getItems().addAll(TipoIntegracao.values());
        tipo.getSelectionModel().select(integracao.getTipo());
        tipo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TipoAutenticacao> autenticacao = new ComboBox<>();
        autenticacao.getItems().addAll(TipoAutenticacao.values());
        autenticacao.getSelectionModel().select(integracao.getAutenticacao());
        autenticacao.setMaxWidth(Double.MAX_VALUE);

        TextField tempoLimite = new TextField(texto(integracao.getTempoLimiteSegundos()));
        TextField tentativas = new TextField(texto(integracao.getTentativas()));
        TextField espera = new TextField(texto(integracao.getEsperaEntreTentativasMs()));

        TextArea observacao = new TextArea(integracao.getObservacao());
        observacao.setPromptText("para que essa ligação serve aqui");
        observacao.setPrefRowCount(2);

        Map<String, TextField> campos = new LinkedHashMap<>();
        VBox acesso = new VBox(10);
        for (TipoAutenticacao.Campo campo : integracao.getCamposDeAcesso()) {
            TextField valor = new TextField(campo.segredo() ? "" : guardado.get(campo.chave()));
            if (campo.segredo()) {
                valor.setPromptText("guardado: " + guardado.get(campo.chave())
                        + " · deixe em branco para manter");
            }
            campos.put(campo.chave(), valor);
            acesso.getChildren().add(Pecas.campo(campo.rotulo(), valor));
        }
        Label aviso = new Label("Segredo salvo não volta a aparecer: mostramos só os últimos "
                + "dígitos. Deixando o campo em branco, o que já estava continua valendo.");
        aviso.getStyleClass().add("dica");
        aviso.setWrapText(true);

        CheckBox assinatura = new CheckBox("Conferir assinatura do aviso");
        assinatura.setSelected(integracao.isVerificarAssinatura());

        HBox primeira = new HBox(16, Pecas.campo("Nome", nome),
                Pecas.campo("Ambiente", ambiente), Pecas.campo("Endereço base", endereco));
        HBox segunda = new HBox(16, Pecas.campo("Tipo de ligação", tipo),
                Pecas.campo("Forma de acesso", autenticacao),
                Pecas.campo("Tempo limite (segundos)", tempoLimite),
                Pecas.campo("Tentativas", tentativas),
                Pecas.campo("Espera entre tentativas (ms)", espera));
        primeira.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        segunda.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        return Pecas.caixa(primeira, segunda,
                Pecas.campo("Para que essa ligação serve aqui", observacao),
                acesso, aviso, assinatura,
                new HBox(12, Pecas.botao("Guardar", () -> {
                    Map<String, String> informados = new LinkedHashMap<>();
                    campos.forEach((chave, campo) -> informados.put(chave, campo.getText()));
                    integracoes.salvar(qual, nome.getText(), ambiente.getText(),
                            endereco.getText(), observacao.getText(), tipo.getValue(),
                            autenticacao.getValue(), numero(tempoLimite.getText()),
                            numero(tentativas.getText()), numero(espera.getText()),
                            assinatura.isSelected(), informados);
                    janela.avisar("Configuração salva.");
                    janela.ir(TelaIntegracao.class, qual);
                })));
    }

    private void testar() {
        boolean respondeu = integracoes.testar(qual);
        if (respondeu) {
            janela.avisar("A ligação respondeu.");
        } else {
            janela.reclamar("A ligação não respondeu. O motivo está registrado na checagem.");
        }
        janela.ir(TelaIntegracao.class, qual);
    }

    // -------------------------------------------------------------- operações

    private Node operacoes() {
        List<OperacaoIntegracao> lista = integracoes.operacoesDe(qual);
        if (lista.isEmpty()) {
            return Pecas.vazio("Nenhuma operação cadastrada. Cadastre a primeira no botão "
                    + "de cima.");
        }

        VBox todas = new VBox(10);
        for (OperacaoIntegracao operacao : lista) {
            Label nome = new Label(operacao.getNome());
            nome.getStyleClass().add("titulo-item");

            Label chamada = new Label(operacao.getVerbo() + "  " + operacao.getCaminho());
            chamada.getStyleClass().add("dica");

            Label paraQue = new Label(operacao.getParaQue() == null ? "" : operacao.getParaQue());
            paraQue.getStyleClass().add("dica");
            paraQue.setWrapText(true);

            VBox esquerda = new VBox(3, nome, chamada, paraQue);
            HBox linha = new HBox(12, esquerda, Pecas.empurrar(),
                    Pecas.botaoVazado("Executar", () -> executar(operacao)),
                    Pecas.botaoPerigo("Remover", () -> remover(operacao)));
            linha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(esquerda, Priority.ALWAYS);
            todas.getChildren().add(Pecas.caixa(linha));
        }
        return todas;
    }

    private void executar(OperacaoIntegracao operacao) {
        TextArea corpo = new TextArea(operacao.getCorpoModelo());
        corpo.setPromptText("o que vai junto na chamada, quando precisa");
        corpo.setPrefRowCount(4);

        JanelaFlutuante.estreita(janela.palco(), "Executar " + operacao.getNome(),
                        operacao.getVerbo() + " " + operacao.getCaminho())
                .com(Pecas.campo("Corpo da chamada", corpo))
                .acao("Executar", () -> {
                    integracoes.executar(qual, operacao.getId(), corpo.getText());
                    janela.avisar("Chamada feita. O que voltou está no movimento, aqui embaixo.");
                    janela.ir(TelaIntegracao.class, qual);
                    return true;
                })
                .abrir();
    }

    private void remover(OperacaoIntegracao operacao) {
        Label aviso = new Label("A operação " + operacao.getNome() + " deixa de existir. O que "
                + "já foi chamado por ela continua registrado no movimento.");
        aviso.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Remover operação", null)
                .com(aviso)
                .acao("Remover", () -> {
                    integracoes.apagarOperacao(qual, operacao.getId());
                    janela.avisar("Operação removida.");
                    janela.ir(TelaIntegracao.class, qual);
                    return true;
                })
                .abrir();
    }

    private void abrirNovaOperacao() {
        TextField nome = new TextField();
        nome.setPromptText("Consultar extrato");

        ComboBox<String> verbo = new ComboBox<>();
        verbo.getItems().addAll("GET", "POST", "PUT", "PATCH", "DELETE");
        verbo.getSelectionModel().selectFirst();
        verbo.setMaxWidth(Double.MAX_VALUE);

        TextField caminho = new TextField();
        caminho.setPromptText("/v1/extrato");

        TextField paraQue = new TextField();
        paraQue.setPromptText("traz o movimento da conta");

        HBox campos = new HBox(16, Pecas.campo("O que ela faz", nome),
                Pecas.campo("Verbo", verbo), Pecas.campo("Caminho", caminho),
                Pecas.campo("Para que serve aqui", paraQue));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.estreita(janela.palco(), "Nova operação",
                        "Os caminhos que vieram do modelo são sugestão: confira na documentação "
                                + "do provedor.")
                .com(campos)
                .acao("Cadastrar operação", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()
                            || caminho.getText() == null || caminho.getText().isBlank()) {
                        janela.reclamar("Escreva o que ela faz e o caminho.");
                        return false;
                    }
                    integracoes.criarOperacao(qual, nome.getText().trim(), verbo.getValue(),
                            caminho.getText().trim(), paraQue.getText());
                    janela.avisar("Operação cadastrada.");
                    janela.ir(TelaIntegracao.class, qual);
                    return true;
                })
                .abrir();
    }

    // ------------------------------------------------- endereço de recebimento

    private VBox enderecoDeRecebimento(Integracao integracao) {
        Label explicacao = new Label("É este endereço que o outro sistema chama para avisar o "
                + "que aconteceu. O trecho final é secreto: quem não tiver ele não entrega nada "
                + "aqui. A porta só aceita aviso com a integração ligada.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);

        Label endereco = new Label(integracao.getCaminhoDoWebhook());
        endereco.getStyleClass().add("titulo-item");
        endereco.setWrapText(true);

        return Pecas.caixa(explicacao, endereco,
                new HBox(12, Pecas.botaoVazado("Trocar endereço", this::trocarEndereco)));
    }

    private void trocarEndereco() {
        Label aviso = new Label("O endereço antigo para de funcionar na hora. Quem já avisava "
                + "por ele precisa ser atualizado com o endereço novo.");
        aviso.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Trocar endereço", null)
                .com(aviso)
                .acao("Trocar endereço", () -> {
                    integracoes.trocarSegredo(qual);
                    janela.avisar("Endereço trocado.");
                    janela.ir(TelaIntegracao.class, qual);
                    return true;
                })
                .abrir();
    }

    // -------------------------------------------------------------- movimento

    private Node movimento() {
        List<EventoIntegracao> eventos = integracoes.eventosDe(qual);
        return Tabela.de(eventos)
                .coluna("Quando", e -> e.getOcorridoEm() == null ? ""
                        : e.getOcorridoEm().format(QUANDO), 0.9)
                .coluna("Direção", EventoIntegracao::getDirecao, 0.7)
                .coluna("Operação", EventoIntegracao::getTipo, 1.2)
                .coluna("Referência", EventoIntegracao::getReferencia, 1.2)
                .coluna("Conteúdo", e -> e.getErro() != null ? e.getErro()
                        : e.getResumoDaCarga(), 2.4)
                .comMarca(EventoIntegracao::getStatus,
                        e -> "ERRO".equals(e.getStatus()) ? "s-vencido"
                                : "PROCESSADO".equals(e.getStatus()) ? "s-pago" : "")
                .quandoVazia("Nada passou por esta ligação ainda.")
                .montar();
    }

    // -------------------------------------------------------------- ajudinhas

    private String texto(Integer numero) {
        return numero == null ? "" : String.valueOf(numero);
    }

    private Integer numero(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return null;
        }
    }
}
