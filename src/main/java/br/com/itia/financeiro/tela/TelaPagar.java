package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.CentroDeCusto;
import br.com.itia.financeiro.dominio.Favorecido;
import br.com.itia.financeiro.dominio.Natureza;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.servico.ContasAPagar;
import br.com.itia.financeiro.servico.FiltroDeContas;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contas a pagar: o que a empresa deve.
 *
 * As visões são recortes do mesmo conjunto, não listas diferentes: trocar de
 * visão nunca muda o valor de uma conta, só o que aparece.
 */
@Component
public class TelaPagar implements Tela {

    /** Os recortes, na mesma ordem de sempre. */
    private static final Map<String, String> VISOES = new LinkedHashMap<>();

    static {
        VISOES.put("todas", "Todas");
        VISOES.put("proximos7", "Hoje e 7 dias");
        VISOES.put("vencidas", "Vencidas");
        VISOES.put("aprovacao", "Aguardando aprovação");
        VISOES.put("prontas", "Prontas");
        VISOES.put("encaminhadas", "Encaminhadas");
        VISOES.put("reembolsos", "Reembolsos");
        VISOES.put("parciais", "Parciais");
        VISOES.put("incompletas", "Incompletos");
        VISOES.put("naoconciliadas", "Sem conciliar");
    }

    private final ContasAPagar contas;
    private final Janela janela;

    private String visao = "todas";

    /** O que cada funil de coluna está peneirando agora. */
    private final Map<String, String> filtros = new LinkedHashMap<>();
    private final List<String> situacoesEscolhidas = new ArrayList<>();

    public TelaPagar(ContasAPagar contas, @Lazy Janela janela) {
        this.contas = contas;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "pagar";
    }

    private JanelasDoPagar janelas() {
        return new JanelasDoPagar(contas, janela);
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        FiltroDeContas filtro = new FiltroDeContas(filtros, situacoesEscolhidas);
        List<Obrigacao> lista = contas.lista(visao, filtro);
        ContasAPagar.Resumo resumo = contas.resumo(lista);
        Map<String, Integer> contagem = contas.contagemDasVisoes();

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("obrigações", "Contas a pagar",
                "O que a empresa deve, com quem aprovou e quem pagou.",
                Pecas.botaoVazado("Cadastros", () -> janelas().cadastros()),
                Pecas.botaoVazado("Parcelar", () -> janelas().parcelar()),
                Pecas.botaoVazado("Recorrências", () -> janelas().recorrencias()),
                Pecas.botao("Lançar conta", () -> janela.ir(TelaObrigacaoNova.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("Em aberto", Pecas.dinheiro(resumo.aberto()),
                        "o que falta pagar", true, false),
                Pecas.quadro("Vencido", Pecas.dinheiro(resumo.vencido()),
                        "passou do vencimento", false, resumo.vencido().signum() > 0),
                Pecas.quadro("Vence em 7 dias", Pecas.dinheiro(resumo.aVencer7()),
                        "próxima semana"),
                Pecas.quadro("Qualidade", resumo.pendentes() + " com pendência",
                        Pecas.dinheiro(resumo.semClassificacao()) + " sem classificação",
                        false, resumo.pendentes() > 0))));

        LinkedHashMap<String, Runnable> abas = new LinkedHashMap<>();
        for (Map.Entry<String, String> parte : VISOES.entrySet()) {
            Integer quantas = contagem.get(parte.getKey());
            String rotulo = parte.getValue() + (quantas == null ? "" : " (" + quantas + ")");
            abas.put(rotulo, () -> {
                visao = parte.getKey();
                janela.atualizar();
            });
        }
        String atual = VISOES.get(visao)
                + (contagem.get(visao) == null ? "" : " (" + contagem.get(visao) + ")");
        tela.getChildren().add(Pecas.abas(atual, abas));


        List<ContasAPagar.LinhaDeCusto> custos = contas.custoPorCentro(lista);
        if (!custos.isEmpty()) {
            tela.getChildren().add(Pecas.secao("Custo por área, nesta lista"));
            tela.getChildren().add(Tabela.de(custos)
                    .coluna("Centro de custo", ContasAPagar.LinhaDeCusto::area, 2)
                    .valor("Custo direto", c -> Pecas.numero(c.direto()))
                    .valor("Custo rateado", c -> Pecas.numero(c.rateado()))
                    .valor("Total", c -> Pecas.numero(c.total()))
                    .montar());
        }

        tela.getChildren().add(Pecas.secao("Contas"));
        if (!filtro.vazio()) {
            HBox aplicado = new HBox(12,
                    new javafx.scene.control.Label("Filtro aplicado nesta tabela: "
                            + filtro.quantos() + " coluna(s)."),
                    Pecas.botaoVazado("Limpar filtros", () -> {
                        filtros.clear();
                        situacoesEscolhidas.clear();
                        janela.atualizar();
                    }));
            aplicado.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            aplicado.getChildren().get(0).getStyleClass().add("dica");
            tela.getChildren().add(aplicado);
        }
        tela.getChildren().add(Tabela.de(lista)
                .coluna("Vencimento", o -> Pecas.data(o.getVencimento()), 0.9)
                .coluna("Nº", o -> String.valueOf(o.getNumero()), 0.4)
                .coluna("Favorecido", o -> o.getFavorecido() == null ? ""
                        : o.getFavorecido().getNome(), 1.8)
                .coluna("Descrição", Obrigacao::getDescricao, 1.8)
                .coluna("Natureza", o -> o.getItens().stream()
                        .filter(i -> i.getNatureza() != null)
                        .map(i -> i.getNatureza().getNome()).findFirst().orElse(""), 1.2)
                .coluna("Centro de custo", o -> o.getItens().stream()
                        .filter(i -> i.getCentroDeCusto() != null)
                        .map(i -> i.getCentroDeCusto().getNome()).findFirst().orElse(""), 1.2)
                .valor("Valor devido", o -> Pecas.numero(o.getValor()))
                .valor("Pago", o -> Pecas.numero(o.getTotalPago()))
                .valor("Saldo", o -> Pecas.numero(o.getSaldo()))
                .coluna("Pendências", o -> String.join(", ", o.pendencias()), 1.2)
                .funil("Vencimento", filtro.filtrando("vencimento"), this::filtrarVencimento)
                .funil("Nº", filtro.filtrando("numero"), this::filtrarNumero)
                .funil("Favorecido", filtro.filtrando("favorecido"), this::filtrarFavorecido)
                .funil("Descrição", filtro.filtrando("descricao"), this::filtrarDescricao)
                .funil("Natureza", filtro.filtrando("natureza"), this::filtrarNatureza)
                .funil("Centro de custo", filtro.filtrando("centro"), this::filtrarCentro)
                .funil("Valor devido", filtro.filtrando("valor"),
                        () -> filtrarFaixa("Valor devido", "f_valor_de", "f_valor_ate"))
                .funil("Pago", filtro.filtrando("pago"),
                        () -> filtrarFaixa("Pago", "f_pago_de", "f_pago_ate"))
                .funil("Saldo", filtro.filtrando("saldo"),
                        () -> filtrarFaixa("Saldo", "f_saldo_de", "f_saldo_ate"))
                .funil("Pendências", filtro.filtrando("pendencias"), this::filtrarPendencias)
                .funil("Situação", filtro.filtrando("situacao"), this::filtrarSituacao)
                .comMarca(o -> situacao(o, hoje), o -> cor(o, hoje))
                .aoClicar(o -> janela.ir(TelaObrigacao.class, o.getId()))
                .quandoVazia("Nenhuma conta neste recorte.")
                .montar());
        return tela;
    }


