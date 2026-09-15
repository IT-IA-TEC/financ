package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ItemDeServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemDeServicoRepositorio extends JpaRepository<ItemDeServico, UUID> {

    Optional<ItemDeServico> findByIdAndServicoId(UUID id, UUID servicoId);

    long countByServicoId(UUID servicoId);
}
