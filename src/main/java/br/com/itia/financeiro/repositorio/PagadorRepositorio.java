package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Pagador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagadorRepositorio extends JpaRepository<Pagador, UUID> {

    List<Pagador> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<Pagador> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByEmpresaIdAndAtivoTrue(UUID empresaId);
}
