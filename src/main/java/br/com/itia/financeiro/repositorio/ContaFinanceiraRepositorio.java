package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ContaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaFinanceiraRepositorio extends JpaRepository<ContaFinanceira, UUID> {

    List<ContaFinanceira> findByEmpresaIdOrderByNome(UUID empresaId);

    List<ContaFinanceira> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<ContaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
