package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.GrupoDeNatureza;
import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.OrigemDoRegistro;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lançar uma conta a pagar.
 *
 * O básico entra aqui; o rateio e os documentos entram depois, na ficha da
 * própria conta. A classificação é uma só: o plano de conta, em categoria e
 * subcategoria, que dá para criar na hora sem sair do lançamento.
 */
@Component
public class TelaObrigacaoNova implements Tela {

    private final ContasAPagar contas;
    private final ContextoEmpresa contexto;
    private final TransactionTemplate transacao;
    private final TelaPagar pagar;
    private final Janela janela;

    /**
     * O nome completo de cada item do plano, montado enquanto o banco ainda está
     * aberto. Sem isto, a lista quebraria ao tentar ler a categoria de cima
     * depois que a tela já foi montada.
     */
    private final Map<UUID, String> nomeNoPlano = new LinkedHashMap<>();

    public TelaObrigacaoNova(ContasAPagar contas, ContextoEmpresa contexto,
                             TransactionTemplate transacao, @Lazy TelaPagar pagar,
                             @Lazy Janela janela) {
        this.contas = contas;
        this.contexto = contexto;
        this.transacao = transacao;
        this.pagar = pagar;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "pagar";
    }

    @Override
    public Node montar() {
        List<Favorecido> favorecidos = contas.favorecidosAtivos();
        LocalDate hoje = LocalDate.now();

        VBox tela = new VBox(16, Pecas.cabecalho("nova conta", "Lançar conta a pagar",
                "Quem recebe, quanto e quando. O resto entra na ficha da conta.",
                Pecas.botaoVazado("Ver rascunhos", pagar::verRascunhos)));

        if (favorecidos.isEmpty()) {
            tela.getChildren().add(Pecas.vazio(
                    "Nenhum favorecido cadastrado ainda. Cadastre quem recebe antes de lançar."));
            return tela;
        }

        ComboBox<Favorecido> favorecido = new ComboBox<>();
        favorecido.getItems().addAll(favorecidos);
        favorecido.setConverter(new StringConverter<>() {
            @Override
            public String toString(Favorecido quem) {
                return quem == null ? "" : quem.getNome();
            }

            @Override
            public Favorecido fromString(String texto) {
                return null;
            }
        });
        favorecido.getSelectionModel().selectFirst();
        favorecido.setMaxWidth(Double.MAX_VALUE);

        ComboBox<ContaFinanceira> conta = new ComboBox<>();
        conta.getItems().add(null);
        conta.getItems().addAll(contas.contasAtivas());
        conta.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContaFinanceira qual) {
                return qual == null ? "não definida" : qual.getNome();
            }

