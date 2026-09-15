package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.PassoDoFluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassoDoFluxoRepositorio extends JpaRepository<PassoDoFluxo, UUID> {

    List<PassoDoFluxo> findByFluxoIdOrderByOrdem(UUID fluxoId);

    Optional<PassoDoFluxo> findByIdAndFluxoId(UUID id, UUID fluxoId);
}
