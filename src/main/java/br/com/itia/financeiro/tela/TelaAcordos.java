package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Acordo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.Acordos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acordos: quando o cliente não consegue pagar tudo de uma vez.
 *
 * Fechar um acordo tira os documentos antigos da cobrança e cria as parcelas
 * como títulos de verdade. Se o acordo quebra, os originais voltam.
 */
@Component
public class TelaAcordos implements Tela {

    private final Acordos acordos;
    private final Janela janela;

    private UUID escolhida;

    public TelaAcordos(Acordos acordos, @Lazy Janela janela) {
        this.acordos = acordos;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        List<Acordo> lista = acordos.todos();
        Map<UUID, String> devedores = acordos.clientesComDivida();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("quando não dá para pagar de uma vez", "Acordos",
                "Parcelar o que está vencido. As parcelas viram títulos de verdade.",
                Pecas.botao("+ Novo acordo", () -> abrirNovoAcordo(devedores))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Acordos"));
        tela.getChildren().add(numeros(lista, devedores.size()));

        tela.getChildren().add(Pecas.secao("Acordos fechados"));
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Nº", a -> String.valueOf(a.getNumero()), 0.4)
                .coluna("Quando", a -> a.getCriadoEm() == null ? ""
                        : Pecas.data(a.getCriadoEm().toLocalDate()), 0.8)
                .coluna("Cliente", a -> devedores.getOrDefault(a.getUnidadeId(), ""), 2)
                .valor("Devia", a -> Pecas.numero(a.getValorOriginal()))
                .valor("Acréscimo", a -> Pecas.numero(a.getAcrescimo()))
                .valor("Desconto", a -> Pecas.numero(a.getDesconto()))
                .valor("Combinado", a -> Pecas.numero(a.getValorCombinado()))
                .coluna("Parcelas", a -> a.getParcelas() + "x", 0.6)
                .coluna("1º vencimento", a -> Pecas.data(a.getPrimeiroVencimento()))
                .comMarca(Acordo::getSituacaoLegivel,
                        a -> a.estaAtivo() ? "" : "s-cancelado")
                .aoClicar(a -> janela.ir(TelaAcordo.class, a.getId()))
                .quandoVazia("Nenhum acordo fechado ainda.")
                .montar());
        return tela;
    }

    /** Os quatro números do topo: ativos, cumpridos, quebrados e quem pode negociar. */
    private HBox numeros(List<Acordo> lista, int podemNegociar) {
        long ativos = lista.stream().filter(a -> "ATIVO".equals(a.getSituacao())).count();
        long cumpridos = lista.stream().filter(a -> "CUMPRIDO".equals(a.getSituacao())).count();
        long quebrados = lista.stream().filter(a -> "QUEBRADO".equals(a.getSituacao())).count();
        return Pecas.quadros(
                Pecas.quadro("Ativos", String.valueOf(ativos), "em andamento", true, false),
                Pecas.quadro("Cumpridos", String.valueOf(cumpridos), "pagos até o fim"),
                Pecas.quadro("Quebrados", String.valueOf(quebrados), "voltaram para a cobrança",
                        false, quebrados > 0),
                Pecas.quadro("Podem negociar", String.valueOf(podemNegociar),
                        "clientes com dívida"));
    }

    /**
     * O acordo abre por cima da tela: escolhe o cliente, marca os documentos
     * que entram e fecha. Nada disso ocupa espaço na página.
     */
    private void abrirNovoAcordo(Map<UUID, String> devedores) {
        if (devedores.isEmpty()) {
            janela.reclamar("Nenhum cliente com dívida em aberto para acordo.");
            return;
        }

        ComboBox<Map.Entry<UUID, String>> cliente = new ComboBox<>();
        cliente.getItems().addAll(new LinkedHashMap<>(devedores).entrySet());
        cliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Map.Entry<UUID, String> qual) {
                return qual == null ? "" : qual.getValue();
            }

