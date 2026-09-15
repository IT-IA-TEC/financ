package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.PagamentoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Os numeros do painel. Todos calculados na hora, nenhum guardado. */
@Service
public class PainelServico {

    private final TituloRepositorio titulos;
    private final PagamentoRepositorio pagamentos;
    private final ClienteRepositorio clientes;
    private final ContextoEmpresa contexto;

    public PainelServico(TituloRepositorio titulos, PagamentoRepositorio pagamentos,
                         ClienteRepositorio clientes, ContextoEmpresa contexto) {
        this.titulos = titulos;
        this.pagamentos = pagamentos;
        this.clientes = clientes;
        this.contexto = contexto;
    }

    public Resumo resumoDoMes() {
        UUID empresaId = contexto.exigirEmpresaId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioDoMes = hoje.withDayOfMonth(1);

        return new Resumo(
                titulos.somarEmAberto(empresaId),
                titulos.somarVencido(empresaId, hoje),
                pagamentos.somarRecebido(empresaId, inicioDoMes, hoje),
                titulos.countByEmpresaIdAndSituacao(empresaId, SituacaoTitulo.ABERTO)
                        + titulos.countByEmpresaIdAndSituacao(empresaId, SituacaoTitulo.PARCIAL),
                clientes.countByEmpresaIdAndAtivoTrue(empresaId));
    }

    /** O que aparece nos quadrados do topo da tela. */
    public record Resumo(BigDecimal emAberto,
                         BigDecimal vencido,
                         BigDecimal recebidoNoMes,
                         long titulosEmAberto,
                         long clientesAtivos) {
    }
}
