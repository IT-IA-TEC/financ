package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import br.com.itia.financeiro.marca.ItiaFonts;
import br.com.itia.financeiro.marca.ItiaTokens;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A janela do IT.FC: a moldura que não muda.
 *
 * Em cima, a faixa preta com a marca, o menu, a empresa em que a pessoa está e
 * a bolinha do perfil. No meio, a tela aberta. É o mesmo desenho de sempre, só
 * que agora em janela.
 *
 * Quem troca de tela passa por aqui, e é aqui que a tela é montada de novo:
 * assim nenhuma tela mostra número velho de quando foi aberta pela primeira
 * vez.
 */
@Component
public class Janela {

    /** O menu, na mesma ordem de sempre, agora separado por grupo. */
    private static final Map<String, String> MENU = new LinkedHashMap<>();

    /** Cada grupo da coluna lateral, com as partes que vão dentro dele. */
    private static final Map<String, java.util.List<String>> GRUPOS = new LinkedHashMap<>();

    static {
        MENU.put("painel", "Painel");
        MENU.put("cupula", "Tarefas");
        MENU.put("titulos", "Contas a receber");
        MENU.put("pagar", "Contas a pagar");
        MENU.put("conciliacao", "Conciliação");
        MENU.put("cobranca", "Cobrança");
        MENU.put("clientes", "Clientes");
        MENU.put("servicos", "Pacotes & serviços");
        MENU.put("integracoes", "Integrações");
        MENU.put("inteligencia", "Inteligência");
        MENU.put("etiquetas", "Tags");

        GRUPOS.put("Operação", java.util.List.of("painel", "cupula", "clientes"));
        GRUPOS.put("Dinheiro", java.util.List.of("titulos", "pagar", "conciliacao", "cobranca"));
        GRUPOS.put("Cadastro", java.util.List.of("servicos", "etiquetas"));
        GRUPOS.put("Sistema", java.util.List.of("integracoes", "inteligencia"));
    }

    private final ApplicationContext molas;
    private final ContextoEmpresa contexto;
    private final TransactionTemplate transacao;
    private final br.com.itia.financeiro.servico.Acessos acessos;
    private final br.com.itia.financeiro.servico.FinanceiroServico financeiro;

    private Stage palco;
    private final BorderPane raiz = new BorderPane();
    private final BorderPane areaDaDireita = new BorderPane();
    private VBox barraDoTopo;
    private final VBox recados = new VBox(8);
    /** A coluna lateral, com os itens do menu um embaixo do outro. */
    private final VBox coluna = new VBox(0);
    private final VBox itensDoMenu = new VBox(2);
    private final HBox rodape = new HBox(10);
    private final Label chipDaEmpresa = new Label();
    private final Label perfil = new Label();
    private final Label trilha = new Label();
    private final ScrollPane rolagem = new ScrollPane();
    private Class<? extends Tela> telaAberta;

    /** Guarda cada item da coluna pela seção, para acender o certo. */
    private final Map<String, HBox> itensPorSecao = new LinkedHashMap<>();

    /** Por onde a pessoa passou, para a seta voltar uma tela de cada vez. */
    private final java.util.Deque<Runnable> historico = new java.util.ArrayDeque<>();
    /** Enquanto está voltando, o caminho não é guardado de novo. */
    private boolean voltando;
    private final javafx.scene.layout.StackPane setaDeVoltar =
            new javafx.scene.layout.StackPane();


    public Janela(ApplicationContext molas, ContextoEmpresa contexto,
                  TransactionTemplate transacao,
                  br.com.itia.financeiro.servico.Acessos acessos,
                  br.com.itia.financeiro.servico.FinanceiroServico financeiro) {
        this.molas = molas;
        this.contexto = contexto;
        this.transacao = transacao;
        this.acessos = acessos;
        this.financeiro = financeiro;
    }

    // ------------------------------------------------------------------ abrir