    // ------------------------------------------------------- funis de coluna

    private void filtrarNumero() {
        TextField numero = new TextField(filtros.getOrDefault("f_numero", ""));
        numero.setPromptText("número da conta");
        aplicar("Filtrar por nº", Pecas.campo("Número da conta", numero),
                () -> guardar("f_numero", numero.getText()));
    }

    private void filtrarDescricao() {
        TextField descricao = new TextField(filtros.getOrDefault("f_descricao", ""));
        descricao.setPromptText("parte do texto");
        aplicar("Filtrar por descrição", Pecas.campo("Contém", descricao),
                () -> guardar("f_descricao", descricao.getText()));
    }

    private void filtrarFavorecido() {
        ComboBox<Favorecido> quem = new ComboBox<>();
        quem.getItems().add(null);
        quem.getItems().addAll(contas.favorecidosAtivos());
        quem.setConverter(new StringConverter<>() {
            @Override
            public String toString(Favorecido qual) {
                return qual == null ? "todos" : qual.getNome();
            }

            @Override
            public Favorecido fromString(String texto) {
                return null;
            }
        });
        quem.getSelectionModel().select(contas.favorecidosAtivos().stream()
                .filter(f -> f.getId().toString().equals(filtros.get("f_favorecido")))
                .findFirst().orElse(null));
        quem.setMaxWidth(Double.MAX_VALUE);
        aplicar("Filtrar por favorecido", Pecas.campo("Quem recebe", quem),
                () -> guardar("f_favorecido", quem.getValue() == null ? ""
                        : quem.getValue().getId().toString()));
    }

    private void filtrarNatureza() {
        ComboBox<Natureza> qual = new ComboBox<>();
        qual.getItems().add(null);
        qual.getItems().addAll(contas.naturezasAtivas());
        qual.setConverter(new StringConverter<>() {
            @Override
            public String toString(Natureza natureza) {
                return natureza == null ? "todas" : natureza.getNome();
            }

            @Override
            public Natureza fromString(String texto) {
                return null;
            }
        });
        qual.getSelectionModel().select(contas.naturezasAtivas().stream()
                .filter(n -> n.getId().toString().equals(filtros.get("f_natureza")))
                .findFirst().orElse(null));
        qual.setMaxWidth(Double.MAX_VALUE);
        aplicar("Filtrar por natureza", Pecas.campo("O que está sendo pago", qual),
                () -> guardar("f_natureza", qual.getValue() == null ? ""
                        : qual.getValue().getId().toString()));
    }

