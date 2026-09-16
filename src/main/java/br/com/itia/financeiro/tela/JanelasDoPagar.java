package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.GrupoDeNatureza;
import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.Periodicidade;
import br.com.itia.financeiro.dominio.Recorrencia;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * As janelas que o contas a pagar abre: parcelar uma contratação, as contas que
 * se repetem, e os cadastros de apoio (favorecido, área, natureza, conta e bem).
 *
 * Ficam fora da tela para que a lista de contas continue sendo só a lista.
 */
final class JanelasDoPagar {

    private final ContasAPagar contas;
    private final Janela janela;

    JanelasDoPagar(ContasAPagar contas, Janela janela) {
        this.contas = contas;
        this.janela = janela;
    }

    // -------------------------------------------------------------- parcelar

    /** Divide uma contratação em parcelas, cada uma virando uma conta. */
    void parcelar() {
        ComboBox<Favorecido> favorecido = lista(contas.favorecidosAtivos(), Favorecido::getNome);
        TextField descricao = new TextField();
        descricao.setPromptText("o que está sendo parcelado");
        TextField total = new TextField("0,00");
        TextField vezes = new TextField("2");
        DatePicker primeiro = new DatePicker(LocalDate.now().plusDays(30));
        primeiro.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Natureza> natureza = lista(contas.naturezasAtivas(), Natureza::getNome);
        ComboBox<CentroDeCusto> centro = lista(contas.centrosAtivos(), CentroDeCusto::getNome);

        HBox linha1 = emLinha(Pecas.campo("Favorecido", favorecido),
                Pecas.campo("Valor total (R$)", total),
                Pecas.campo("Em quantas vezes", vezes),
                Pecas.campo("Primeiro vencimento", primeiro));
        HBox linha2 = emLinha(Pecas.campo("Natureza", natureza),
                Pecas.campo("Centro de custo", centro));

        JanelaFlutuante.nova(janela.palco(), "Dividir uma contratação em parcelas",
                        "Cada parcela vira uma conta própria, com o seu vencimento.")
                .com(Pecas.campo("Descrição", descricao), linha1, linha2)
                .acao("Lançar parcelas", () -> {
                    if (favorecido.getValue() == null || descricao.getText().isBlank()) {
                        janela.reclamar("Escolha o favorecido e escreva a descrição.");
                        return false;
                    }
                    List<Obrigacao> criadas = contas.parcelar(favorecido.getValue().getId(),
                            descricao.getText(), TipoDeOperacao.DESPESA, numero(total.getText()),
                            inteiro(vezes.getText(), 2), primeiro.getValue(),
                            id(natureza.getValue() == null ? null : natureza.getValue().getId()),
                            id(centro.getValue() == null ? null : centro.getValue().getId()));
                    janela.avisar(criadas.size() + " parcela(s) lançadas.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // ----------------------------------------------------------- recorrências

    /** As contas que se repetem todo período, e a geração do mês. */
    void recorrencias() {
        ComboBox<YearMonth> mes = new ComboBox<>();
        for (int atras = 0; atras < 12; atras++) {
            mes.getItems().add(YearMonth.now().minusMonths(atras));
        }
        mes.getSelectionModel().selectFirst();
        mes.setMaxWidth(Double.MAX_VALUE);

        TextField descricao = new TextField();
        descricao.setPromptText("aluguel, internet, contador");
        ComboBox<Favorecido> favorecido = lista(contas.favorecidosAtivos(), Favorecido::getNome);
        TextField valor = new TextField("0,00");
        CheckBox variavel = new CheckBox("O valor muda todo período");
        TextField dia = new TextField("10");
        ComboBox<Natureza> natureza = lista(contas.naturezasAtivas(), Natureza::getNome);
        ComboBox<CentroDeCusto> centro = lista(contas.centrosAtivos(), CentroDeCusto::getNome);
        DatePicker inicio = new DatePicker(LocalDate.now());
        inicio.setMaxWidth(Double.MAX_VALUE);

        HBox linha1 = emLinha(Pecas.campo("Favorecido", favorecido),
                Pecas.campo("Valor previsto (R$)", valor),
                Pecas.campo("Dia do vencimento", dia),
                Pecas.campo("Início", inicio));
        HBox linha2 = emLinha(Pecas.campo("Natureza", natureza),
                Pecas.campo("Centro de custo", centro));

        VBox jaCadastradas = new VBox(4);
        for (Recorrencia qual : contas.recorrencias()) {
            jaCadastradas.getChildren().add(new Label(qual.getDescricao() + " · dia "
                    + qual.getDiaVencimento() + " · " + Pecas.dinheiro(qual.getValorPrevisto())));
        }
        if (jaCadastradas.getChildren().isEmpty()) {
            jaCadastradas.getChildren().add(new Label("Nenhuma recorrência cadastrada ainda."));
        }

        JanelaFlutuante.nova(janela.palco(), "Contas que se repetem",
                        "Cadastre uma vez e gere as contas de cada mês com um clique.")
                .com(Pecas.secao("Gerar as contas do mês"),
                        Pecas.caixa(emLinha(Pecas.campo("Mês", mes)),
                                new HBox(Pecas.botao("Gerar contas do mês", () -> {
                                    int quantas = contas.gerarRecorrentes(mes.getValue());
                                    janela.avisar(quantas == 0
                                            ? "Nada a gerar: as contas deste mês já existem."
                                            : quantas + " conta(s) geradas.");
                                    janela.atualizar();
                                }))),
                        Pecas.secao("Recorrências cadastradas"), Pecas.caixa(jaCadastradas),
                        Pecas.secao("Nova recorrência"),
                        Pecas.campo("Descrição", descricao), linha1, variavel, linha2)
                .acao("Cadastrar recorrência", () -> {
                    if (favorecido.getValue() == null || descricao.getText().isBlank()) {
                        janela.reclamar("Escolha o favorecido e escreva a descrição.");
                        return false;
                    }
                    contas.cadastrarRecorrencia(descricao.getText(),
                            favorecido.getValue().getId(),
                            natureza.getValue() == null ? null : natureza.getValue().getId(),
                            centro.getValue() == null ? null : centro.getValue().getId(),
                            TipoDeOperacao.DESPESA, Periodicidade.MENSAL,
                            inteiro(dia.getText(), 10), numero(valor.getText()),
                            variavel.isSelected(), inicio.getValue(), null);
                    janela.avisar("Recorrência cadastrada.");
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    // -------------------------------------------------------------- cadastros

    /** Os cinco cadastros de apoio, numa janela só. */
    void cadastros() {
        TextField nomeFavorecido = new TextField();
        TextField documento = new TextField();
        ComboBox<String> tipoFavorecido = new ComboBox<>();
        tipoFavorecido.getItems().addAll("PESSOA_FISICA", "PESSOA_JURIDICA");
        tipoFavorecido.getSelectionModel().selectFirst();
        tipoFavorecido.setMaxWidth(Double.MAX_VALUE);
        TextField chavePix = new TextField();

        TextField nomeCentro = new TextField();
        TextField descricaoCentro = new TextField();

        TextField codigoNatureza = new TextField();
        TextField nomeNatureza = new TextField();
        ComboBox<GrupoDeNatureza> grupo = new ComboBox<>();
        grupo.getItems().addAll(GrupoDeNatureza.values());
        grupo.getSelectionModel().selectFirst();
        grupo.setMaxWidth(Double.MAX_VALUE);

        TextField nomeConta = new TextField();
        TextField banco = new TextField();
        TextField agencia = new TextField();
        TextField numeroConta = new TextField();
        TextField titular = new TextField();
        TextField saldo = new TextField("0,00");

        TextField patrimonio = new TextField();
        TextField descricaoBem = new TextField();
        TextField responsavel = new TextField();
        javafx.scene.control.DatePicker aquisicao = new javafx.scene.control.DatePicker();
        aquisicao.setMaxWidth(Double.MAX_VALUE);
        TextField valorDoBem = new TextField("0,00");
        ComboBox<br.com.itia.financeiro.dominio.CentroDeCusto> dentroDe =
                lista(contas.centrosAtivos(), br.com.itia.financeiro.dominio.CentroDeCusto::getNome);

        JanelaFlutuante caixa = JanelaFlutuante.nova(janela.palco(),
                "Favorecidos, áreas, naturezas, contas e bens",
                "Os cadastros que o lançamento de conta usa.");

        caixa.com(Pecas.secao("Novo favorecido"),
                Pecas.caixa(emLinha(Pecas.campo("Nome", nomeFavorecido),
                                Pecas.campo("CPF ou CNPJ", documento),
                                Pecas.campo("Tipo", tipoFavorecido),
                                Pecas.campo("Chave PIX", chavePix)),
                        new HBox(Pecas.botao("Cadastrar favorecido", () -> {
                            contas.cadastrarFavorecido(nomeFavorecido.getText(),
                                    documento.getText(), tipoFavorecido.getValue(),
                                    chavePix.getText());
                            janela.avisar("Favorecido cadastrado.");
                            nomeFavorecido.clear();
                            documento.clear();
                            chavePix.clear();
                        }))),

                Pecas.secao("Novo centro de custo"),
                Pecas.caixa(emLinha(Pecas.campo("Nome", nomeCentro),
                                Pecas.campo("Descrição", descricaoCentro)),
                        new HBox(Pecas.botao("Cadastrar centro", () -> {
                            contas.cadastrarCentro(nomeCentro.getText(),
                                    descricaoCentro.getText());
                            janela.avisar("Centro de custo cadastrado.");
                            nomeCentro.clear();
                            descricaoCentro.clear();
                        }))),

                Pecas.secao("Nova natureza"),
                Pecas.caixa(emLinha(Pecas.campo("Código", codigoNatureza),
                                Pecas.campo("Nome", nomeNatureza),
                                Pecas.campo("Grupo", grupo)),
                        new HBox(Pecas.botao("Cadastrar natureza", () -> {
                            contas.cadastrarNatureza(codigoNatureza.getText(),
                                    nomeNatureza.getText(), grupo.getValue(), null);
                            janela.avisar("Natureza cadastrada.");
                            codigoNatureza.clear();
                            nomeNatureza.clear();
                        }))),

                Pecas.secao("Nova conta financeira"),
                Pecas.caixa(emLinha(Pecas.campo("Nome", nomeConta),
                                Pecas.campo("Banco", banco), Pecas.campo("Agência", agencia),
                                Pecas.campo("Conta", numeroConta)),
                        emLinha(Pecas.campo("Titular", titular),
                                Pecas.campo("Saldo inicial (R$)", saldo)),
                        new HBox(Pecas.botao("Cadastrar conta", () -> {
                            contas.cadastrarConta(nomeConta.getText(), "CORRENTE",
                                    banco.getText(), agencia.getText(), numeroConta.getText(),
                                    titular.getText(), numero(saldo.getText()));
                            janela.avisar("Conta financeira cadastrada.");
                            nomeConta.clear();
                            banco.clear();
                            agencia.clear();
                            numeroConta.clear();
                        }))),

                Pecas.secao("Novo bem"),
                Pecas.caixa(emLinha(Pecas.campo("Número patrimonial", patrimonio),
                                Pecas.campo("Descrição", descricaoBem),
                                Pecas.campo("Responsável pela guarda", responsavel)),
                        emLinha(Pecas.campo("Fica dentro de", dentroDe),
                                Pecas.campo("Data da aquisição", aquisicao),
                                Pecas.campo("Valor da aquisição (R$)", valorDoBem)),
                        new HBox(Pecas.botao("Cadastrar bem", () -> {
                            contas.cadastrarBem(patrimonio.getText(), descricaoBem.getText(),
                                    "EQUIPAMENTO",
                                    dentroDe.getValue() == null ? null
                                            : dentroDe.getValue().getId(),
                                    responsavel.getText(),
                                    aquisicao.getValue() == null ? LocalDate.now()
                                            : aquisicao.getValue(),
                                    numero(valorDoBem.getText()));
                            janela.avisar("Bem cadastrado.");
                            patrimonio.clear();
                            descricaoBem.clear();
                        }))));
        caixa.abrir();
        janela.atualizar();
    }

    // ------------------------------------------------------------------ apoio

    private <T> ComboBox<T> lista(List<T> itens, java.util.function.Function<T, String> comoChamar) {
        ComboBox<T> caixa = new ComboBox<>();
        caixa.getItems().addAll(itens);
        caixa.setConverter(new StringConverter<>() {
            @Override
            public String toString(T qual) {
                return qual == null ? "" : comoChamar.apply(qual);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        });
        caixa.getSelectionModel().selectFirst();
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    private HBox emLinha(javafx.scene.Node... partes) {
        HBox linha = new HBox(16, partes);
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        return linha;
    }

    private UUID id(UUID qual) {
        return qual;
    }

    private BigDecimal numero(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private int inteiro(String texto, int seNaoDer) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (RuntimeException naoEhNumero) {
            return seNaoDer;
        }
    }
}
