package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ExecucaoDoFluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExecucaoDoFluxoRepositorio extends JpaRepository<ExecucaoDoFluxo, UUID> {

    List<ExecucaoDoFluxo> findTop50ByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    List<ExecucaoDoFluxo> findTop30ByFluxoIdOrderByOcorridoEmDesc(UUID fluxoId);
}
