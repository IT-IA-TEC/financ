package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Obrigacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Os filtros das colunas do contas a pagar.
 *
 * Cada coluna filtra pelo que ela mostra, no formato dela: data tem de e até,
 * dinheiro tem mínimo e máximo, situação tem caixas de marcar. Filtro genérico
 * em cima de tudo não ajuda ninguém a achar uma conta.
 *
 * O que vem do endereço é sempre texto. Valor que não dá para entender é
 * ignorado, e não derruba a tela.
 */
public class FiltroDeContas {

    private final Map<String, String> campos = new LinkedHashMap<>();
    private final List<String> situacoes = new ArrayList<>();

    public FiltroDeContas(Map<String, String> parametros, List<String> situacoesMarcadas) {
        if (parametros != null) {
            parametros.forEach((chave, valor) -> {
                if (chave.startsWith("f_") && valor != null && !valor.isBlank()) {
                    campos.put(chave, valor.trim());
                }
            });
        }
        if (situacoesMarcadas != null) {
            situacoes.addAll(situacoesMarcadas);
        }
    }

    public boolean vazio() {
        return campos.isEmpty() && situacoes.isEmpty();
    }

    public String valor(String campo) {
        return campos.getOrDefault(campo, "");
    }

    public boolean marcado(String situacao) {
        return situacoes.contains(situacao);
    }

    /** Se aquela coluna está filtrando agora, para a tela marcar o cabeçalho. */
    public boolean filtrando(String coluna) {
        if ("situacao".equals(coluna)) {
            return !situacoes.isEmpty();
        }
        return campos.keySet().stream().anyMatch(chave -> chave.startsWith("f_" + coluna));
    }

    /** Quantos filtros estão ligados, para o aviso de "limpar filtros". */
    public int quantos() {
        return campos.size() + (situacoes.isEmpty() ? 0 : 1);
    }

    public boolean aceita(Obrigacao o, LocalDate hoje) {
        return combinaTexto(o) && combinaListas(o) && combinaDatas(o) && combinaValores(o)
                && combinaSituacao(o, hoje) && combinaPendencia(o);
    }

    private boolean combinaTexto(Obrigacao o) {
        String descricao = texto("f_descricao");
        String numero = texto("f_numero");
        boolean okDescricao = descricao.isEmpty()
                || o.getDescricao().toLowerCase().contains(descricao);
        boolean okNumero = numero.isEmpty()
                || String.valueOf(o.getNumero()).contains(numero);
        return okDescricao && okNumero;
    }

    private boolean combinaListas(Obrigacao o) {
        String favorecido = valor("f_favorecido");
        String natureza = valor("f_natureza");
        String centro = valor("f_centro");

        boolean okFavorecido = favorecido.isEmpty()
                || o.getFavorecido().getId().toString().equals(favorecido);
        boolean okNatureza = natureza.isEmpty() || o.getItens().stream()
                .anyMatch(i -> i.getNatureza() != null
                        && i.getNatureza().getId().toString().equals(natureza));
        boolean okCentro = centro.isEmpty() || o.getItens().stream()
                .anyMatch(i -> i.getCentroDeCusto() != null
                        && i.getCentroDeCusto().getId().toString().equals(centro));
        return okFavorecido && okNatureza && okCentro;
    }

    private boolean combinaDatas(Obrigacao o) {
        LocalDate de = data("f_vencimento_de");
        LocalDate ate = data("f_vencimento_ate");
        boolean okDe = de == null || !o.getVencimento().isBefore(de);
        boolean okAte = ate == null || !o.getVencimento().isAfter(ate);
        return okDe && okAte;
    }

    private boolean combinaValores(Obrigacao o) {
        return dentro(o.getValor(), "f_valor_de", "f_valor_ate")
                && dentro(o.getTotalPago(), "f_pago_de", "f_pago_ate")
                && dentro(o.getSaldo(), "f_saldo_de", "f_saldo_ate");
    }

    private boolean combinaSituacao(Obrigacao o, LocalDate hoje) {
        if (situacoes.isEmpty()) {
            return true;
        }
        String atual = o.isCancelada() ? "cancelada"
                : o.estaVencida(hoje) ? "vencida"
                : o.getLiquidacao().name().toLowerCase();
        return situacoes.contains(atual);
    }

    private boolean combinaPendencia(Obrigacao o) {
        String escolha = valor("f_pendencias");
        if (escolha.isEmpty()) {
            return true;
        }
        boolean tem = !o.pendencias().isEmpty();
        return "com".equals(escolha) == tem;
    }

    // ------------------------------------------------------------------ apoio

    private String texto(String campo) {
        return valor(campo).toLowerCase();
    }

    private LocalDate data(String campo) {
        String bruto = valor(campo);
        if (bruto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(bruto);
        } catch (RuntimeException dataIlegivel) {
            return null;
        }
    }

    private BigDecimal numero(String campo) {
        String bruto = valor(campo).replace(".", "").replace(",", ".");
        if (bruto.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(bruto);
        } catch (NumberFormatException valorIlegivel) {
            return null;
        }
    }

    private boolean dentro(BigDecimal valor, String campoDe, String campoAte) {
        BigDecimal de = numero(campoDe);
        BigDecimal ate = numero(campoAte);
        BigDecimal alvo = valor == null ? BigDecimal.ZERO : valor;
        return (de == null || alvo.compareTo(de) >= 0)
                && (ate == null || alvo.compareTo(ate) <= 0);
    }
}
