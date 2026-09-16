package br.com.itia.financeiro.web;

import br.com.itia.financeiro.dominio.Empresa;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import br.com.itia.financeiro.servico.FinanceiroServico;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * O que toda tela precisa ter a mao: quem entrou e a lista de empresas que a
 * setinha do topo mostra.
 */
@ControllerAdvice
public class DadosComuns {

    private final ContextoEmpresa contexto;
    private final FinanceiroServico financeiro;

    public DadosComuns(ContextoEmpresa contexto, FinanceiroServico financeiro) {
        this.contexto = contexto;
        this.financeiro = financeiro;
    }

    @ModelAttribute("usuario")
    public ContextoEmpresa usuario() {
        return contexto;
    }

    @ModelAttribute("empresasDoTopo")
    public List<Empresa> empresasDoTopo() {
        if (!contexto.estaLogado()) {
            return List.of();
        }
        return financeiro.empresasAtivas();
    }
}
