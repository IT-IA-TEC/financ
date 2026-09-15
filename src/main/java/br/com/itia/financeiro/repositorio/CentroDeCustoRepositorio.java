package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.CentroDeCusto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CentroDeCustoRepositorio extends JpaRepository<CentroDeCusto, UUID> {

    List<CentroDeCusto> findByEmpresaIdOrderByNome(UUID empresaId);

    List<CentroDeCusto> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<CentroDeCusto> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByEmpresaId(UUID empresaId);
}