            @Override
            public ContaFinanceira fromString(String texto) {
                return null;
            }
        });
        conta.getSelectionModel().selectFirst();
        conta.setMaxWidth(Double.MAX_VALUE);

        TextField descricao = new TextField();
        descricao.setPromptText("aluguel da sala, energia, honorários");
        TextField valor = new TextField();
        valor.setPromptText("0,00");

        DatePicker emissao = new DatePicker(hoje);
        DatePicker competencia = new DatePicker(hoje.withDayOfMonth(1));
        DatePicker vencimento = new DatePicker(hoje.plusDays(10));
        emissao.setMaxWidth(Double.MAX_VALUE);
        competencia.setMaxWidth(Double.MAX_VALUE);
        vencimento.setMaxWidth(Double.MAX_VALUE);

        // Quem paga não é escolha: é sempre a empresa que está aberta na hora do
        // lançamento. O campo aparece só para a pessoa conferir em qual empresa está.
        Empresa pagadora = contexto.exigirEmpresa();
        TextField quemPaga = new TextField(pagadora.getNome());
        quemPaga.setEditable(false);
        quemPaga.setFocusTraversable(false);
        quemPaga.getStyleClass().add("campo-travado");
        quemPaga.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Natureza> plano = new ComboBox<>();
        encherPlano(plano, null);
        plano.setConverter(escolha(qual -> nomeNoPlano.getOrDefault(qual.getId(), qual.getNome()),
                "definir depois"));
        plano.setMaxWidth(Double.MAX_VALUE);

        ComboBox<CentroDeCusto> centro = new ComboBox<>();
        encherCentros(centro, null);
        centro.setConverter(escolha(CentroDeCusto::getNome, "definir depois"));
        centro.setMaxWidth(Double.MAX_VALUE);

        TextField pessoa = new TextField();
        pessoa.setPromptText("de quem é a conta, quando é de alguém");
        TextField solicitante = new TextField();
        solicitante.setPromptText("quem pediu");
        TextField observacao = new TextField();

        tela.getChildren().add(Pecas.caixa(
                linha(Pecas.campo("Favorecido", favorecido),
                        Pecas.campo("Descrição", descricao),
                        Pecas.campo("Conta financeira", conta)),
                linha(Pecas.campo("Emissão", emissao),
                        Pecas.campo("Competência", competencia),
                        Pecas.campo("Vencimento", vencimento),
                        Pecas.campo("Valor devido (R$)", valor)),
                linha(Pecas.campo("Quem paga", quemPaga),
                        Pecas.campo("Plano de conta", comMais(plano,
                                "Criar categoria ou subcategoria", () -> novoNoPlano(plano))),
                        Pecas.campo("Centro de custo", comMais(centro,
                                "Criar centro de custo", () -> novoCentro(centro)))),
                linha(Pecas.campo("Pessoa relacionada", pessoa),
                        Pecas.campo("Solicitante", solicitante)),
                linha(Pecas.campo("Observação", observacao)),
                new HBox(12,
                        Pecas.botao("Lançar", () -> lancar(favorecido.getValue(),
                                descricao.getText(), emissao.getValue(),
                                competencia.getValue(), vencimento.getValue(), valor.getText(),
                                conta.getValue(),
                                pagadora, plano.getValue(), centro.getValue(),
                                pessoa.getText(), solicitante.getText(),
                                observacao.getText(), false)),
                        Pecas.botaoVazado("Salvar rascunho", () -> lancar(favorecido.getValue(),
                                descricao.getText(), emissao.getValue(),
                                competencia.getValue(), vencimento.getValue(), valor.getText(),
                                conta.getValue(),
                                pagadora, plano.getValue(), centro.getValue(),
                                pessoa.getText(), solicitante.getText(),
                                observacao.getText(), true)),
                        Pecas.botaoVazado("Cancelar", () -> janela.ir(TelaPagar.class)))));
        return tela;
    }

    // ------------------------------------------------- criar na hora, sem sair

    /**
     * Cria uma categoria nova do plano de conta, ou uma subcategoria dentro de
     * uma categoria que já existe. O código é dado pelo sistema.
     */
    private void novoNoPlano(ComboBox<Natureza> plano) {
        ComboBox<Natureza> dentroDe = new ComboBox<>();
        dentroDe.getItems().add(null);
        dentroDe.getItems().addAll(categorias());
        dentroDe.setConverter(escolha(Natureza::getNome, "nenhuma: é uma categoria nova"));
        dentroDe.getSelectionModel().selectFirst();
        dentroDe.setMaxWidth(Double.MAX_VALUE);

        TextField nome = new TextField();
        nome.setPromptText("aluguel, energia, software");

        javafx.scene.control.CheckBox livroCaixa =
                new javafx.scene.control.CheckBox("Entra no livro caixa");

        ComboBox<GrupoDeNatureza> grupo = new ComboBox<>();
        grupo.getItems().addAll(GrupoDeNatureza.values());
        grupo.getSelectionModel().select(GrupoDeNatureza.DESPESA);
        grupo.setConverter(escolha(GrupoDeNatureza::getRotulo, ""));
        grupo.setMaxWidth(Double.MAX_VALUE);

        JanelaFlutuante.estreita(janela.palco(), "Novo item do plano de conta",
                        "Deixe a categoria em branco para criar uma categoria nova. "
                                + "Escolhendo uma categoria, o que entra é uma subcategoria dela.")
                .com(Pecas.caixa(
                        Pecas.campo("Dentro da categoria", dentroDe),
                        Pecas.campo("Nome", nome),
                        Pecas.campo("Onde entra no resultado", grupo),
                        livroCaixa))
                .acao("Criar", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome do item do plano de conta.");
                        return false;
                    }
                    Natureza pai = dentroDe.getValue();
                    Natureza criado = transacao.execute(status ->
                            contas.cadastrarNatureza(proximoCodigo(pai), nome.getText().trim(),
                                    grupo.getValue(), pai == null ? null : pai.getId(),
                                    livroCaixa.isSelected()));
                    encherPlano(plano, criado.getId());
                    janela.avisar("Plano de conta: "
                            + nomeNoPlano.getOrDefault(criado.getId(), criado.getNome())
                            + " criado.");
                    return true;
                })
                .abrir();
    }

    /** Cria um centro de custo na hora, sem sair do lançamento. */
    private void novoCentro(ComboBox<CentroDeCusto> centro) {
        TextField nome = new TextField();
        nome.setPromptText("fiscal, contábil, comercial");
        TextField descricao = new TextField();
        descricao.setPromptText("para que serve esta área");

        JanelaFlutuante.estreita(janela.palco(), "Novo centro de custo",
                        "A área que absorve o custo desta conta.")
                .com(Pecas.caixa(Pecas.campo("Nome", nome),
                        Pecas.campo("Descrição", descricao)))
                .acao("Criar", () -> {
                    if (nome.getText() == null || nome.getText().isBlank()) {
                        janela.reclamar("Escreva o nome do centro de custo.");
                        return false;
                    }
                    CentroDeCusto criado = transacao.execute(status ->
                            contas.cadastrarCentro(nome.getText().trim(), descricao.getText()));
                    encherCentros(centro, criado.getId());
                    janela.avisar("Centro de custo " + criado.getNome() + " criado.");
                    return true;
                })
                .abrir();
    }

    /** As categorias do plano: as que não estão dentro de nenhuma outra. */
    private List<Natureza> categorias() {
        return transacao.execute(status -> contas.naturezasAtivas().stream()
                .filter(qual -> qual.getPai() == null).toList());
    }

    /**
     * O código do item novo, dado pelo sistema: a subcategoria continua o código
     * da categoria, e a categoria nova entra no fim da fila.
     */
    private String proximoCodigo(Natureza pai) {
        List<Natureza> todas = transacao.execute(status -> {
            List<Natureza> tudo = contas.naturezasTodas();
            tudo.forEach(qual -> {
                if (qual.getPai() != null) {
                    qual.getPai().getId();
                }
            });
            return tudo;
        });
        if (pai == null) {
            int maior = 0;
            for (Natureza qual : todas) {
                if (qual.getPai() == null) {
                    maior = Math.max(maior, primeiroNumero(qual.getCodigo()));
                }
            }
            return String.valueOf(maior + 1);
        }
        int quantos = 0;
        for (Natureza qual : todas) {
            if (qual.getPai() != null && qual.getPai().getId().equals(pai.getId())) {
                quantos++;
            }
        }
        return pai.getCodigo() + "." + (quantos + 1);
    }

    private int primeiroNumero(String codigo) {
        if (codigo == null) {
            return 0;
        }
        StringBuilder so = new StringBuilder();
        for (char letra : codigo.toCharArray()) {
            if (Character.isDigit(letra)) {
                so.append(letra);
            } else {
                break;
            }
        }
        return so.isEmpty() ? 0 : Integer.parseInt(so.toString());
    }

    private void encherPlano(ComboBox<Natureza> plano, UUID escolher) {
        List<Natureza> itens = transacao.execute(status -> {
            List<Natureza> tudo = contas.naturezasAtivas();
            nomeNoPlano.clear();
            tudo.forEach(qual -> nomeNoPlano.put(qual.getId(), qual.getCaminho()
                    + (qual.isLivroCaixa() ? "  ·  livro caixa" : "")));
            return tudo;
        });
        plano.getItems().clear();
        plano.getItems().add(null);
        plano.getItems().addAll(itens);
        plano.getSelectionModel().selectFirst();
        if (escolher != null) {
            itens.stream().filter(qual -> qual.getId().equals(escolher)).findFirst()
                    .ifPresent(qual -> plano.getSelectionModel().select(qual));
        }
    }

    private void encherCentros(ComboBox<CentroDeCusto> centro, UUID escolher) {
        List<CentroDeCusto> itens = transacao.execute(status -> contas.centrosAtivos());
        centro.getItems().clear();
        centro.getItems().add(null);
        centro.getItems().addAll(itens);
        centro.getSelectionModel().selectFirst();
        if (escolher != null) {
            itens.stream().filter(qual -> qual.getId().equals(escolher)).findFirst()
                    .ifPresent(qual -> centro.getSelectionModel().select(qual));
        }
    }

    /** A lista com o botão de criar do lado, para não precisar sair da tela. */
    private HBox comMais(Node lista, String oQueFaz, Runnable acao) {
        Button mais = Pecas.botaoVazado("+", acao);
        mais.getStyleClass().add("botao-mais");
        javafx.scene.control.Tooltip.install(mais, new javafx.scene.control.Tooltip(oQueFaz));

        HBox junto = new HBox(8, lista, mais);
        junto.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lista, Priority.ALWAYS);
        return junto;
    }

    // ------------------------------------------------------------------ apoio

    private <T> StringConverter<T> escolha(java.util.function.Function<T, String> comoChama,
                                           String quandoNulo) {
        return new StringConverter<>() {
            @Override
            public String toString(T qual) {
                return qual == null ? quandoNulo : comoChama.apply(qual);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        };
    }

    private HBox linha(Node... campos) {
        HBox linha = new HBox(16, campos);
        for (Node campo : campos) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return linha;
    }

    private void lancar(Favorecido favorecido, String descricao,
                        LocalDate emissao, LocalDate competencia, LocalDate vencimento,
                        String valor, ContaFinanceira conta, Empresa pagadora,
                        Natureza plano, CentroDeCusto centro, String pessoa,
                        String solicitante, String observacao, boolean rascunho) {
        if (favorecido == null || descricao == null || descricao.isBlank()) {
            janela.reclamar("Escolha o favorecido e escreva a descrição.");
            return;
        }
        BigDecimal quanto = TelaTituloNovo.dinheiro(valor);
        if (quanto == null || quanto.signum() <= 0) {
            janela.reclamar("Escreva o valor da conta, maior que zero.");
            return;
        }
        Obrigacao nova = contas.lancar(favorecido.getId(), descricao.trim(),
                TipoDeOperacao.DESPESA, emissao,
                competencia, vencimento, quanto, conta == null ? null : conta.getId(),
                pagadora.getId(), pessoa, null,
                null, solicitante, observacao, rascunho,
                OrigemDoRegistro.MANUAL, null);
        if (plano != null || centro != null) {
            contas.adicionarItem(nova.getId(), descricao.trim(), BigDecimal.ONE, quanto,
                    plano == null ? null : plano.getId(),
                    centro == null ? null : centro.getId(), pessoa, null);
        }
        janela.avisar(rascunho
                ? "Rascunho " + nova.getNumero() + " guardado. Ele fica em Rascunhos até ser "
                        + "conferido."
                : "Conta " + nova.getNumero() + " lançada.");
        janela.ir(TelaObrigacao.class, nova.getId());
    }
}