    private void filtrarCentro() {
        ComboBox<CentroDeCusto> qual = new ComboBox<>();
        qual.getItems().add(null);
        qual.getItems().addAll(contas.centrosAtivos());
        qual.setConverter(new StringConverter<>() {
            @Override
            public String toString(CentroDeCusto centro) {
                return centro == null ? "todos" : centro.getNome();
            }

            @Override
            public CentroDeCusto fromString(String texto) {
                return null;
            }
        });
        qual.getSelectionModel().select(contas.centrosAtivos().stream()
                .filter(c -> c.getId().toString().equals(filtros.get("f_centro")))
                .findFirst().orElse(null));
        qual.setMaxWidth(Double.MAX_VALUE);
        aplicar("Filtrar por centro de custo", Pecas.campo("Área que absorve", qual),
                () -> guardar("f_centro", qual.getValue() == null ? ""
                        : qual.getValue().getId().toString()));
    }

    private void filtrarVencimento() {
        DatePicker de = new DatePicker(data("f_vencimento_de"));
        DatePicker ate = new DatePicker(data("f_vencimento_ate"));
        HBox campos = new HBox(16, Pecas.campo("De", de), Pecas.campo("Até", ate));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        aplicar("Filtrar por vencimento", campos, () -> {
            guardar("f_vencimento_de", de.getValue() == null ? "" : de.getValue().toString());
            guardar("f_vencimento_ate", ate.getValue() == null ? "" : ate.getValue().toString());
        });
    }

    private void filtrarFaixa(String oQue, String campoDe, String campoAte) {
        TextField de = new TextField(filtros.getOrDefault(campoDe, ""));
        TextField ate = new TextField(filtros.getOrDefault(campoAte, ""));
        HBox campos = new HBox(16, Pecas.campo("De", de), Pecas.campo("Até", ate));
        campos.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        aplicar("Filtrar por " + oQue.toLowerCase(), campos, () -> {
            guardar(campoDe, de.getText());
            guardar(campoAte, ate.getText());
        });
    }

    private void filtrarPendencias() {
        ComboBox<String> escolha = new ComboBox<>();
        escolha.getItems().addAll("", "com", "sem");
        escolha.setConverter(new StringConverter<>() {
            @Override
            public String toString(String qual) {
                if (qual == null || qual.isBlank()) {
                    return "todas as contas";
                }
                return "com".equals(qual) ? "só com pendência" : "só sem pendência";
            }

            @Override
            public String fromString(String texto) {
                return null;
            }
        });
        escolha.getSelectionModel().select(filtros.getOrDefault("f_pendencias", ""));
        escolha.setMaxWidth(Double.MAX_VALUE);
        aplicar("Filtrar por pendências", Pecas.campo("Mostrar", escolha),
                () -> guardar("f_pendencias", escolha.getValue()));
    }

    private void filtrarSituacao() {
        List<CheckBox> marcas = new ArrayList<>();
        VBox lista = new VBox(8);
        for (String situacao : List.of("em aberto", "parcial", "liquidada", "vencida",
                "cancelada")) {
            CheckBox marca = new CheckBox(situacao);
            marca.setSelected(situacoesEscolhidas.contains(situacao));
            marcas.add(marca);
            lista.getChildren().add(marca);
        }
        aplicar("Filtrar por situação", lista, () -> {
            situacoesEscolhidas.clear();
            for (CheckBox marca : marcas) {
                if (marca.isSelected()) {
                    situacoesEscolhidas.add(marca.getText());
                }
            }
        });
    }

    /** A janelinha do funil: mostra os campos daquela coluna e aplica. */
    private void aplicar(String titulo, Node campos, Runnable oQueGuardar) {
        JanelaFlutuante.estreita(janela.palco(), titulo, null)
                .com(campos)
                .outraAcao("Limpar esta coluna", () -> {
                    oQueGuardar.run();
                })
                .acao("Aplicar", () -> {
                    oQueGuardar.run();
                    janela.atualizar();
                    return true;
                })
                .abrir();
    }

    private void guardar(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            filtros.remove(campo);
        } else {
            filtros.put(campo, valor.trim());
        }
    }

    private LocalDate data(String campo) {
        String bruto = filtros.get(campo);
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(bruto);
        } catch (RuntimeException naoEhData) {
            return null;
        }
    }

    private String situacao(Obrigacao o, LocalDate hoje) {
        if (o.isCancelada()) {
            return "cancelada";
        }
        if (o.getSaldo().signum() == 0) {
            return "paga";
        }
        return o.estaVencida(hoje) ? o.diasDeAtraso(hoje) + " dias" : "em aberto";
    }

    private String cor(Obrigacao o, LocalDate hoje) {
        if (o.isCancelada()) {
            return "s-cancelado";
        }
        if (o.getSaldo().signum() == 0) {
            return "s-pago";
        }
        return o.estaVencida(hoje) ? "s-vencido" : "";
    }
}
