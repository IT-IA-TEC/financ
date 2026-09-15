package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Natureza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NaturezaRepositorio extends JpaRepository<Natureza, UUID> {

    List<Natureza> findByEmpresaIdOrderByCodigo(UUID empresaId);

    List<Natureza> findByEmpresaIdAndAtivoTrueOrderByCodigo(UUID empresaId);

    Optional<Natureza> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Natureza> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    long countByEmpresaId(UUID empresaId);
}
