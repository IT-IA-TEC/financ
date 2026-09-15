package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.AtendimentoDoAgente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AtendimentoDoAgenteRepositorio
        extends JpaRepository<AtendimentoDoAgente, UUID> {

    List<AtendimentoDoAgente> findTop50ByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    long countByEmpresaIdAndEscaladoTrue(UUID empresaId);

    long countByEmpresaId(UUID empresaId);
}