            @Override
            public Map.Entry<UUID, String> fromString(String texto) {
                return null;
            }
        });
        if (escolhida != null) {
            cliente.getItems().stream()
                    .filter(item -> item.getKey().equals(escolhida))
                    .findFirst()
                    .ifPresent(item -> cliente.getSelectionModel().select(item));
        } else {
            cliente.getSelectionModel().selectFirst();
        }
        cliente.setMaxWidth(Double.MAX_VALUE);
        VBox escolhas = new VBox(6);
        List<CheckBox> marcados = new ArrayList<>();
        cliente.setOnAction(acao -> {
            if (cliente.getValue() != null) {
                escolhida = cliente.getValue().getKey();
                listarDocumentos(escolhida, escolhas, marcados);
            }
        });
        UUID[] unidade = { cliente.getValue() == null ? null : cliente.getValue().getKey() };
        listarDocumentos(unidade[0], escolhas, marcados);

        TextField acrescimo = new TextField("0,00");
        TextField desconto = new TextField("0,00");
        TextField entrada = new TextField("0,00");
        TextField parcelas = new TextField("3");
        DatePicker primeiro = new DatePicker(LocalDate.now().plusDays(10));
        primeiro.setMaxWidth(Double.MAX_VALUE);
        TextField motivo = new TextField();
        motivo.setPromptText("por que houve desconto");
        TextField observacao = new TextField();
        observacao.setPromptText("o que mais ficou combinado");

        HBox linha1 = new HBox(16, Pecas.campo("Cliente", cliente));
        HBox linha2 = new HBox(16,
                Pecas.campo("Juros e multa", acrescimo),
                Pecas.campo("Desconto (R$)", desconto),
                Pecas.campo("Entrada (R$)", entrada),
                Pecas.campo("Parcelas", parcelas),
                Pecas.campo("Primeira vence em", primeiro));
        HBox linha3 = new HBox(16, Pecas.campo("Motivo do desconto", motivo),
                Pecas.campo("Observação", observacao));
        linha1.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha2.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        linha3.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        JanelaFlutuante.nova(janela.palco(), "Com quem é o acordo",
                        "Escolha o cliente, marque o que entra e feche.")
                .com(linha1, Pecas.secao("Documentos que entram"), escolhas, linha2, linha3)
                .acao("Fechar o acordo", () -> {
                    UUID qual = cliente.getValue() == null ? unidade[0]
                            : cliente.getValue().getKey();
                    List<UUID> ids = marcados.stream()
                            .filter(CheckBox::isSelected)
                            .map(marca -> (UUID) marca.getUserData())
                            .toList();
                    if (qual == null || ids.isEmpty()) {
                        janela.reclamar("Escolha o cliente e pelo menos um documento.");
                        return false;
                    }
                    Acordo acordo = acordos.fechar(qual, ids, valor(acrescimo.getText()),
                            valor(desconto.getText()), valor(entrada.getText()),
                            inteiro(parcelas.getText()), primeiro.getValue(),
                            motivo.getText() == null || motivo.getText().isBlank() ? null
                                    : motivo.getText().trim(), observacao.getText());
                    janela.avisar("Acordo " + acordo.getNumero() + " fechado. Os documentos "
                            + "antigos saíram da cobrança e as parcelas já estão no contas a "
                            + "receber.");
                    janela.ir(TelaAcordo.class, acordo.getId());
                    return true;
                })
                .abrir();
    }

    /** Os documentos daquele cliente que podem entrar no acordo. */
    private void listarDocumentos(UUID unidade, VBox escolhas, List<CheckBox> marcados) {
        escolhas.getChildren().clear();
        marcados.clear();
        List<Titulo> podem = unidade == null ? List.of() : acordos.podeEntrar(unidade);
        for (Titulo titulo : podem) {
            long atraso = java.time.temporal.ChronoUnit.DAYS.between(titulo.getVencimento(),
                    LocalDate.now());
            CheckBox marca = new CheckBox(titulo.getNumero() + " · " + titulo.getDescricao()
                    + " · vence " + Pecas.data(titulo.getVencimento())
                    + (atraso > 0 ? " · " + atraso + " dias de atraso" : " · em dia")
                    + " · " + Pecas.dinheiro(titulo.getSaldo()));
            marca.setSelected(true);
            marca.setUserData(titulo.getId());
            marcados.add(marca);
            escolhas.getChildren().add(marca);
        }
        if (podem.isEmpty()) {
            Label nada = new Label("Este cliente não tem documento em aberto para acordo.");
            nada.getStyleClass().add("dica");
            escolhas.getChildren().add(nada);
        }
    }

    private BigDecimal valor(String texto) {
        BigDecimal valor = TelaTituloNovo.dinheiro(texto);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private int inteiro(String texto) {
        try {
            return Math.max(1, Integer.parseInt(texto.trim()));
        } catch (RuntimeException naoEhNumero) {
            return 1;
        }
    }
}
