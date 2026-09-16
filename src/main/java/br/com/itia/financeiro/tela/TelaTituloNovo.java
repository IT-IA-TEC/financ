package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.FinanceiroServico;
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
 * Lançar título.
 *
 * O número do título e o código do PIX saem sozinhos, ninguém digita: é o que
 * garante que a baixa automática encontre o pagamento depois.
 */
@Component
public class TelaTituloNovo implements Tela {

    private final FinanceiroServico financeiro;
    private final Janela janela;

    public TelaTituloNovo(FinanceiroServico financeiro, @Lazy Janela janela) {
        this.financeiro = financeiro;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "titulos";
    }

    @Override
    public Node montar() {
        List<ClienteEspelho> clientes = financeiro.clientesDaEmpresa();
        LocalDate hoje = LocalDate.now();

        VBox tela = new VBox(16, Pecas.cabecalho("novo lançamento", "Lançar título",
                "O número do título e o código do PIX saem sozinhos. Ninguém digita."));

        if (clientes.isEmpty()) {
            tela.getChildren().add(Pecas.vazio("Esta empresa ainda não tem cliente cadastrado. "
                    + "Cadastre o primeiro cliente antes de lançar um título."));
            return tela;
        }

        ComboBox<ClienteEspelho> cliente = new ComboBox<>();
        cliente.getItems().addAll(clientes);
        cliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClienteEspelho escolhido) {
                return escolhido == null ? "" : escolhido.getRazaoSocial();
            }

            @Override
            public ClienteEspelho fromString(String texto) {
                return null;
            }
        });
        cliente.getSelectionModel().selectFirst();
        cliente.setMaxWidth(Double.MAX_VALUE);

        TextField descricao = new TextField();
        descricao.setPromptText("Honorário contábil");

        DatePicker competencia = new DatePicker(hoje.withDayOfMonth(1));
        DatePicker vencimento = new DatePicker(hoje.plusMonths(1).withDayOfMonth(20));
        competencia.setMaxWidth(Double.MAX_VALUE);
        vencimento.setMaxWidth(Double.MAX_VALUE);

        TextField valor = new TextField();
        valor.setPromptText("0,00");

        HBox linha1 = linha(
                Pecas.campo("Cliente", cliente),
                Pecas.campo("Descrição", descricao));
        HBox linha2 = linha(
                Pecas.campo("Competência", competencia),
                Pecas.campo("Vencimento", vencimento),
                Pecas.campo("Valor (R$)", valor));

        HBox acoes = new HBox(12,
                Pecas.botao("Lançar", () -> lancar(cliente.getValue(), competencia.getValue(),
                        descricao.getText(), valor.getText(), vencimento.getValue())),
                Pecas.botaoVazado("Cancelar", () -> janela.ir(TelaTitulos.class)));

        tela.getChildren().add(Pecas.caixa(linha1, linha2, acoes));
        return tela;
    }

    private HBox linha(Node... campos) {
        HBox linha = new HBox(16, campos);
        for (Node campo : campos) {
            HBox.setHgrow(campo, Priority.ALWAYS);
        }
        return linha;
    }

    private void lancar(ClienteEspelho cliente, LocalDate competencia, String descricao, String valor,
                        LocalDate vencimento) {
        if (cliente == null || descricao == null || descricao.isBlank()) {
            janela.reclamar("Escolha o cliente e escreva a descrição.");
            return;
        }
        BigDecimal quanto = dinheiro(valor);
        if (quanto == null || quanto.signum() <= 0) {
            janela.reclamar("Escreva o valor do título, maior que zero.");
            return;
        }
        Titulo titulo = financeiro.lancarTitulo(cliente.getId(), competencia, descricao.trim(),
                quanto, vencimento);
        janela.avisar("Título " + titulo.getNumero() + " lançado. Código do PIX: "
                + titulo.getIdentificadorPix());
        janela.ir(TelaTitulo.class, titulo.getId());
    }

    /** Aceita tanto 1.234,56 quanto 1234.56, que é como as pessoas digitam. */
    static BigDecimal dinheiro(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim().replace("R$", "").trim();
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException naoEhNumero) {
            return null;
        }
    }
}
