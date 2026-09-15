package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ColunaCarteira;
import br.com.itia.financeiro.dominio.EscolhaDeColunas;
import br.com.itia.financeiro.repositorio.EscolhaDeColunasRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Quais colunas a empresa quer ver na tela de clientes.
 *
 * A escolha e por empresa: a YOU pode querer o e-mail e a 40% o codigo da loja.
 * As colunas fixas entram sempre, mesmo que alguem tente salvar sem elas.
 */
@Service
public class ColunasServico {

    private final EscolhaDeColunasRepositorio escolhas;
    private final ContextoEmpresa contexto;

    public ColunasServico(EscolhaDeColunasRepositorio escolhas, ContextoEmpresa contexto) {
        this.escolhas = escolhas;
        this.contexto = contexto;
    }

    public List<ColunaCarteira> escolhidas() {
        UUID empresaId = contexto.exigirEmpresaId();
        return escolhas.findById(empresaId)
                .map(e -> converter(e.getColunas()))
                .filter(lista -> !lista.isEmpty())
                .orElseGet(ColunaCarteira::padrao);
    }

    @Transactional
    public void salvar(List<String> nomes) {
        UUID empresaId = contexto.exigirEmpresaId();
        List<ColunaCarteira> lista = new ArrayList<>();

        // As fixas entram primeiro, na ordem do catalogo, aconteca o que acontecer.
        for (ColunaCarteira coluna : ColunaCarteira.values()) {
            boolean pedida = nomes != null && nomes.contains(coluna.name());
            if (coluna.isFixa() || pedida) {
                lista.add(coluna);
            }
        }

        String texto = String.join(",", lista.stream().map(Enum::name).toList());
        escolhas.findById(empresaId)
                .ifPresentOrElse(
                        e -> e.trocar(texto, contexto.autor()),
                        () -> escolhas.save(new EscolhaDeColunas(empresaId, texto, contexto.autor())));
    }

    private List<ColunaCarteira> converter(String texto) {
        List<ColunaCarteira> lista = new ArrayList<>();
        for (String nome : texto.split(",")) {
            try {
                lista.add(ColunaCarteira.valueOf(nome.trim()));
            } catch (IllegalArgumentException coluna_que_nao_existe_mais) {
                // Coluna removida do sistema: ignora em vez de quebrar a tela.
            }
        }
        return lista;
    }
}