    public void abrir(Stage palco) {
        this.palco = palco;
        ItiaFonts.load();

        rolagem.getStyleClass().add("rolagem");
        rolagem.setFitToWidth(true);

        barraDoTopo = topo();
        areaDaDireita.setTop(barraDoTopo);
        areaDaDireita.setCenter(rolagem);
        areaDaDireita.getStyleClass().add("area-de-trabalho");
        BorderPane.setMargin(barraDoTopo, new Insets(12, 12, 0, 12));

        VBox lateral = colunaLateral();
        raiz.setLeft(lateral);
        BorderPane.setMargin(lateral, new Insets(12, 0, 12, 12));
        raiz.setCenter(areaDaDireita);
        raiz.getStyleClass().add("moldura");

        Scene cena = new Scene(raiz, 1400, 820);
        // primeiro as folhas da marca (cores e tipografia oficiais), depois as do sistema
        ItiaTokens.applyStylesheets(cena);
        cena.getStylesheets().add(
                Janela.class.getResource("/estilo/itia.css").toExternalForm());

        palco.setTitle("IT.FC");
        palco.setScene(cena);
        palco.setMinWidth(1100);
        palco.setMinHeight(680);
        palco.setMaximized(true);
        palco.show();

        entrarSozinhoSePedido();
        String direto = System.getProperty("itfc.tela");
        if (contexto.temEmpresaEscolhida() && direto != null && !direto.isBlank()) {
            irPorSecao(direto);
        } else if (contexto.temEmpresaEscolhida()) {
            ir(TelaPainel.class);
        } else if (contexto.estaLogado()) {
            // já se identificou, mas ainda não disse em qual empresa vai trabalhar
            ir(TelaEmpresas.class);
        } else {
            ir(TelaEntrar.class);
        }
    }

    /**
     * Atalho de conferência: abrir o programa com
     * -Ditfc.entrarComo="Nome" -Ditfc.empresa=apelido já entra direto, para dar
     * para percorrer as telas sem digitar. No uso normal ninguém passa isso, e
     * então nada muda.
     */
    private void entrarSozinhoSePedido() {
        String quem = System.getProperty("itfc.entrarComo");
        String empresa = System.getProperty("itfc.empresa");
        if (quem == null || quem.isBlank()) {
            return;
        }
        transacao.execute(status -> {
            contexto.entrar(quem, null);
            contexto.identificar(acessos.identificar(quem, null).getId());
            if (empresa != null && !empresa.isBlank()) {
                financeiro.empresasAtivas().stream()
                        .filter(e -> empresa.equalsIgnoreCase(e.getApelido()))
                        .findFirst()
                        .ifPresent(e -> {
                            contexto.escolher(e.getId());
                            contexto.assumirPapel(acessos
                                    .crachaDe(contexto.getUsuarioId(), e.getId()).getPapel());
                        });
            }
            return null;
        });
    }

    /** Abre uma tela, montando ela na hora. */
    public void ir(Class<? extends Tela> qual) {
        guardarNoCaminho();
        Tela tela = molas.getBean(qual);
        this.telaAberta = qual;
        try {
            // a tela é montada dentro de uma leitura no banco: assim ela pode
            // percorrer os dados ligados sem esbarrar em conexão fechada
            Node conteudo = transacao.execute(status -> tela.montar());

            // a porta de entrada ocupa a janela inteira: sem faixa, sem coluna
            areaDaDireita.setTop(tela.ocupaTudo() ? null : barraDoTopo);
            raiz.setLeft(tela.ocupaTudo() ? null : coluna);

            if (tela.ocupaTudo()) {
                areaDaDireita.setCenter(conteudo);
            } else {
                VBox area = new VBox(16);
                area.getStyleClass().add("conteudo");
                area.setPadding(Pecas.folga());
                if (!recados.getChildren().isEmpty()) {
                    area.getChildren().add(recados);
                }
                area.getChildren().add(conteudo);
                VBox.setVgrow(conteudo, Priority.ALWAYS);

                if (tela.cabeNaTela()) {
                    // a tela se vira dentro do espaço que tem: nada rola por fora
                    areaDaDireita.setCenter(area);
                    BorderPane.setMargin(area, new Insets(12));
                } else {
                    rolagem.setContent(area);
                    rolagem.setVvalue(0);
                    areaDaDireita.setCenter(rolagem);
                    BorderPane.setMargin(rolagem, new Insets(12));
                }
            }
            acenderMenu(tela.secao());
            atualizarTopo();
        } catch (RuntimeException problema) {
            mostrarProblema(problema);
        }
    }

