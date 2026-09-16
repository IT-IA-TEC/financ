package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.CasoDeCobranca;
import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Interacao;
import br.com.itia.financeiro.dominio.Mensagem;
import br.com.itia.financeiro.dominio.PermissaoDaIa;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.Conversas;
import br.com.itia.financeiro.servico.FerramentasDaMesa;
import br.com.itia.financeiro.servico.FichaDoCaso;
import br.com.itia.financeiro.servico.Inteligencia;
import br.com.itia.financeiro.servico.MesaDeCobranca;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A mesa de cobrança: a fila de quem precisa de atenção, a conversa com o
 * cliente, as ferramentas do atendimento, a ficha do cliente e a história do
 * caso, tudo na mesma tela.
 *
 * É a mesma mesa que o sistema sempre teve na página: nada do que existia lá
 * ficou de fora, nem as dezesseis ferramentas nem os comandos de barra.
 */
@Component
public class TelaConversas implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final MesaDeCobranca mesa;
    private final Conversas conversas;
    private final FichaDoCaso ficha;
    private final FerramentasDaMesa ferramentas;
    private final Inteligencia inteligencia;
    private final Janela janela;

    private UUID aberta;
    private String recorte = "dia";
    private String busca = "";
    private String abaDoCliente = "resumo";

    /** O que a pessoa está escrevendo, para não se perder quando a tela remonta. */
    private String rascunho = "";
    /** A última leitura do modelo, mostrada embaixo da conversa. */
    private String leituraDaIa;

    private TextArea escrever;

    public TelaConversas(MesaDeCobranca mesa, Conversas conversas, FichaDoCaso ficha,
                         FerramentasDaMesa ferramentas, Inteligencia inteligencia,
                         @Lazy Janela janela) {
        this.mesa = mesa;
        this.conversas = conversas;
        this.ficha = ficha;
        this.ferramentas = ferramentas;
        this.inteligencia = inteligencia;
        this.janela = janela;
    }

    @Override
    public boolean cabeNaTela() {
        return true;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        List<MesaDeCobranca.NaFila> fila = mesa.fila(hoje);
        List<MesaDeCobranca.Recorte> recortes = mesa.recortes(fila);
        List<MesaDeCobranca.NaFila> peneirada = mesa.peneirar(fila, recorte, busca);

        if (aberta == null && !peneirada.isEmpty()) {
            aberta = peneirada.get(0).unidadeId();
        }

        long esperando = fila.stream().filter(f -> "espera".equals(f.urgencia())).count();
        boolean canalLigado = conversas.canalDeWhatsApp().isPresent();
        List<Mensagem> semDono = conversas.semDono();

        VBox tela = new VBox(12);
        tela.getChildren().add(Pecas.cabecalho("com quem falar agora", "Mesa de cobrança",
                "A fila de quem precisa de atenção agora, com o motivo à mostra.",
                Pecas.botao("+ Chamar cliente", this::abrirChamarCliente),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Esperando", String.valueOf(esperando),
                        "responderam e ninguém voltou", true, false),
                Pecas.quadro("Na fila", String.valueOf(fila.size()), "clientes neste recorte"),
                Pecas.quadro("Sem dono", String.valueOf(semDono.size()),
                        "números não reconhecidos"),
                Pecas.quadro("Canal", canalLigado ? "ligado" : "desligado",
                        canalLigado ? "a mensagem sai daqui" : "a mensagem fica na fila"))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Conversas"));
        tela.getChildren().add(barraDeFiltros(recortes));

        HBox mesaDeTrabalho = new HBox(14,
                colunaDaFila(peneirada, semDono),
                colunaDoCaso(hoje, canalLigado),
                colunaDoCliente(hoje),
                colunaDeApoio());
        HBox.setHgrow(mesaDeTrabalho.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(mesaDeTrabalho, Priority.ALWAYS);
        tela.getChildren().add(mesaDeTrabalho);
        return tela;
    }

    // ------------------------------------------------------------- filtros

    private HBox barraDeFiltros(List<MesaDeCobranca.Recorte> recortes) {
        FlowPane filtros = new FlowPane(8, 8);
        for (MesaDeCobranca.Recorte qual : recortes) {
            Label marca = new Label(qual.nome() + " (" + qual.quantos() + ")");
            marca.getStyleClass().add("marca-situacao");
            if (qual.chave().equals(recorte)) {
                marca.getStyleClass().add("s-pago");
            }
            marca.setStyle("-fx-cursor: hand;");
            marca.setOnMouseClicked(clique -> {
                recorte = qual.chave();
                aberta = null;
                janela.atualizar();
            });
            filtros.getChildren().add(marca);
        }

        TextField procurar = new TextField(busca);
        procurar.setPromptText("buscar por nome ou valor");
        procurar.setPrefWidth(260);
        procurar.setOnAction(acao -> {
            busca = procurar.getText();
            janela.atualizar();
        });
        return new HBox(16, filtros, Pecas.empurrar(), procurar,
                Pecas.botaoVazado("Buscar", () -> {
                    busca = procurar.getText();
                    janela.atualizar();
                }));
    }

    // ---------------------------------------------------------------- fila

    /** A fila, do lado esquerdo, com o bloco dos números sem dono embaixo. */
    private Node colunaDaFila(List<MesaDeCobranca.NaFila> fila, List<Mensagem> semDono) {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");

        Label nome = new Label("FILA  ·  " + fila.size());
        nome.getStyleClass().add("nome-coluna");
        nome.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(nome);

        for (MesaDeCobranca.NaFila quem : fila) {
            coluna.getChildren().add(itemDaFila(quem));
        }
        if (fila.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("ninguém neste recorte"));
        }

        if (!semDono.isEmpty()) {
            Label cabeca = new Label("SEM CLIENTE LIGADO  ·  " + semDono.size());
            cabeca.getStyleClass().add("nome-coluna");
            cabeca.setMaxWidth(Double.MAX_VALUE);
            coluna.getChildren().add(cabeca);
            for (Mensagem solta : semDono) {
                coluna.getChildren().add(itemSemDono(solta));
            }
        }
        return larga(Pecas.rolandoPorDentro(coluna), 330);
    }

    private VBox itemDaFila(MesaDeCobranca.NaFila quem) {
        Label pessoa = new Label(quem.quem());
        pessoa.getStyleClass().add("titulo-item");
        pessoa.setWrapText(true);

        Label valor = new Label(quem.quitado() ? "quitado"
                : Pecas.dinheiro(quem.emAberto())
                        + (quem.maiorAtraso() > 0 ? " · " + quem.maiorAtraso() + " dias" : ""));
        valor.getStyleClass().add("codigo-item");

        HBox topo = new HBox(8, pessoa, Pecas.empurrar(), valor);
        topo.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(pessoa, Priority.ALWAYS);

        VBox item = new VBox(5, topo);
        if (quem.resumo() != null && !quem.resumo().isBlank()) {
            Label resumo = new Label(quem.resumo());
            resumo.getStyleClass().add("apoio-item");
            resumo.setWrapText(true);
            item.getChildren().add(resumo);
        }

        // a etiqueta já diz o motivo; os sinais abaixo só entram quando dizem outra coisa
        String etiqueta = quem.etiqueta() == null ? "" : quem.etiqueta();
        FlowPane sinais = new FlowPane(6, 6);
        if (!etiqueta.isBlank()) {
            sinais.getChildren().add(sinal(etiqueta,
                    "espera".equals(quem.urgencia()) ? "s-vencido" : ""));
        }
        if (quem.semResposta() && !etiqueta.equalsIgnoreCase("sem resposta")) {
            sinais.getChildren().add(sinal("sem resposta", "s-vencido"));
        }
        if (quem.promessaHoje() && !etiqueta.equalsIgnoreCase("promessa hoje")) {
            sinais.getChildren().add(sinal("promessa hoje", ""));
        }
        if (quem.acordoQuebrado() && !etiqueta.equalsIgnoreCase("acordo quebrado")) {
            sinais.getChildren().add(sinal("acordo quebrado", "s-vencido"));
        }
        if (quem.bloqueioPedido() && !etiqueta.equalsIgnoreCase("bloqueio pedido")) {
            sinais.getChildren().add(sinal("bloqueio pedido", "s-vencido"));
        }
        if (quem.comprovanteEsperando() && !etiqueta.equalsIgnoreCase("comprovante esperando")) {
            sinais.getChildren().add(sinal("comprovante esperando", ""));
        }
        if (quem.unidades() > 1) {
            sinais.getChildren().add(sinal(quem.unidades() + " unidades", ""));
        }
        if (quem.minha()) {
            sinais.getChildren().add(sinal("seu", "s-pago"));
        }
        if (!sinais.getChildren().isEmpty()) {
            item.getChildren().add(sinais);
        }

        item.getStyleClass().add("item-lista");
        if (quem.unidadeId().equals(aberta)) {
            item.getStyleClass().add("aberto");
        }
        item.setOnMouseClicked(clique -> {
            aberta = quem.unidadeId();
            leituraDaIa = null;
            janela.atualizar();
        });
        return item;
    }

    /** Um número que chegou e o sistema não sabe de quem é. */
    private VBox itemSemDono(Mensagem solta) {
        Label numero = new Label(solta.getDestino());
        numero.getStyleClass().add("titulo-item");

        Label corpo = new Label(encurtar(solta.getCorpo(), 70));
        corpo.getStyleClass().add("apoio-item");
        corpo.setWrapText(true);

        ComboBox<ClienteEspelho> dono = new ComboBox<>();
        dono.getItems().addAll(conversas.todosOsClientes());
        dono.setConverter(nomeDoCliente());
        dono.setMaxWidth(Double.MAX_VALUE);

        VBox item = new VBox(6, numero, corpo, dono,
                Pecas.botaoVazado("Ligar ao cliente", () -> {
                    if (dono.getValue() == null) {
                        janela.reclamar("Escolha de qual cliente é este número.");
                        return;
                    }
                    conversas.darDono(solta.getId(), dono.getValue().getId());
                    janela.avisar("Número ligado ao cliente.");
                    janela.atualizar();
                }));
        item.getStyleClass().add("item-lista");
        return item;
    }

    // ----------------------------------------------------------------- caso

    /** O meio: quem é o cliente, a conversa, as ferramentas e a caixa de escrever. */
    private Node colunaDoCaso(LocalDate hoje, boolean canalLigado) {
        if (aberta == null) {
            return Pecas.vazio("Escolha alguém da fila para abrir o caso.");
        }
        ClienteEspelho cliente = conversas.cliente(aberta);
        List<Titulo> emAberto = ferramentas.emAberto(aberta);
        BigDecimal deve = emAberto.stream().map(Titulo::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CasoDeCobranca caso = inteligencia.caso(aberta);

        VBox coluna = new VBox(12);
        coluna.getChildren().add(cabecaDoCliente(cliente, deve, emAberto.size(), caso, hoje));
        Node fio = fioDaConversa();
        coluna.getChildren().add(fio);
        VBox.setVgrow(fio, Priority.ALWAYS);
        coluna.getChildren().add(ferramentasDaConversa(hoje, caso));
        coluna.getChildren().add(caixaDeEscrever(hoje, canalLigado));
        if (leituraDaIa != null) {
            coluna.getChildren().add(bloco("Leitura do modelo", leituraDaIa,
                    "confira antes de mandar: o modelo erra, e quem assina é você"));
        }
        MesaDeCobranca.Sugestao copiloto = mesa.copiloto(aberta, hoje);
        coluna.getChildren().add(bloco("O que costuma resolver", copiloto.texto(),
                copiloto.base() >= 3
                        ? "olhando " + copiloto.base() + " caso(s) parecido(s) desta empresa"
                        : null));
        return Pecas.rolandoPorDentro(coluna);
    }

    private VBox cabecaDoCliente(ClienteEspelho cliente, BigDecimal deve, int documentos,
                                 CasoDeCobranca caso, LocalDate hoje) {
        Label nome = new Label(cliente.getRazaoSocial());
        nome.getStyleClass().add("titulo-item");
        nome.setStyle("-fx-font-size: 18px;");

        Label telefone = new Label(cliente.getTelefone() == null || cliente.getTelefone().isBlank()
                ? "sem telefone" : cliente.getTelefone());
        telefone.getStyleClass().add("dica");

        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().add(sinal("deve " + Pecas.dinheiro(deve), "s-vencido"));
        if (documentos > 0) {
            chips.getChildren().add(sinal(documentos + " documento(s)", ""));
        }
        if (caso != null && caso.pausada(hoje)) {
            chips.getChildren().add(sinal("régua pausada até "
                    + caso.getPausadaAte().format(DIA), ""));
        }
        if (cliente.getTomDeCobranca() != null) {
            chips.getChildren().add(sinal("tom "
                    + cliente.getTomDeCobranca().toLowerCase(), ""));
        }
        Map<String, BigDecimal> unidades = ferramentas.unidadesDoMesmoDono(aberta);
        if (unidades.size() > 1) {
            chips.getChildren().add(sinal(unidades.size() + " unidades do mesmo dono", ""));
        }

        VBox esquerda = new VBox(2, nome, telefone);
        HBox linha = new HBox(16, esquerda, Pecas.empurrar(), chips);
        linha.setAlignment(Pos.CENTER_LEFT);

        VBox caixa = new VBox(10, linha);
        caixa.getStyleClass().add("caixa");
        caixa.setPadding(new Insets(14));
        return caixa;
    }

    private Node fioDaConversa() {
        List<Mensagem> conversa = conversas.conversa(aberta);

        VBox fio = new VBox(10);
        fio.setPadding(new Insets(14));
        for (Mensagem mensagem : conversa) {
            Label texto = new Label(mensagem.getCorpo());
            texto.setWrapText(true);
            texto.setMaxWidth(560);
            texto.getStyleClass().add("balao");
            texto.getStyleClass().add(mensagem.deEntrada() ? "do-cliente" : "da-empresa");

            StringBuilder pe = new StringBuilder();
            if (mensagem.getEnviadaEm() != null) {
                pe.append(mensagem.getEnviadaEm().format(QUANDO));
            } else if (mensagem.getCriadoEm() != null) {
                pe.append(mensagem.getCriadoEm().format(QUANDO));
            }
            pe.append(pe.length() == 0 ? "" : " · ").append(mensagem.getSituacaoLegivel());
            if (mensagem.getCriadoPor() != null && !mensagem.getCriadoPor().isBlank()) {
                pe.append(" · ").append(mensagem.getCriadoPor());
            }
            Label quando = new Label(pe.toString());
            quando.getStyleClass().add("quando-balao");

            VBox bloco = new VBox(2, texto, quando);
            bloco.setAlignment(mensagem.deEntrada() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            fio.getChildren().add(bloco);
        }
        if (conversa.isEmpty()) {
            Label nada = new Label("nenhuma mensagem com este cliente ainda");
            nada.getStyleClass().add("dica");
            fio.getChildren().add(nada);
        }

        ScrollPane rolagem = new ScrollPane(fio);
        rolagem.getStyleClass().add("rolagem");
        rolagem.setFitToWidth(true);
        rolagem.setMinHeight(240);
        rolagem.setPrefHeight(360);
        rolagem.setVvalue(1);
        return rolagem;
    }

    // --------------------------------------------------- as dezesseis teclas

    /** A grade de ferramentas, com a mesma tecla de atalho que a página tinha. */
    private VBox ferramentasDaConversa(LocalDate hoje, CasoDeCobranca caso) {
        Map<String, String> comandos = ferramentas.comandos(aberta, hoje);
        boolean pausada = caso != null && caso.pausada(hoje);

        FlowPane grade = new FlowPane(8, 8);
        grade.getChildren().addAll(
                ferramenta("F2", "Segunda via", () -> escreverTexto(comandos.get("2via"))),
                ferramenta("F3", "Mandar chave PIX", () -> escreverTexto(comandos.get("pix"))),
                ferramenta("F4", "Cobrar tudo de uma vez",
                        () -> escreverTexto(comandos.get("tudo"))),
                ferramenta("F5", "Registrar promessa", () -> abrirPromessa(hoje)),
                ferramenta("F6", "Simular parcelamento", () -> abrirSimular(hoje)),
                ferramenta("F7", "Criar acordo", () -> janela.ir(TelaAcordos.class)),
                ferramenta("F8", "Conferir comprovante", () -> janela.ir(TelaComprovantes.class)),
                ferramenta("F9", "Registrar ligação", this::abrirLigacao),
                ferramenta("F10", "Agendar retorno", this::abrirRetorno),
                ferramenta("F11", "Passar o caso", this::abrirTransferir));
        if (pausada) {
            grade.getChildren().add(ferramenta("F12", "Religar a régua", () -> {
                ferramentas.voltarACobrar(aberta);
                janela.avisar("A régua voltou a valer para este cliente.");
                janela.atualizar();
            }));
        } else {
            grade.getChildren().add(ferramenta("F12", "Pausar a régua", this::abrirPausa));
        }
        grade.getChildren().add(ferramenta("N", "Nota interna", this::abrirNota));
        grade.getChildren().add(ferramentaPerigo("B", "Pedir bloqueio", this::abrirBloqueio));
        if (inteligencia.prontoParaTrabalhar()) {
            grade.getChildren().add(ferramenta("IA", "Ler o caso", this::pedirLeitura));
        }
        grade.getChildren().add(ferramenta("F", "Finalizar atendimento", this::abrirFinalizar));

        VBox caixa = new VBox(10, Pecas.secao("Ferramentas da conversa"), grade);
        caixa.getStyleClass().add("caixa");
        caixa.setPadding(new Insets(14));
        return caixa;
    }

    private Node ferramenta(String tecla, String texto, Runnable acao) {
        return botaoDeFerramenta(tecla, texto, acao, false);
    }

    private Node ferramentaPerigo(String tecla, String texto, Runnable acao) {
        return botaoDeFerramenta(tecla, texto, acao, true);
    }

    private Node botaoDeFerramenta(String tecla, String texto, Runnable acao, boolean perigo) {
        Label marca = new Label(tecla);
        marca.getStyleClass().add("tecla");

        Label nome = new Label(texto);
        nome.getStyleClass().add("nome-ferramenta");

        HBox botao = new HBox(8, marca, nome);
        botao.setAlignment(Pos.CENTER_LEFT);
        botao.getStyleClass().add("ferramenta-botao");
        if (perigo) {
            botao.getStyleClass().add("perigo");
        }
        botao.setOnMouseClicked(clique -> acao.run());
        return botao;
    }

    // ------------------------------------------------------------- escrever

    private VBox caixaDeEscrever(LocalDate hoje, boolean canalLigado) {
        escrever = new TextArea(rascunho);
        escrever.setPromptText("Escreva para o cliente, ou escolha um comando na lista.");
        escrever.setPrefRowCount(3);
        escrever.textProperty().addListener((onde, antes, agora) -> rascunho = agora);

        Map<String, String> comandos = ferramentas.comandos(aberta, hoje);
        ComboBox<String> lista = new ComboBox<>();
        lista.getItems().addAll(comandos.keySet());
        lista.setPromptText("comandos");
        lista.setOnAction(acao -> {
            if (lista.getValue() != null) {
                escreverTexto(comandos.get(lista.getValue()));
            }
        });

        Label dica = new Label(canalLigado ? "canal ligado: a mensagem sai daqui"
                : "canal desligado: a mensagem fica na fila");
        dica.getStyleClass().add("dica");

        HBox pe = new HBox(12,
                Pecas.botao("Mandar", () -> responder(escrever.getText())),
                Pecas.botaoVazado("Assumir o caso", () -> {
                    mesa.assumir(aberta);
                    janela.avisar("O caso é seu agora.");
                    janela.atualizar();
                }),
                lista, Pecas.empurrar(), dica);
        pe.setAlignment(Pos.CENTER_LEFT);

        TextField porFora = new TextField();
        porFora.setPromptText("ex.: ligou dizendo que paga sexta");
        HBox anotar = new HBox(12, Pecas.campo("O cliente respondeu por fora", porFora),
                Pecas.botaoVazado("Registrar", () -> {
                    if (porFora.getText() == null || porFora.getText().isBlank()) {
                        janela.reclamar("Escreva o que o cliente falou.");
                        return;
                    }
                    conversas.anotarResposta(aberta, porFora.getText().trim());
                    janela.avisar("Resposta registrada na conversa.");
                    janela.atualizar();
                }));
        anotar.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(anotar.getChildren().get(0), Priority.ALWAYS);

        VBox caixa = new VBox(10, escrever, pe, anotar);
        caixa.getStyleClass().add("caixa");
        caixa.setPadding(new Insets(14));
        return caixa;
    }

    private void escreverTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            janela.reclamar("Não há texto pronto para este comando.");
            return;
        }
        rascunho = texto;
        if (escrever != null) {
            escrever.setText(texto);
            escrever.requestFocus();
            escrever.positionCaret(texto.length());
        }
    }

    private void responder(String texto) {
        if (texto == null || texto.isBlank()) {
            janela.reclamar("Escreva a mensagem antes de mandar.");
            return;
        }
        Mensagem mandada = conversas.responder(aberta, texto.trim(), null);
        rascunho = "";
        janela.avisar("FALHOU".equals(mandada.getSituacao())
                ? "Não consegui enviar agora: " + mandada.getMotivo()
                : "Mensagem enviada.");
        janela.atualizar();
    }

    // -------------------------------------------------------- lado do cliente

    /** A direita: a ficha do cliente, com as quatro abas que a página tinha. */
    private Node colunaDoCliente(LocalDate hoje) {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");

        if (aberta == null) {
            Label nome = new Label("FICHA DO CASO");
            nome.getStyleClass().add("nome-coluna");
            nome.setMaxWidth(Double.MAX_VALUE);
            coluna.getChildren().add(nome);
            coluna.getChildren().add(recadoEscuro("escolha alguém da fila"));
            return larga(Pecas.rolandoPorDentro(coluna), 320);
        }

        HBox abas = new HBox(0);
        abas.getStyleClass().add("abas-lado");
        abas.getChildren().addAll(
                abaDoLado("Resumo", "resumo"),
                abaDoLado("Cobranças", "cobrancas"),
                abaDoLado("Pagamentos", "pagamentos"),
                abaDoLado("Acordos", "acordos"));
        coluna.getChildren().add(abas);

        switch (abaDoCliente) {
            case "cobrancas" -> preencherCobrancas(coluna, hoje);
            case "pagamentos" -> preencherPagamentos(coluna);
            case "acordos" -> preencherAcordos(coluna);
            default -> preencherResumo(coluna, hoje);
        }
        return larga(Pecas.rolandoPorDentro(coluna), 320);
    }

    private Label abaDoLado(String texto, String chave) {
        Label aba = new Label(texto.toUpperCase());
        aba.getStyleClass().add("aba-lado");
        if (abaDoCliente.equals(chave)) {
            aba.getStyleClass().add("ativa");
        }
        aba.setOnMouseClicked(clique -> {
            abaDoCliente = chave;
            janela.atualizar();
        });
        return aba;
    }

    private void preencherResumo(VBox coluna, LocalDate hoje) {
        List<FichaDoCaso.Linha> linhas = ficha.de(aberta, hoje);
        for (FichaDoCaso.Linha linha : linhas) {
            Label rotulo = new Label(linha.rotulo().toUpperCase());
            rotulo.getStyleClass().add("apoio-item");

            Label valor = new Label(linha.valor());
            valor.getStyleClass().add("titulo-item");
            valor.setWrapText(true);
            if (linha.atencao()) {
                valor.setStyle("-fx-text-fill: #FF9200;");
            }

            VBox bloco = new VBox(3, rotulo, valor);
            if (linha.detalhe() != null && !linha.detalhe().isBlank()) {
                Label detalhe = new Label(linha.detalhe());
                detalhe.getStyleClass().add("apoio-item");
                detalhe.setWrapText(true);
                bloco.getChildren().add(detalhe);
            }
            bloco.getStyleClass().add("item-lista");
            coluna.getChildren().add(bloco);
        }
        if (linhas.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("nenhuma linha configurada"));
        }
        VBox mudar = new VBox(Pecas.botaoVazado("Mudar o que aparece aqui",
                () -> janela.ir(TelaInteligencia.class)));
        mudar.getStyleClass().add("item-lista");
        coluna.getChildren().add(mudar);
    }

    private void preencherCobrancas(VBox coluna, LocalDate hoje) {
        List<Titulo> abertos = ferramentas.emAberto(aberta);
        for (Titulo titulo : abertos) {
            Label codigo = new Label(titulo.getIdentificadorPix());
            codigo.getStyleClass().add("titulo-item");

            Label descricao = new Label(titulo.getDescricao());
            descricao.getStyleClass().add("apoio-item");
            descricao.setWrapText(true);

            Label numeros = new Label(Pecas.data(titulo.getVencimento()) + "  ·  "
                    + Pecas.dinheiro(titulo.getSaldo()));
            numeros.getStyleClass().add("codigo-item");

            VBox bloco = new VBox(3, codigo, descricao, numeros);
            long atraso = titulo.diasDeAtraso(hoje);
            if (atraso > 0) {
                bloco.getChildren().add(sinal(atraso + " dias de atraso", "s-vencido"));
            }
            bloco.getStyleClass().add("item-lista");
            bloco.setOnMouseClicked(clique -> janela.ir(TelaTitulo.class, titulo.getId()));
            coluna.getChildren().add(bloco);
        }
        if (abertos.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("nada em aberto"));
        }
    }

    private void preencherPagamentos(VBox coluna) {
        List<FerramentasDaMesa.Entrada> entradas = ferramentas.pagamentos(aberta);
        for (FerramentasDaMesa.Entrada entrada : entradas) {
            Label documento = new Label(entrada.documento());
            documento.getStyleClass().add("titulo-item");

            Label numeros = new Label((entrada.quando() == null ? "sem data"
                    : Pecas.data(entrada.quando())) + "  ·  " + Pecas.dinheiro(entrada.valor()));
            numeros.getStyleClass().add("codigo-item");

            Label como = new Label(entrada.atraso() == 0 ? "pagou em dia"
                    : "pagou com " + entrada.atraso() + " dias de atraso");
            como.getStyleClass().add("apoio-item");

            VBox bloco = new VBox(3, documento, numeros, como);
            bloco.getStyleClass().add("item-lista");
            coluna.getChildren().add(bloco);
        }
        if (entradas.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("este cliente ainda não pagou nada aqui"));
        }
    }

    private void preencherAcordos(VBox coluna) {
        List<Acordo> acordos = ferramentas.acordosDe(aberta);
        for (Acordo acordo : acordos) {
            Label nome = new Label("Acordo " + acordo.getNumero());
            nome.getStyleClass().add("titulo-item");

            Label numeros = new Label(acordo.getSituacaoLegivel() + "  ·  "
                    + Pecas.dinheiro(acordo.getValorCombinado()));
            numeros.getStyleClass().add("codigo-item");

            Label parcelas = new Label(acordo.getParcelas() + " parcela(s)");
            parcelas.getStyleClass().add("apoio-item");

            VBox bloco = new VBox(3, nome, numeros, parcelas);
            bloco.getStyleClass().add("item-lista");
            bloco.setOnMouseClicked(clique -> janela.ir(TelaAcordo.class, acordo.getId()));
            coluna.getChildren().add(bloco);
        }
        if (acordos.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("nenhum acordo"));
        }
    }

    // ------------------------------------------------------------ apoio

    /** A ponta direita: os atalhos de um clique e a história do caso. */
    private Node colunaDeApoio() {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");

        Label cabeca = new Label("DE UM CLIQUE");
        cabeca.getStyleClass().add("nome-coluna");
        cabeca.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(cabeca);

        if (aberta == null) {
            coluna.getChildren().add(recadoEscuro("escolha alguém da fila"));
            return larga(Pecas.rolandoPorDentro(coluna), 280);
        }

        coluna.getChildren().addAll(
                atalho("Ficha completa do cliente", "cadastro, contatos e contratos",
                        () -> janela.ir(TelaCliente.class, aberta)),
                atalho("Contas a receber", "todos os documentos da empresa",
                        () -> janela.ir(TelaTitulos.class)),
                atalho("Esteira", "onde ele está na cobrança",
                        () -> janela.ir(TelaEsteira.class)));

        Label historiaCabeca = new Label("HISTÓRIA");
        historiaCabeca.getStyleClass().add("nome-coluna");
        historiaCabeca.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(historiaCabeca);

        List<Interacao> historia = mesa.historia(aberta);
        for (Interacao momento : historia) {
            Label quando = new Label(momento.getOcorridoEm() == null ? ""
                    : momento.getOcorridoEm().format(QUANDO));
            quando.getStyleClass().add("codigo-item");

            Label tipo = new Label(momento.getTipoLegivel());
            tipo.getStyleClass().add("titulo-item");

            Label descricao = new Label(encurtar(momento.getDescricao(), 90));
            descricao.getStyleClass().add("apoio-item");
            descricao.setWrapText(true);

            VBox bloco = new VBox(3, quando, tipo, descricao);
            bloco.getStyleClass().add("item-lista");
            coluna.getChildren().add(bloco);
        }
        if (historia.isEmpty()) {
            coluna.getChildren().add(recadoEscuro("nada registrado ainda"));
        }
        return larga(Pecas.rolandoPorDentro(coluna), 280);
    }

    private VBox atalho(String titulo, String apoio, Runnable acao) {
        Label nome = new Label(titulo);
        nome.getStyleClass().add("titulo-item");
        nome.setWrapText(true);

        Label explicacao = new Label(apoio);
        explicacao.getStyleClass().add("apoio-item");
        explicacao.setWrapText(true);

        VBox bloco = new VBox(3, nome, explicacao);
        bloco.getStyleClass().add("item-lista");
        bloco.setOnMouseClicked(clique -> acao.run());
        return bloco;
    }

    // ---------------------------------------------------------- as janelas

    private void abrirChamarCliente() {
        ComboBox<ClienteEspelho> cliente = new ComboBox<>();
        cliente.getItems().add(null);
        cliente.getItems().addAll(conversas.todosOsClientes());
        cliente.setConverter(nomeDoCliente());
        cliente.getSelectionModel().selectFirst();
        cliente.setMaxWidth(Double.MAX_VALUE);

        TextField numero = new TextField();
        numero.setPromptText("(11) 90000-0000");

        TextArea texto = new TextArea();
        texto.setPromptText("Oi, tudo bem? Aqui é da empresa, sobre o pagamento.");
        texto.setPrefRowCount(4);

        Label dica = new Label("Escolha um cliente da lista, ou escreva um número solto. Número "
                + "que não é de cliente nenhum entra como conversa sem dono, e depois você liga "
                + "ao cadastro certo.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Chamar cliente",
                        "Começar uma conversa do zero.")
                .com(dica, Pecas.campo("Cliente", cliente),
                        Pecas.campo("Número do WhatsApp", numero),
                        Pecas.campo("Mensagem", texto))
                .acao("Chamar", () -> {
                    if (texto.getText() == null || texto.getText().isBlank()) {
                        janela.reclamar("Escreva a mensagem.");
                        return false;
                    }
                    Mensagem mandada = conversas.comecar(
                            cliente.getValue() == null ? null : cliente.getValue().getId(),
                            numero.getText(), texto.getText().trim());
                    janela.avisar(switch (mandada.getSituacao()) {
                        case "NA_FILA" -> "Conversa começada. A mensagem está na fila.";
                        case "FALHOU" -> "Conversa começada, mas o canal não aceitou: "
                                + mandada.getMotivo();
                        default -> "Conversa começada e mensagem enviada.";
                    });
                    if (cliente.getValue() != null) {
                        aberta = cliente.getValue().getId();
                    }
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirPromessa(LocalDate hoje) {
        DatePicker quando = new DatePicker(hoje.plusDays(1));
        TextField valor = new TextField(Pecas.dinheiro(ferramentas.emAberto(aberta).stream()
                .map(Titulo::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add))
                .replace("R$ ", ""));

        Label dica = new Label("Enquanto a promessa estiver de pé, a cobrança automática segura "
                + "este cliente. Se a data passar sem pagamento, ele volta para o topo da fila.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Registrar promessa",
                        "O que ficou combinado com o cliente.")
                .com(dica, new HBox(16, Pecas.campo("Prometeu pagar em", quando),
                        Pecas.campo("Valor", valor)))
                .acao("Registrar", () -> {
                    if (quando.getValue() == null) {
                        janela.reclamar("Escolha a data da promessa.");
                        return false;
                    }
                    mesa.anotarPromessa(aberta, quando.getValue(),
                            TelaTituloNovo.dinheiro(valor.getText()));
                    janela.avisar("Promessa registrada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirSimular(LocalDate hoje) {
        TextField vezes = new TextField("3");
        DatePicker primeiro = new DatePicker(hoje.plusDays(7));

        Label dica = new Label("A simulação só escreve o texto na caixa, com os números certos. "
                + "O acordo de verdade continua sendo criado na tela de acordos, por quem tem "
                + "alçada.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Simular parcelamento", "Sem criar nada.")
                .com(dica, new HBox(16, Pecas.campo("Em quantas vezes", vezes),
                        Pecas.campo("Primeira parcela em", primeiro)))
                .acao("Simular", () -> {
                    escreverTexto(ferramentas.simular(aberta, inteiro(vezes.getText()),
                            primeiro.getValue(), hoje));
                    return true;
                })
                .abrir();
    }

    private void abrirLigacao() {
        TextArea texto = new TextArea();
        texto.setPromptText("quem atendeu, o que ficou combinado");
        texto.setPrefRowCount(3);

        JanelaFlutuante.estreita(janela.palco(), "Registrar ligação", "Fora do WhatsApp.")
                .com(Pecas.campo("O que foi falado", texto))
                .acao("Registrar", () -> {
                    ferramentas.registrarLigacao(aberta, texto.getText());
                    janela.avisar("Ligação registrada na história.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirRetorno() {
        DatePicker quando = new DatePicker(LocalDate.now().plusDays(2));
        TextField fazer = new TextField();
        fazer.setPromptText("conferir se o PIX caiu");

        JanelaFlutuante.estreita(janela.palco(), "Agendar retorno", "Para não esquecer.")
                .com(new HBox(16, Pecas.campo("Voltar a falar em", quando),
                        Pecas.campo("O que fazer", fazer)))
                .acao("Agendar", () -> {
                    if (quando.getValue() == null) {
                        janela.reclamar("Escolha a data do retorno.");
                        return false;
                    }
                    ferramentas.agendarRetorno(aberta, quando.getValue(), fazer.getText());
                    janela.avisar("Retorno agendado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirPausa() {
        DatePicker ate = new DatePicker(LocalDate.now().plusDays(7));
        TextField motivo = new TextField();
        motivo.setPromptText("acordo em dia, combinado com o cliente");

        Label dica = new Label("Nenhuma cobrança automática sai para este cliente até a data "
                + "escolhida. A data de fim é obrigatória: régua parada para sempre é dívida "
                + "esquecida.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Pausar a régua", "Sem cobrar por cima.")
                .com(dica, new HBox(16, Pecas.campo("Parada até", ate),
                        Pecas.campo("Por quê", motivo)))
                .acao("Pausar", () -> {
                    if (ate.getValue() == null) {
                        janela.reclamar("Escolha até quando a régua fica parada.");
                        return false;
                    }
                    ferramentas.pausarRegua(aberta, ate.getValue(), motivo.getText());
                    janela.avisar("Régua pausada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirNota() {
        TextArea texto = new TextArea();
        texto.setPromptText("ex.: o dono pediu para falar só com o contador");
        texto.setPrefRowCount(3);

        Label dica = new Label("O cliente não vê esta nota. Ela fica na história, para quem "
                + "pegar o caso depois entender o que já aconteceu.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Nota interna", "Só para a equipe.")
                .com(dica, texto)
                .acao("Guardar", () -> {
                    if (texto.getText() == null || texto.getText().isBlank()) {
                        janela.reclamar("Escreva a nota.");
                        return false;
                    }
                    ferramentas.notaInterna(aberta, texto.getText().trim());
                    janela.avisar("Nota guardada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirTransferir() {
        TextField quem = new TextField();
        quem.setPromptText("nome de quem assume");

        JanelaFlutuante.estreita(janela.palco(), "Passar o caso", "Passar adiante.")
                .com(Pecas.campo("Para quem", quem))
                .acao("Passar", () -> {
                    if (quem.getText() == null || quem.getText().isBlank()) {
                        janela.reclamar("Escreva para quem o caso vai.");
                        return false;
                    }
                    ferramentas.transferir(aberta, quem.getText().trim());
                    janela.avisar("Caso passado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirBloqueio() {
        TextField motivo = new TextField();
        motivo.setPromptText("ex.: 90 dias de atraso e três promessas quebradas");

        Label dica = new Label("O pedido fica registrado com o motivo e com quem pediu. Bloquear "
                + "de verdade continua sendo decisão de quem cuida do serviço.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        JanelaFlutuante.estreita(janela.palco(), "Pedir bloqueio", "Último recurso.")
                .com(dica, Pecas.campo("Motivo", motivo))
                .acao("Pedir bloqueio", () -> {
                    if (motivo.getText() == null || motivo.getText().isBlank()) {
                        janela.reclamar("Escreva o motivo do pedido.");
                        return false;
                    }
                    ferramentas.pedirBloqueio(aberta, motivo.getText().trim());
                    janela.avisar("Bloqueio pedido.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void abrirFinalizar() {
        ComboBox<String> situacao = new ComboBox<>();
        situacao.getItems().addAll(CasoDeCobranca.SITUACOES);
        situacao.getSelectionModel().selectFirst();
        situacao.setMaxWidth(Double.MAX_VALUE);

        DatePicker data = new DatePicker();
        TextField acao = new TextField();
        acao.setPromptText("conferir se o PIX caiu");

        TextArea combinado = new TextArea();
        combinado.setPromptText("fica na história do cliente, para quem pegar o caso depois");
        combinado.setPrefRowCount(3);

        HBox linha = new HBox(16, Pecas.campo("Como o caso fica", situacao),
                Pecas.campo("Voltar a falar em", data),
                Pecas.campo("O que fazer na volta", acao));
        linha.getChildren().forEach(campo -> HBox.setHgrow(campo, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Finalizar atendimento", "Fim do atendimento.")
                .com(linha, Pecas.campo("O que ficou combinado", combinado))
                .acao("Finalizar", () -> {
                    mesa.finalizar(aberta, situacao.getValue(), combinado.getText(),
                            data.getValue(), acao.getText());
                    janela.avisar("Atendimento finalizado.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void pedirLeitura() {
        Inteligencia.Resposta resposta = inteligencia.pedirLeitura(aberta, null);
        if (!resposta.deuCerto()) {
            janela.reclamar(resposta.recusa());
            return;
        }
        leituraDaIa = resposta.texto();
        if (inteligencia.podeFazer(PermissaoDaIa.SUGERIR_RESPOSTA, aberta, null) == null) {
            rascunho = resposta.texto();
        }
        janela.avisar("O modelo leu o caso. Confira antes de usar.");
        janela.atualizar();
    }

    // ------------------------------------------------------------ ajudinhas

    /** Fixa a largura da coluna: quem rola é o miolo, não a mesa inteira. */
    private Node larga(javafx.scene.control.ScrollPane coluna, double largura) {
        coluna.setPrefWidth(largura);
        coluna.setMinWidth(largura);
        coluna.setMaxWidth(largura);
        return coluna;
    }

    private Label sinal(String texto, String cor) {
        Label marca = new Label(texto);
        marca.getStyleClass().add("marca-situacao");
        if (cor != null && !cor.isBlank()) {
            marca.getStyleClass().add(cor);
        }
        return marca;
    }

    private VBox bloco(String titulo, String texto, String rodape) {
        Label corpo = new Label(texto == null ? "" : texto);
        corpo.getStyleClass().add("texto");
        corpo.setWrapText(true);

        VBox caixa = new VBox(8, Pecas.secao(titulo), corpo);
        if (rodape != null && !rodape.isBlank()) {
            Label pe = new Label(rodape);
            pe.getStyleClass().add("dica");
            pe.setWrapText(true);
            caixa.getChildren().add(pe);
        }
        caixa.getStyleClass().add("caixa");
        caixa.setPadding(new Insets(14));
        return caixa;
    }

    private Label recadoEscuro(String texto) {
        Label recado = new Label(texto);
        recado.getStyleClass().add("apoio-item");
        recado.setStyle("-fx-padding: 20 14 20 14;");
        recado.setWrapText(true);
        return recado;
    }

    private StringConverter<ClienteEspelho> nomeDoCliente() {
        return new StringConverter<>() {
            @Override
            public String toString(ClienteEspelho qual) {
                if (qual == null) {
                    return "nenhum, vou digitar o número";
                }
                return qual.getTelefone() == null || qual.getTelefone().isBlank()
                        ? qual.getRazaoSocial()
                        : qual.getRazaoSocial() + " · " + qual.getTelefone();
            }

            @Override
            public ClienteEspelho fromString(String texto) {
                return null;
            }
        };
    }

    private String encurtar(String texto, int quanto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= quanto ? texto : texto.substring(0, quanto - 1) + "…";
    }

    private int inteiro(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return 3;
        }
    }
}
