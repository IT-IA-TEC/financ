package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartamentoRepositorio extends JpaRepository<Departamento, UUID> {

    List<Departamento> findByEmpresaIdOrderByNome(UUID empresaId);

    List<Departamento> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<Departamento> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
