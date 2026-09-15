package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ColunaCarteira;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Os filtros da tela de clientes.
 *
 * Cada coluna filtra do jeito do assunto dela: texto procura por pedaco,
 * situacao escolhe entre as situacoes que existem, dinheiro e numero filtram
 * por faixa, e data filtra por periodo. Nao existe filtro solto.
 *
 * Os valores chegam pela propria barra de endereco, entao um filtro montado
 * continua valendo se a pessoa sair e voltar, ou recarregar a tela.
 */
public class FiltroCarteira {

    private final Map<String, String> valores;

    public FiltroCarteira(Map<String, String> valores) {
        this.valores = valores == null ? Map.of() : valores;
    }

    public static FiltroCarteira de(Map<String, String> parametros) {
        Map<String, String> limpos = new HashMap<>();
        if (parametros != null) {
            parametros.forEach((chave, valor) -> {
                if (chave.startsWith("f_") && valor != null && !valor.isBlank()) {
                    limpos.put(chave, valor.trim());
                }
            });
        }
        return new FiltroCarteira(limpos);
    }

    public boolean vazio() {
        return valores.isEmpty();
    }

    public String valor(ColunaCarteira coluna, String sufixo) {
        return valores.getOrDefault(chave(coluna, sufixo), "");
    }

    /** Marca a caixinha da situacao que esta escolhida. */
    public boolean marcado(ColunaCarteira coluna, String situacao) {
        String escolhidas = valor(coluna, "");
        return escolhidas.isEmpty() || List.of(escolhidas.split("\\|")).contains(situacao);
    }

    public boolean temFiltro(ColunaCarteira coluna) {
        return valores.keySet().stream().anyMatch(k -> k.startsWith("f_" + coluna.name()));
    }

    // ------------------------------------------------------------- conferencia

    public boolean aceitaTexto(ColunaCarteira coluna, String conteudo) {
        String procurado = valor(coluna, "");
        if (procurado.isEmpty()) {
            return true;
        }
        return conteudo != null
                && conteudo.toLowerCase().contains(procurado.toLowerCase());
    }

    public boolean aceitaSituacao(ColunaCarteira coluna, String situacao) {
        String escolhidas = valor(coluna, "");
        if (escolhidas.isEmpty()) {
            return true;
        }
        return List.of(escolhidas.split("\\|")).contains(situacao);
    }

    public boolean aceitaFaixa(ColunaCarteira coluna, BigDecimal numero) {
        BigDecimal de = numeroDe(valor(coluna, "_de"));
        BigDecimal ate = numeroDe(valor(coluna, "_ate"));
        if (de != null && (numero == null || numero.compareTo(de) < 0)) {
            return false;
        }
        return !(ate != null && (numero == null || numero.compareTo(ate) > 0));
    }

    public boolean aceitaPeriodo(ColunaCarteira coluna, LocalDate data) {
        LocalDate de = dataDe(valor(coluna, "_de"));
        LocalDate ate = dataDe(valor(coluna, "_ate"));
        if (de == null && ate == null) {
            return true;
        }
        if (data == null) {
            return false;
        }
        if (de != null && data.isBefore(de)) {
            return false;
        }
        return !(ate != null && data.isAfter(ate));
    }

    private String chave(ColunaCarteira coluna, String sufixo) {
        return "f_" + coluna.name() + sufixo;
    }

    private BigDecimal numeroDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.replace(".", "").replace(",", "."));
        } catch (NumberFormatException naoEhNumero) {
            return null;
        }
    }

    private LocalDate dataDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (RuntimeException naoEhData) {
            return null;
        }
    }
}
