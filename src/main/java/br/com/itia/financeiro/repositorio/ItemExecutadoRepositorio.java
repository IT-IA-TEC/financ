package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ItemExecutado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemExecutadoRepositorio extends JpaRepository<ItemExecutado, UUID> {

    Optional<ItemExecutado> findByIdAndRealizadoId(UUID id, UUID realizadoId);
}
