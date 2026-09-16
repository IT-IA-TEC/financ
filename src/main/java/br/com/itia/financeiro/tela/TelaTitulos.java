package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.servico.FinanceiroServico;
import br.com.itia.financeiro.servico.PainelServico;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Contas a receber: tudo o que foi lançado nesta empresa.
 *
 * Os números do topo podem ser conferidos linha a linha na lista de baixo, de
 * propósito: nenhum deles é digitado, todos saem do que está lançado.
 */
@Component
public class TelaTitulos implements Tela {

    private static final DateTimeFormatter COMPETENCIA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final FinanceiroServico financeiro;
    private final PainelServico painel;
    private final Janela janela;

    public TelaTitulos(FinanceiroServico financeiro, PainelServico painel, @Lazy Janela janela) {
        this.financeiro = financeiro;
        this.painel = painel;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "titulos";
    }

    @Override
    public Node montar() {
        LocalDate hoje = LocalDate.now();
        List<Titulo> titulos = financeiro.titulosDaEmpresa();
        PainelServico.Resumo resumo = painel.resumoDoMes();

        // o que vence nos próximos sete dias sai da própria lista, para o número
        // do quadro poder ser conferido linha a linha
        BigDecimal aVencer = titulos.stream()
                .filter(t -> t.getSaldo().signum() > 0)
                .filter(t -> t.getVencimento() != null)
                .filter(t -> !t.getVencimento().isBefore(hoje)
                        && !t.getVencimento().isAfter(hoje.plusDays(7)))
                .map(Titulo::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("recebíveis", "Contas a receber", null,
                Pecas.botao("Lançar título", () -> janela.ir(TelaTituloNovo.class)),
                Pecas.quadrosDoTopo(
                Pecas.quadro("A receber", Pecas.dinheiro(resumo.emAberto()),
                        "saldo em aberto", true, false),
                Pecas.quadro("Vencido", Pecas.dinheiro(resumo.vencido()),
                        "passou do vencimento", false, resumo.vencido().signum() > 0),
                Pecas.quadro("Vence em 7 dias", Pecas.dinheiro(aVencer), "próxima semana"),
                Pecas.quadro("Recebido no mês", Pecas.dinheiro(resumo.recebidoNoMes()),
                        resumo.titulosEmAberto() + " em aberto"))));
        tela.getChildren().add(abas());


        tela.getChildren().add(Pecas.secao("Lançamentos do contas a receber"));
        tela.getChildren().add(Tabela.de(titulos)
                .coluna("Nº", t -> String.valueOf(t.getNumero()), 0.4)
                .coluna("Cliente", t -> t.getCliente().getRazaoSocial(), 2)
                .coluna("Competência", t -> t.getCompetencia().format(COMPETENCIA), 0.7)
                .coluna("Descrição", Titulo::getDescricao, 1.8)
                .coluna("Vencimento", t -> Pecas.data(t.getVencimento()), 0.8)
                .valor("Valor", t -> Pecas.numero(t.getValor()))
                .valor("Pago", t -> Pecas.numero(t.getTotalPago()))
                .valor("Saldo", t -> Pecas.numero(t.getSaldo()))
                .coluna("Código PIX", Titulo::getIdentificadorPix, 1.2)
                .comMarca(t -> t.estaVencido(hoje)
                                ? t.diasDeAtraso(hoje) + " dias"
                                : t.getSituacao().name().toLowerCase(),
                        t -> t.estaVencido(hoje) ? "s-vencido"
                                : "s-" + t.getSituacao().name().toLowerCase())
                .aoClicar(t -> janela.ir(TelaTitulo.class, t.getId()))
                .quandoVazia("Nenhum título lançado nesta empresa ainda.")
                .montar());
        return tela;
    }

    /** As três partes do módulo, como sempre foram. */
    private Node abas() {
        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Contas a receber", () -> janela.ir(TelaTitulos.class));
        partes.put("Comprovantes", () -> janela.ir(TelaComprovantes.class));
        partes.put("Análises", () -> janela.ir(TelaAnalises.class));
        return Pecas.abas("Contas a receber", partes);
    }
}
