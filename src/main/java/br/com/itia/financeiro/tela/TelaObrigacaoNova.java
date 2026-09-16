package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ContaFinanceira;
import br.com.itia.financeiro.dominio.Bem;
import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.OrigemDoRegistro;
import br.com.itia.financeiro.dominio.TipoDeOperacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lançar uma conta a pagar.
 *
 * O básico entra aqui; a classificação por natureza, o rateio e os documentos
 * entram depois, na ficha da própria conta.
 */
@Component
public class TelaObrigacaoNova implements Tela {

    private final ContasAPagar contas;
    private final Janela janela;

    public TelaObrigacaoNova(ContasAPagar contas, @Lazy Janela janela) {
        this.contas = contas;
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
                "Quem recebe, quanto e quando. O resto entra na ficha da conta."));

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

        ComboBox<TipoDeOperacao> tipo = new ComboBox<>();
        tipo.getItems().addAll(TipoDeOperacao.values());
        tipo.getSelectionModel().select(TipoDeOperacao.DESPESA);
        tipo.setMaxWidth(Double.MAX_VALUE);

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

        ComboBox<Empresa> pagadora = new ComboBox<>();
        pagadora.getItems().add(null);
        pagadora.getItems().addAll(contas.empresasDoGrupo());
        pagadora.setConverter(escolha(Empresa::getNome, "a própria empresa"));
        pagadora.getSelectionModel().selectFirst();
        pagadora.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Natureza> natureza = new ComboBox<>();
        natureza.getItems().add(null);
        natureza.getItems().addAll(contas.naturezasAtivas());
        natureza.setConverter(escolha(Natureza::getNome, "definir depois"));
        natureza.getSelectionModel().selectFirst();
        natureza.setMaxWidth(Double.MAX_VALUE);

        ComboBox<CentroDeCusto> centro = new ComboBox<>();
        centro.getItems().add(null);
        centro.getItems().addAll(contas.centrosAtivos());
        centro.setConverter(escolha(CentroDeCusto::getNome, "definir depois"));
        centro.getSelectionModel().selectFirst();
        centro.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Bem> bem = new ComboBox<>();
        bem.getItems().add(null);
        bem.getItems().addAll(contas.bensAtivos());
        bem.setConverter(escolha(Bem::getDescricao, "nenhum"));
        bem.getSelectionModel().selectFirst();
        bem.setMaxWidth(Double.MAX_VALUE);

        TextField pessoa = new TextField();
        pessoa.setPromptText("de quem é a conta, quando é de alguém");
        TextField solicitante = new TextField();
        solicitante.setPromptText("quem pediu");
        TextField observacao = new TextField();

        ComboBox<String> situacaoDoCadastro = new ComboBox<>();
        situacaoDoCadastro.getItems().addAll("completo", "rascunho, falta informação");
        situacaoDoCadastro.getSelectionModel().selectFirst();
        situacaoDoCadastro.setMaxWidth(Double.MAX_VALUE);

        tela.getChildren().add(Pecas.caixa(
                linha(Pecas.campo("Favorecido", favorecido),
                        Pecas.campo("Descrição", descricao),
                        Pecas.campo("Tipo de operação", tipo)),
                linha(Pecas.campo("Emissão", emissao),
                        Pecas.campo("Competência", competencia),
                        Pecas.campo("Vencimento", vencimento),
                        Pecas.campo("Valor devido (R$)", valor)),
                linha(Pecas.campo("Conta financeira", conta),
                        Pecas.campo("Quem paga", pagadora),
                        Pecas.campo("Natureza", natureza),
                        Pecas.campo("Centro de custo", centro)),
                linha(Pecas.campo("Pessoa relacionada", pessoa),
                        Pecas.campo("Bem relacionado", bem),
                        Pecas.campo("Solicitante", solicitante),
                        Pecas.campo("Situação do cadastro", situacaoDoCadastro)),
                linha(Pecas.campo("Observação", observacao)),
                new HBox(12,
                        Pecas.botao("Lançar", () -> lancar(favorecido.getValue(),
                                descricao.getText(), tipo.getValue(), emissao.getValue(),
                                competencia.getValue(), vencimento.getValue(), valor.getText(),
                                conta.getValue(),
                                pagadora.getValue(), natureza.getValue(), centro.getValue(),
                                pessoa.getText(), bem.getValue(), solicitante.getText(),
                                observacao.getText(),
                                situacaoDoCadastro.getSelectionModel().getSelectedIndex() == 1)),
                        Pecas.botaoVazado("Cancelar", () -> janela.ir(TelaPagar.class)))));
        return tela;
    }

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

    private void lancar(Favorecido favorecido, String descricao, TipoDeOperacao tipo,
                        LocalDate emissao, LocalDate competencia, LocalDate vencimento,
                        String valor, ContaFinanceira conta, Empresa pagadora,
                        Natureza natureza, CentroDeCusto centro, String pessoa, Bem bem,
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
        Obrigacao nova = contas.lancar(favorecido.getId(), descricao.trim(), tipo, emissao,
                competencia, vencimento, quanto, conta == null ? null : conta.getId(),
                pagadora == null ? null : pagadora.getId(), pessoa, null,
                bem == null ? null : bem.getId(), solicitante, observacao, rascunho,
                OrigemDoRegistro.MANUAL, null);
        if (natureza != null || centro != null) {
            contas.adicionarItem(nova.getId(), descricao.trim(), BigDecimal.ONE, quanto,
                    natureza == null ? null : natureza.getId(),
                    centro == null ? null : centro.getId(), pessoa,
                    bem == null ? null : bem.getId());
        }
        janela.avisar("Conta " + nova.getNumero() + " lançada.");
        janela.ir(TelaObrigacao.class, nova.getId());
    }
}