    /** Abre uma tela que mostra um registro só, dizendo antes qual é. */
    public void ir(Class<? extends TelaDeUmSo> qual, java.util.UUID registro) {
        molas.getBean(qual).escolher(registro);
        ir((Class<? extends Tela>) qual);
    }

    /**
     * Guarda a tela que está aberta antes de trocar, para a seta conseguir
     * voltar uma de cada vez. Quem está voltando não guarda nada, senão a
     * pessoa ficaria presa indo e voltando entre as duas mesmas telas.
     */
    private void guardarNoCaminho() {
        if (voltando || telaAberta == null) {
            return;
        }
        Class<? extends Tela> qual = telaAberta;
        historico.push(() -> ir(qual));
        if (historico.size() > 40) {
            historico.removeLast();
        }
    }

    /** A seta do topo: volta uma tela, na ordem em que a pessoa passou. */
    private void voltar() {
        if (historico.isEmpty()) {
            return;
        }
        Runnable passo = historico.pop();
        voltando = true;
        try {
            passo.run();
        } finally {
            voltando = false;
        }
    }

    /** Monta de novo a tela que está aberta. */
    public void atualizar() {
        if (telaAberta != null) {
            ir(telaAberta);
        }
    }

    // ----------------------------------------------------------- recados

    /** O aviso verde-escuro do sistema: o que acabou de acontecer. */
    public void avisar(String texto) {
        recado(texto, false);
    }

    /** O aviso de erro, com o fio vermelho. */
    public void reclamar(String texto) {
        recado(texto, true);
    }

    private void recado(String texto, boolean erro) {
        Label linha = new Label(texto);
        linha.getStyleClass().add("recado");
        if (erro) {
            linha.getStyleClass().add("erro");
        }
        linha.setWrapText(true);
        linha.setMaxWidth(Double.MAX_VALUE);
        recados.getChildren().setAll(linha);
    }

    public void limparRecados() {
        recados.getChildren().clear();
    }

    private void mostrarProblema(RuntimeException problema) {
        String texto = problema.getMessage() == null
                ? "Não consegui abrir esta tela." : problema.getMessage();
        reclamar(texto);
        VBox area = new VBox(16);
        area.getStyleClass().add("conteudo");
        area.setPadding(Pecas.folga());
        area.getChildren().addAll(recados, Pecas.vazio("Escolha outro caminho no menu."));
        rolagem.setContent(area);
    }

    // --------------------------------------------------------------- o topo

    private VBox topo() {
        trilha.getStyleClass().add("trilha");

        setaDeVoltar.getChildren().add(br.com.itia.financeiro.marca.Icones.voltar(16,
                javafx.scene.paint.Color.web("#52525B")));
        setaDeVoltar.getStyleClass().add("seta-de-voltar");
        setaDeVoltar.setOnMouseClicked(clique -> voltar());

        Label busca = new Label("Buscar");
        busca.getStyleClass().add("busca-topo");
        busca.setGraphic(br.com.itia.financeiro.marca.Icones.busca(15,
                javafx.scene.paint.Color.web("#6B6B72")));
        busca.setGraphicTextGap(8);

        javafx.scene.layout.StackPane sino = new javafx.scene.layout.StackPane(
                br.com.itia.financeiro.marca.Icones.sino(17,
                        javafx.scene.paint.Color.web("#111114")));
        sino.getStyleClass().add("sino-topo");

        chipDaEmpresa.getStyleClass().add("empresa-atual");
        chipDaEmpresa.setOnMouseClicked(clique -> listaDeEmpresas());

        perfil.getStyleClass().add("perfil");
        perfil.setOnMouseClicked(clique -> listaDoPerfil());

        HBox faixa = new HBox(12, setaDeVoltar, trilha, Pecas.empurrar(), busca, sino,
                chipDaEmpresa, perfil);
        faixa.setAlignment(Pos.CENTER_LEFT);
        faixa.setPadding(new Insets(14, 18, 14, 18));

        VBox barra = new VBox(faixa);
        barra.getStyleClass().add("topo");
        return barra;
    }


