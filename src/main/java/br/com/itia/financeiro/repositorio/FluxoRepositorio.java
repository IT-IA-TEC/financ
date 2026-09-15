package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Fluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FluxoRepositorio extends JpaRepository<Fluxo, UUID> {

    List<Fluxo> findByEmpresaIdOrderByNome(UUID empresaId);

    List<Fluxo> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<Fluxo> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
