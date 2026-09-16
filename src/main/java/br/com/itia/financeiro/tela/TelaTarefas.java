package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.AnotacaoDaTarefa;
import br.com.itia.financeiro.dominio.FonteDeTarefas;
import br.com.itia.financeiro.dominio.Tarefa;
import br.com.itia.financeiro.servico.Cupula;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Tarefas: o que precisa de você e o que você pediu para os outros.
 *
 * A lista fica de um lado e a tarefa aberta do outro, para não se perder o fio
 * ao andar de uma para a outra. Nada é apagado: cancelar guarda o motivo.
 */
@Component
public class TelaTarefas implements Tela {

    private static final DateTimeFormatter QUANDO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Cupula cupula;
    private final br.com.itia.financeiro.servico.TarefasDeFora deFora;
    private final Janela janela;

    private String aba = "central";
    private String tipoEscolhido = "TAREFA";
    private UUID aberta;

    public TelaTarefas(Cupula cupula,
                       br.com.itia.financeiro.servico.TarefasDeFora deFora,
                       @Lazy Janela janela) {
        this.cupula = cupula;
        this.deFora = deFora;
        this.janela = janela;
    }

    @Override
    public boolean cabeNaTela() {
        return true;
    }

    @Override
    public String secao() {
        return "cupula";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        Cupula.Resumo resumo = cupula.resumo(hoje);
        List<Tarefa> lista = switch (aba) {
            case "recebidas" -> cupula.recebidas();
            case "enviadas" -> cupula.enviadas();
            case "faco_parte" -> cupula.facoParte();
            case "do_setor" -> cupula.doSetor();
            default -> cupula.central();
        };

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("o que precisa de você", "Tarefas",
                "O que chegou para você, o que você pediu e o que o setor ainda não pegou.",
                Pecas.botaoVazado("De onde vêm as tarefas",
                        () -> janela.ir(TelaFontesDeTarefas.class)),
                botaoDoNovo(),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Na fila", String.valueOf(resumo.central()),
                        "ainda não vistas", true, false),
                Pecas.quadro("Comigo", String.valueOf(resumo.minhas()),
                        "abertas para eu fazer"),
                Pecas.quadro("Atrasadas", String.valueOf(resumo.atrasadas()),
                        "passaram do prazo", false, resumo.atrasadas() > 0),
                Pecas.quadro("Esperando você", String.valueOf(resumo.esperandoRevisao()),
                        "terminadas, faltando conferir"))));

        LinkedHashMap<String, Runnable> abas = new LinkedHashMap<>();
        abas.put("Central (" + resumo.central() + ")", () -> trocarPara("central"));
        abas.put("Recebidas (" + resumo.minhas() + ")", () -> trocarPara("recebidas"));
        abas.put("Enviadas", () -> trocarPara("enviadas"));
        abas.put("Faço parte", () -> trocarPara("faco_parte"));
        abas.put("Do setor (" + resumo.doSetor() + ")", () -> trocarPara("do_setor"));
        tela.getChildren().add(Pecas.abas(rotuloDaAba(resumo), abas));


        HBox mesa = new HBox(16,
                Pecas.rolandoPorDentro(listaDeTarefas(lista, hoje)),
                Pecas.rolandoPorDentro(tarefaAberta(hoje)),
                Pecas.rolandoPorDentro(colunaDeApoio(hoje)));
        mesa.getChildren().get(0).setStyle("-fx-min-width: 320; -fx-pref-width: 320;");
        mesa.getChildren().get(2).setStyle("-fx-min-width: 300; -fx-pref-width: 300;");
        HBox.setHgrow(mesa.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(mesa, Priority.ALWAYS);
        tela.getChildren().add(mesa);
        return tela;
    }

    private String rotuloDaAba(Cupula.Resumo resumo) {
        return switch (aba) {
            case "recebidas" -> "Recebidas (" + resumo.minhas() + ")";
            case "enviadas" -> "Enviadas";
            case "faco_parte" -> "Faço parte";
            case "do_setor" -> "Do setor (" + resumo.doSetor() + ")";
            default -> "Central (" + resumo.central() + ")";
        };
    }

    private void trocarPara(String qual) {
        this.aba = qual;
        this.aberta = null;
        janela.atualizar();
    }

    /** A coluna da esquerda: a fila da aba escolhida. */
    private VBox listaDeTarefas(List<Tarefa> lista, LocalDate hoje) {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");
        coluna.setPrefWidth(320);
        coluna.setMinWidth(280);

        Label nome = new Label(nomeDaLista());
        nome.getStyleClass().add("nome-coluna");

        Label quantas = new Label(String.valueOf(lista.size()));
        quantas.getStyleClass().add("contador-coluna");

        HBox faixa = new HBox(8, nome, Pecas.empurrar(), quantas);
        faixa.getStyleClass().add("faixa-coluna");
        faixa.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        coluna.getChildren().add(faixa);

        if ("central".equals(aba)) {
            Label comoFunciona = new Label("Do mais antigo para o mais novo. Abrir tira da fila.");
            comoFunciona.getStyleClass().add("dica");
            comoFunciona.setWrapText(true);
            comoFunciona.setStyle("-fx-padding: 10 14 4 14;");
            coluna.getChildren().add(comoFunciona);
        }

        if (lista.isEmpty()) {
            Label vazio = new Label("Nada aqui.");
            vazio.getStyleClass().add("dica");
            vazio.setStyle("-fx-padding: 24;");
            coluna.getChildren().add(vazio);
            return coluna;
        }

        for (Tarefa tarefa : lista) {
            coluna.getChildren().add(itemDaLista(tarefa, hoje));
        }
        return coluna;
    }

    /**
     * A coluna da direita: quem ainda não tem dono e o que vem de outro sistema.
     *
     * É a mesma coluna de apoio da versão web: mostra o que está parado sem
     * ninguém para pegar, e de onde chegam as tarefas de fora.
     */
    private VBox colunaDeApoio(LocalDate hoje) {
        VBox coluna = new VBox(0);
        coluna.getStyleClass().add("coluna-lista");
        coluna.setPrefWidth(300);
        coluna.setMinWidth(260);

        List<Tarefa> semDono = cupula.semDono();

        Label titulo = new Label("ESPERANDO DONO");
        titulo.getStyleClass().add("nome-coluna");
        titulo.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(titulo);

        if (semDono.isEmpty()) {
            Label vazio = new Label("Ninguém esperando dono.");
            vazio.getStyleClass().add("dica");
            vazio.setStyle("-fx-padding: 18;");
            coluna.getChildren().add(vazio);
        } else {
            for (Tarefa tarefa : semDono) {
                coluna.getChildren().add(itemDaLista(tarefa, hoje));
            }
        }

        Label tituloDeFora = new Label("DE OUTRO SISTEMA");
        tituloDeFora.getStyleClass().add("nome-coluna");
        tituloDeFora.setMaxWidth(Double.MAX_VALUE);
        coluna.getChildren().add(tituloDeFora);

        List<FonteDeTarefas> fontes = deFora.todas();
        if (fontes.isEmpty()) {
            Label nenhuma = new Label("Nenhuma fonte ligada.");
            nenhuma.getStyleClass().add("dica");
            nenhuma.setStyle("-fx-padding: 18;");
            coluna.getChildren().add(nenhuma);
        } else {
            for (FonteDeTarefas fonte : fontes) {
                Label nome = new Label(fonte.getNome());
                nome.getStyleClass().add("titulo-item");

                Label situacao = new Label(fonte.isAtiva() ? "ligada" : "desligada");
                situacao.getStyleClass().add("apoio-item");

                VBox item = new VBox(4, nome, situacao,
                        Pecas.botaoVazado("Buscar agora", () -> {
                            br.com.itia.financeiro.servico.TarefasDeFora.Puxada resultado = deFora.puxar(fonte.getId());
                            janela.avisar(resultado.erro() != null
                                    ? "A busca falhou: " + resultado.erro()
                                    : resultado.novas() + " nova(s), "
                                            + resultado.atualizadas() + " atualizada(s).");
                            janela.atualizar();
                        }));
                item.getStyleClass().add("item-lista");
                coluna.getChildren().add(item);
            }
        }

        VBox configurar = new VBox(4,
                Pecas.botaoVazado("Configurar as fontes",
                        () -> janela.ir(TelaFontesDeTarefas.class)));
        configurar.setStyle("-fx-padding: 12;");
        coluna.getChildren().add(configurar);
        return coluna;
    }

    private String nomeDaLista() {
        return switch (aba) {
            case "recebidas" -> "PARA EU FAZER";
            case "enviadas" -> "EU PEDI";
            case "faco_parte" -> "EU ACOMPANHO";
            case "do_setor" -> "O SETOR INTEIRO";
            default -> "FILA DO QUE NÃO FOI VISTO";
        };
    }

    private VBox itemDaLista(Tarefa tarefa, LocalDate hoje) {
        Label titulo = new Label(tarefa.getTitulo());
        titulo.setWrapText(true);
        titulo.getStyleClass().add("titulo-item");

        Label codigo = new Label(tarefa.getCodigo());
        codigo.getStyleClass().add("codigo-item");
        // o código nunca encolhe: é por ele que a tarefa é chamada
        codigo.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        HBox topo = new HBox(8, titulo, Pecas.empurrar(), codigo);
        HBox.setHgrow(titulo, Priority.ALWAYS);

        Label quem = new Label(tarefa.doSetor()
                ? "sem dono, aberta para " + tarefa.getSetor()
                : "com " + tarefa.getResponsavel());
        quem.getStyleClass().add("apoio-item");

        FlowPane marcas = new FlowPane(6, 6);
        marcas.getChildren().add(etiqueta(tarefa.getSituacaoLegivel(), ""));
        if (!"TAREFA".equals(tarefa.getTipo())) {
            marcas.getChildren().add(etiqueta(tarefa.getRotuloDoTipo(), "s-pago"));
        }
        if (tarefa.getPrazo() != null) {
            marcas.getChildren().add(etiqueta(
                    tarefa.atrasada(hoje) ? "venceu " + Pecas.dataCurta(tarefa.getPrazo())
                            : "até " + Pecas.dataCurta(tarefa.getPrazo()),
                    tarefa.atrasada(hoje) ? "s-vencido" : ""));
        }

        VBox item = new VBox(6, topo, quem, marcas);
        item.getStyleClass().add("item-lista");
        if (tarefa.getId().equals(aberta)) {
            item.getStyleClass().add("aberto");
        }
        item.setOnMouseClicked(clique -> {
            aberta = tarefa.getId();
            cupula.abrir(tarefa.getId());
            janela.atualizar();
        });
        return item;
    }

    private Label etiqueta(String texto, String cor) {
        texto = texto == null ? "" : texto.toUpperCase(new java.util.Locale("pt", "BR"));
        Label marca = new Label(texto);
        marca.getStyleClass().add("marca-situacao");
        if (!cor.isBlank()) {
            marca.getStyleClass().add(cor);
        }
        return marca;
    }

    /** A coluna da direita: a tarefa que está aberta. */
    private Node tarefaAberta(LocalDate hoje) {
        if (aberta == null) {
            return Pecas.vazio("Escolha uma tarefa da lista para abrir.");
        }
        Tarefa tarefa = cupula.tarefa(aberta);
        List<AnotacaoDaTarefa> fio = cupula.fioDe(aberta);

        VBox coluna = new VBox(16);

        Label titulo = new Label(tarefa.getTitulo());
        titulo.getStyleClass().add("titulo-pagina");
        titulo.setStyle("-fx-font-size: 22px;");
        titulo.setWrapText(true);

        Label quem = new Label(tarefa.getCodigo() + " · pedida por " + tarefa.getPedidaPor());
        quem.getStyleClass().add("mono");
        quem.setStyle("-fx-font-size: 12px; -fx-text-fill: #2E2E31;");

        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().add(etiqueta(tarefa.getSituacaoLegivel(), "s-pago"));
        if (tarefa.getPrazo() != null) {
            chips.getChildren().add(etiqueta(
                    tarefa.atrasada(hoje) ? "venceu em " + Pecas.data(tarefa.getPrazo())
                            : "até " + Pecas.data(tarefa.getPrazo()),
                    tarefa.atrasada(hoje) ? "s-vencido" : ""));
        }
        chips.getChildren().add(etiqueta("prioridade " + tarefa.getPrioridadeLegivel(), ""));

        coluna.getChildren().add(new VBox(6, titulo, quem, chips));
        coluna.getChildren().add(acoes(tarefa));

        if (tarefa.getDescricao() != null && !tarefa.getDescricao().isBlank()) {
            Label detalhe = new Label(tarefa.getDescricao());
            detalhe.setWrapText(true);
            coluna.getChildren().add(Pecas.caixa(detalhe));
        }

        coluna.getChildren().add(Pecas.secao("O que aconteceu"));
        VBox linhaDoTempo = new VBox(0);
        linhaDoTempo.getStyleClass().add("caixa");
        for (AnotacaoDaTarefa momento : fio) {
            Label quando = new Label(momento.getCriadoEm().format(QUANDO) + " · "
                    + momento.getAutor());
            quando.getStyleClass().add("mono");
            quando.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E2E31;");

            Label texto = new Label(momento.getTexto());
            texto.setWrapText(true);

            VBox bloco = new VBox(3, quando, texto);
            bloco.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: transparent transparent "
                    + "#DCDCE0 transparent; -fx-border-width: 0 0 1 0;");
            linhaDoTempo.getChildren().add(bloco);
        }
        if (fio.isEmpty()) {
            Label nada = new Label("nada registrado ainda");
            nada.getStyleClass().add("dica");
            linhaDoTempo.getChildren().add(nada);
        }

        ScrollPane rolagem = new ScrollPane(linhaDoTempo);
        rolagem.getStyleClass().add("rolagem");
        rolagem.setFitToWidth(true);
        rolagem.setPrefHeight(320);
        coluna.getChildren().add(rolagem);

        TextArea escrever = new TextArea();
        escrever.setPromptText("escreva no fio desta tarefa");
        escrever.setPrefRowCount(2);
        coluna.getChildren().add(new VBox(8, escrever,
                new HBox(12, Pecas.botao("Mandar", () -> comentar(tarefa.getId(),
                        escrever.getText())))));
        return coluna;
    }

    private FlowPane acoes(Tarefa tarefa) {
        FlowPane linha = new FlowPane(12, 12);
        UUID id = tarefa.getId();

        if (tarefa.doSetor()) {
            linha.getChildren().add(Pecas.botao("Pegar para mim", () -> {
                cupula.pegarParaMim(id);
                janela.avisar("Tarefa é sua agora.");
                janela.atualizar();
            }));
        }
        if ("A_FAZER".equals(tarefa.getSituacao()) && !tarefa.doSetor()) {
            linha.getChildren().add(Pecas.botao("Começar", () -> {
                cupula.comecar(id);
                janela.avisar("Tarefa em andamento.");
                janela.atualizar();
            }));
        }
        if ("EM_ANDAMENTO".equals(tarefa.getSituacao())) {
            linha.getChildren().add(Pecas.botao("Terminei",
                    () -> pedirTexto("Terminei", "o que foi feito", feito -> {
                        cupula.terminar(id, feito);
                        janela.avisar("Marcada como terminada. Quem pediu vai conferir antes "
                                + "de fechar.");
                        janela.atualizar();
                    })));
        }
        if ("AGUARDANDO_REVISAO".equals(tarefa.getSituacao())) {
            linha.getChildren().add(Pecas.botao("Aceitar e fechar", () -> {
                cupula.aceitar(id);
                janela.avisar("Tarefa concluída.");
                janela.atualizar();
            }));
            linha.getChildren().add(Pecas.botaoPerigo("Mandar refazer",
                    () -> pedirTexto("Faltou alguma coisa", "o que precisa mudar", motivo -> {
                        cupula.mandarRefazer(id, motivo);
                        janela.avisar("Voltou para quem fez, com o motivo escrito.");
                        janela.atualizar();
                    })));
        }
        if (tarefa.aberta()) {
            linha.getChildren().add(Pecas.botaoVazado("Pedir posição",
                    () -> pedirTexto("Como está isso", "o que perguntar", texto -> {
                        cupula.cobrar(id, texto);
                        janela.avisar("Cobrança registrada no fio da tarefa.");
                        janela.atualizar();
                    })));
            linha.getChildren().add(Pecas.botaoVazado("Trocar responsável",
                    () -> janelaDeTrocarResponsavel(tarefa)));
            linha.getChildren().add(Pecas.botaoVazado("Mudar prazo",
                    () -> janelaDeMudarPrazo(tarefa)));
            linha.getChildren().add(Pecas.botaoVazado("Acompanhar",
                    () -> janelaDeAcompanhar(tarefa)));
            linha.getChildren().add(Pecas.botaoPerigo("Cancelar",
                    () -> pedirTexto("Não vai acontecer", "por que está cancelando", motivo -> {
                        cupula.cancelar(id, motivo);
                        janela.avisar("Tarefa cancelada, com o motivo guardado.");
                        janela.atualizar();
                    })));
        }
        return linha;
    }

    private void comentar(UUID id, String texto) {
        if (texto == null || texto.isBlank()) {
            janela.reclamar("Escreva alguma coisa antes de mandar.");
            return;
        }
        cupula.comentar(id, texto.trim(), "COMENTARIO");
        janela.avisar("Escrito no fio da tarefa.");
        janela.atualizar();
    }

    private void pedirTexto(String titulo, String oQue,
                            java.util.function.Consumer<String> entao) {
        javafx.scene.control.TextArea texto = new javafx.scene.control.TextArea();
        texto.setPromptText(oQue);
        texto.setPrefRowCount(3);
        texto.setWrapText(true);
        JanelaFlutuante.estreita(janela.palco(), titulo,
                        "Sem isso escrito, a ação não acontece.")
                .com(Pecas.campo(oQue, texto))
                .acao(titulo, () -> {
                    if (texto.getText().isBlank()) {
                        janela.reclamar("Sem isso escrito, a ação não acontece.");
                        return false;
                    }
                    entao.accept(texto.getText().trim());
                    return true;
                })
                .abrir();
    }

    /** O tipo escolhido no menu, que a tela de criar já usa. */
    String tipoEscolhido() {
        return tipoEscolhido;
    }

    /** O botão que abre o menu dos tipos, logo abaixo dele. */
    private javafx.scene.control.Button botaoDoNovo() {
        javafx.scene.control.Button botao = new javafx.scene.control.Button("+ Novo");
        botao.getStyleClass().add("botao");
        botao.setOnAction(clique -> menuDoNovo(botao));
        return botao;
    }

    /** O menu do "+ Novo", com os seis tipos que a empresa abre. */
    private void menuDoNovo(javafx.scene.control.Button botao) {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        String[][] tipos = {
            {"TAREFA", "Criar tarefa", "Ticket único",
             "uma coisa só, para uma pessoa ou para o setor"},
            {"DEMANDA", "Nova demanda", "Vários tickets",
             "um nome só e várias tarefas abertas de uma vez"},
            {"DECISAO", "Nova decisão", "Precisa de resposta",
             "uma escolha que alguém tem que bater o martelo e ficar registrada"},
            {"ALERTA", "Alerta informativo", "Só para saber",
             "aviso que precisa ser lido, sem trabalho embutido"},
            {"CHAMADO", "Chamado técnico", "Algo não funciona",
             "problema de sistema para quem cuida disso resolver"},
            {"COMPRA", "Solicitação de compra", "Precisa de autorização",
             "pedido de compra para quem aprova gasto"},
        };
        for (String[] tipo : tipos) {
            Label nome = new Label(tipo[1]);
            nome.getStyleClass().add("titulo-item-menu");

            Label marca = new Label(tipo[2].toUpperCase(new java.util.Locale("pt", "BR")));
            marca.getStyleClass().add("marca-item-menu");

            Label explicacao = new Label(tipo[3]);
            explicacao.getStyleClass().add("dica");
            explicacao.setWrapText(true);
            explicacao.setMaxWidth(330);

            HBox topo = new HBox(12, nome, Pecas.empurrar(), marca);
            topo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            VBox caixa = new VBox(2, topo, explicacao);
            caixa.setPrefWidth(360);

            javafx.scene.control.CustomMenuItem item =
                    new javafx.scene.control.CustomMenuItem(caixa);
            item.setHideOnClick(true);
            String qual = tipo[0];
            item.setOnAction(acao -> {
                tipoEscolhido = qual;
                janela.ir(TelaTarefaNova.class);
            });
            menu.getItems().add(item);
        }
        menu.show(botao, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    /** Passa a tarefa para outra pessoa. */
    private void janelaDeTrocarResponsavel(Tarefa tarefa) {
        javafx.scene.control.ComboBox<String> quem = new javafx.scene.control.ComboBox<>();
        quem.getItems().addAll(cupula.pessoas());
        quem.setEditable(true);
        quem.setMaxWidth(Double.MAX_VALUE);

        JanelaFlutuante.estreita(janela.palco(), "Trocar responsável",
                        "Quem faz muda daqui em diante. O que já foi escrito continua no fio.")
                .com(Pecas.campo("Passar para", quem))
                .acao("Passar", () -> {
                    String paraQuem = quem.getEditor().getText();
                    if (paraQuem == null || paraQuem.isBlank()) {
                        janela.reclamar("Diga para quem a tarefa vai.");
                        return false;
                    }
                    cupula.trocarResponsavel(tarefa.getId(), paraQuem.trim());
                    janela.avisar("Tarefa passada para " + paraQuem.trim() + ".");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Muda a data combinada. */
    private void janelaDeMudarPrazo(Tarefa tarefa) {
        javafx.scene.control.DatePicker novo =
                new javafx.scene.control.DatePicker(tarefa.getPrazo());
        novo.setMaxWidth(Double.MAX_VALUE);

        JanelaFlutuante.estreita(janela.palco(), "Mudar prazo",
                        "A data nova fica registrada no fio da tarefa.")
                .com(Pecas.campo("Novo prazo", novo))
                .acao("Mudar", () -> {
                    if (novo.getValue() == null) {
                        janela.reclamar("Escolha a data nova.");
                        return false;
                    }
                    cupula.mudarPrazo(tarefa.getId(), novo.getValue());
                    janela.avisar("Prazo mudado para " + Pecas.data(novo.getValue()) + ".");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Põe mais alguém para acompanhar a tarefa. */
    private void janelaDeAcompanhar(Tarefa tarefa) {
        javafx.scene.control.ComboBox<String> quem = new javafx.scene.control.ComboBox<>();
        quem.getItems().addAll(cupula.pessoas());
        quem.setEditable(true);
        quem.setMaxWidth(Double.MAX_VALUE);

        VBox jaAcompanham = new VBox(4);
        for (String pessoa : cupula.quemAcompanha(tarefa.getId())) {
            jaAcompanham.getChildren().add(new Label(pessoa));
        }
        if (jaAcompanham.getChildren().isEmpty()) {
            Label ninguem = new Label("Ninguém acompanha esta tarefa ainda.");
            ninguem.getStyleClass().add("dica");
            jaAcompanham.getChildren().add(ninguem);
        }

        JanelaFlutuante.estreita(janela.palco(), "Acompanhar",
                        "Quem acompanha vê a tarefa e recebe o que for escrito nela.")
                .com(Pecas.secao("Quem já acompanha"), Pecas.caixa(jaAcompanham),
                        Pecas.campo("Quem", quem))
                .acao("Acrescentar", () -> {
                    String pessoa = quem.getEditor().getText();
                    if (pessoa == null || pessoa.isBlank()) {
                        janela.reclamar("Diga quem vai acompanhar.");
                        return false;
                    }
                    cupula.acrescentarAcompanhante(tarefa.getId(), pessoa.trim());
                    janela.avisar(pessoa.trim() + " passou a acompanhar.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    /** Usado pela tela de criar para já abrir a tarefa nova. */
    void abrirEsta(UUID id) {
        this.aberta = id;
        this.aba = "enviadas";
    }
}