    // ------------------------------------------------ as listas que abrem ali

    /**
     * A lista de empresas abre em cima do próprio botão: trocar de empresa é um
     * clique, sem sair da tela em que a pessoa está.
     */
    private void listaDeEmpresas() {
        javafx.scene.control.ContextMenu lista = menuNoPadrao();
        java.util.UUID atual = contexto.temEmpresaEscolhida() ? contexto.exigirEmpresaId() : null;
        double largura = chipDaEmpresa.getWidth();

        transacao.execute(status -> {
            for (br.com.itia.financeiro.dominio.Empresa empresa : financeiro.empresasAtivas()) {
                boolean escolhida = empresa.getId().equals(atual);
                lista.getItems().add(itemDeMenu(empresa.getNome(), escolhida, false, largura,
                        () -> trocarDeEmpresa(empresa.getId())));
            }
            return null;
        });
        lista.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        lista.getItems().add(itemDeMenu("Ver todas as empresas", false, false, largura,
                () -> ir(TelaEmpresas.class)));
        lista.getItems().add(itemDeMenu("Configurar empresas", false, false, largura,
                () -> ir(TelaEmpresasConfigurar.class)));
        lista.setPrefWidth(largura);
        lista.show(chipDaEmpresa, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    /** Troca a empresa sem sair da tela: o crachá muda junto. */
    private void trocarDeEmpresa(java.util.UUID empresaId) {
        transacao.execute(status -> {
            contexto.escolher(empresaId);
            if (contexto.getUsuarioId() != null) {
                contexto.assumirPapel(acessos.crachaDe(contexto.getUsuarioId(), empresaId)
                        .getPapel());
            }
            return null;
        });
        limparRecados();
        ir(TelaPainel.class);
    }

    /** O que dá para fazer a partir da bolinha do perfil. */
    private void listaDoPerfil() {
        javafx.scene.control.ContextMenu lista = menuNoPadrao();
        double largura = 236;
        lista.getItems().add(itemDeMenu("Meu perfil", false, false, largura,
                () -> ir(TelaPerfil.class)));
        lista.getItems().add(itemDeMenu("Regras desta empresa", false, false, largura,
                () -> ir(TelaRegras.class)));
        lista.getItems().add(itemDeMenu("Pessoas da empresa", false, false, largura,
                () -> ir(TelaPessoasDaEmpresa.class)));
        lista.getItems().add(itemDeMenu("Configurar empresas", false, false, largura,
                () -> ir(TelaEmpresasConfigurar.class)));
        lista.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        lista.getItems().add(itemDeMenu("Trocar de empresa", false, false, largura,
                () -> ir(TelaEmpresas.class)));
        lista.getItems().add(itemDeMenu("Sair", false, true, largura, this::sair));
        lista.setPrefWidth(largura);
        lista.show(perfil, javafx.geometry.Side.BOTTOM, -200, 6);
    }

    /** Sai da conta e volta para a porta de entrada. */
    private void sair() {
        contexto.sair();
        historico.clear();
        telaAberta = null;
        limparRecados();
        ir(TelaEntrar.class);
    }

    private javafx.scene.control.ContextMenu menuNoPadrao() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        menu.getStyleClass().add("lista-suspensa");
        return menu;
    }

    /**
     * Uma linha da lista suspensa: cada item cabe numa linha só e, quando o
     * nome é grande demais, termina em reticências. A largura acompanha o
     * botão que abriu a lista, para uma coisa encaixar na outra.
     */
    private javafx.scene.control.CustomMenuItem itemDeMenu(String texto, boolean escolhido,
                                                           boolean perigo, double largura,
                                                           Runnable acao) {
        Label nome = new Label(texto);
        nome.getStyleClass().add("nome-do-suspenso");
        if (perigo) {
            nome.getStyleClass().add("perigo");
        }
        nome.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        nome.setWrapText(false);
        nome.setMaxWidth(largura - 40);

        HBox caixa = new HBox(nome);
        caixa.getStyleClass().add("item-suspenso");
        if (escolhido) {
            caixa.getStyleClass().add("escolhido");
        }
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.setMinWidth(largura - 24);
        caixa.setPrefWidth(largura - 24);
        caixa.setMaxWidth(largura - 24);

        javafx.scene.control.CustomMenuItem item =
                new javafx.scene.control.CustomMenuItem(caixa);
        item.setHideOnClick(true);
        item.setOnAction(clique -> acao.run());
        return item;
    }

    // ------------------------------------------------------- a coluna lateral

    /**
     * A coluna lateral escura: a marca em cima, os grupos do menu no meio e
     * quem está usando o sistema no pé. É a mesma ordem da identidade.
     */
    private VBox colunaLateral() {
        coluna.getStyleClass().add("coluna-lateral");
        coluna.setPrefWidth(252);
        coluna.setMinWidth(252);
        coluna.setMaxWidth(252);

        itensDoMenu.getStyleClass().add("itens-do-menu");
        rodape.getStyleClass().add("rodape-da-coluna");
        ScrollPane rolagemDoMenu = new ScrollPane(itensDoMenu);
        rolagemDoMenu.setFitToWidth(true);
        rolagemDoMenu.getStyleClass().add("rolagem-do-menu");
        rolagemDoMenu.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(rolagemDoMenu, Priority.ALWAYS);

        coluna.getChildren().addAll(marcaDaColuna(), rolagemDoMenu, rodape);
        return coluna;
    }

    /** A marca no alto da coluna. */
    private HBox marcaDaColuna() {
        Label bolinha = new Label("IT");
        bolinha.getStyleClass().add("bolinha-da-marca");

        javafx.scene.layout.Pane nome = br.com.itia.financeiro.marca.TrackedText.bicolor(
                "IT.FC", br.com.itia.financeiro.marca.ItiaFonts.logotype(17),
                br.com.itia.financeiro.marca.ItiaTokens.LOGO_TRACKING,
                javafx.scene.paint.Color.WHITE, ".",
                br.com.itia.financeiro.marca.ItiaTokens.RED);

        HBox faixa = new HBox(10, bolinha, nome);
        faixa.getStyleClass().add("marca-da-coluna");
        faixa.setAlignment(Pos.CENTER_LEFT);
        return faixa;
    }

    /** Quem está usando o sistema, no pé da coluna. */
    private void montarRodape() {
        rodape.getChildren().clear();
        Label iniciais = new Label(contexto.getIniciais());
        iniciais.getStyleClass().add("avatar-da-coluna");

        Label nome = new Label(contexto.getNome() == null ? "" : contexto.getNome());
        nome.getStyleClass().add("nome-da-coluna");

        Label papel = new Label(contexto.getPapel() == null ? ""
                : contexto.getPapel().name().toLowerCase(new java.util.Locale("pt", "BR")));
        papel.getStyleClass().add("papel-da-coluna");

        VBox quem = new VBox(1, nome, papel);
        rodape.getChildren().addAll(iniciais, quem);
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.setOnMouseClicked(clique -> ir(TelaPerfil.class));
    }

    /** O menu só existe depois que a pessoa entrou e escolheu a empresa. */
    private void atualizarTopo() {
        boolean dentro = contexto.estaLogado() && contexto.temEmpresaEscolhida();

        itensDoMenu.getChildren().clear();
        itensPorSecao.clear();
        if (dentro) {
            for (Map.Entry<String, java.util.List<String>> grupo : GRUPOS.entrySet()) {
                Label nome = new Label(grupo.getKey()
                        .toUpperCase(new java.util.Locale("pt", "BR")));
                nome.getStyleClass().add("grupo-da-coluna");
                itensDoMenu.getChildren().add(nome);
                for (String secao : grupo.getValue()) {
                    HBox item = itemDaColuna(secao, MENU.get(secao));
                    itensPorSecao.put(secao, item);
                    itensDoMenu.getChildren().add(item);
                }
            }
        }
        coluna.setVisible(dentro);
        coluna.setManaged(dentro);
        montarRodape();

        chipDaEmpresa.setVisible(dentro);
        chipDaEmpresa.setManaged(dentro);
        if (dentro) {
            chipDaEmpresa.setText(("empresa: "
                    + transacao.execute(status -> contexto.exigirEmpresa().getNome()))
                    .toUpperCase(new java.util.Locale("pt", "BR")) + "  ⌄");
        }

        boolean logado = contexto.estaLogado();
        perfil.setVisible(logado);
        perfil.setManaged(logado);
        perfil.setText(contexto.getIniciais());
        barraDoTopo.setVisible(logado);
        barraDoTopo.setManaged(logado);

        if (telaAberta != null) {
            String secao = molas.getBean(telaAberta).secao();
            acenderMenu(secao);
            trilha.setText("Início  ›  " + MENU.getOrDefault(secao, ""));
        }
        setaDeVoltar.setDisable(historico.isEmpty());
        setaDeVoltar.setOpacity(historico.isEmpty() ? 0.35 : 1);
    }

    /** Uma parte do menu na coluna: o ícone, o nome e a marca de escolhido. */
    private HBox itemDaColuna(String secao, String nome) {
        // a marca do escolhido: um risco laranja arredondado, colado na esquerda
        javafx.scene.layout.Region marca = new javafx.scene.layout.Region();
        marca.getStyleClass().add("marca-do-item");
        marca.setVisible(false);

        javafx.scene.layout.StackPane icone = br.com.itia.financeiro.marca.Icones.daSecao(
                secao, 18, javafx.scene.paint.Color.web("#D4D4D8"));

        Label rotulo = new Label(nome);
        rotulo.getStyleClass().add("nome-do-item");

        HBox item = new HBox(11, marca, icone, rotulo);
        item.getStyleClass().add("item-da-coluna");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setOnMouseClicked(clique -> irPorSecao(secao));
        return item;
    }

    private void acenderMenu(String secao) {
        itensPorSecao.forEach((qual, item) -> {
            boolean escolhido = qual.equals(secao);
            item.getStyleClass().remove("ativo");
            if (escolhido) {
                item.getStyleClass().add("ativo");
            }
            // o risco laranja e o ícone aceso só no item escolhido
            item.getChildren().get(0).setVisible(escolhido);
            item.getChildren().set(1, br.com.itia.financeiro.marca.Icones.daSecao(qual, 18,
                    javafx.scene.paint.Color.web(escolhido ? "#FE4901" : "#D4D4D8")));
        });
    }

    /** Leva para a tela principal de cada parte do menu. */
    private void irPorSecao(String secao) {
        limparRecados();
        switch (secao) {
            case "painel" -> ir(TelaPainel.class);
            case "titulos" -> ir(TelaTitulos.class);
            case "clientes" -> ir(TelaClientes.class);
            case "pagar" -> ir(TelaPagar.class);
            case "cupula", "tarefas" -> ir(TelaTarefas.class);
            case "cobranca" -> ir(TelaDisparos.class);
            case "conversas" -> ir(TelaConversas.class);
            case "esteira" -> ir(TelaEsteira.class);
            case "acordos" -> ir(TelaAcordos.class);
            case "regua" -> ir(TelaRegua.class);
            case "modelos" -> ir(TelaModelos.class);
            case "fluxos" -> ir(TelaFluxos.class);
            case "comprovantes" -> ir(TelaComprovantes.class);
            case "conciliacao" -> ir(TelaConciliacao.class);
            case "servicos" -> ir(TelaServicos.class);
            case "etiquetas" -> ir(TelaEtiquetas.class);
            case "inteligencia" -> ir(TelaInteligencia.class);
            case "integracoes" -> ir(TelaIntegracoes.class);
            default -> {
                // nome desconhecido nunca deixa a janela vazia: cai no painel
                ir(TelaPainel.class);
            }
        }
    }

    // -------------------------------------------------------------- ajudinhas

    public Stage palco() {
        return palco;
    }

    public void fechar() {
        Platform.exit();
    }
}
