package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import br.com.itia.financeiro.repositorio.RegraDaEmpresaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * As regras que a empresa liga ou desliga.
 *
 * Empresa que nunca mexeu recebe a regra desligada, sem precisar de cadastro
 * previo: assim nenhuma tela quebra por falta de configuracao.
 */
@Service
public class Regras {

    private final RegraDaEmpresaRepositorio regras;
    private final ContextoEmpresa contexto;

    public Regras(RegraDaEmpresaRepositorio regras, ContextoEmpresa contexto) {
        this.regras = regras;
        this.contexto = contexto;
    }

    public RegraDaEmpresa daEmpresa() {
        return regras.findById(contexto.exigirEmpresaId())
                .orElseGet(() -> new RegraDaEmpresa(contexto.exigirEmpresaId()));
    }

    @Transactional
    public void salvar(boolean pixIdentificador, boolean pixAutomatico,
                       boolean agenteSeIdentifica, boolean cobrarJuros, BigDecimal jurosAoMes,
                       BigDecimal multaPorAtraso, int carenciaDias) {
        contexto.exigirPapel(UsuarioEmpresa.Papel.DIRETOR);
        RegraDaEmpresa regra = regras.findById(contexto.exigirEmpresaId())
                .orElseGet(() -> new RegraDaEmpresa(contexto.exigirEmpresaId()));
        regra.ajustar(pixIdentificador, pixAutomatico, agenteSeIdentifica, cobrarJuros,
                jurosAoMes, multaPorAtraso, carenciaDias, contexto.autor());
        regras.save(regra);
    }
}
