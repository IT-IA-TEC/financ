package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ItemDoPacote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemDoPacoteRepositorio extends JpaRepository<ItemDoPacote, UUID> {

    Optional<ItemDoPacote> findByIdAndPacoteId(UUID id, UUID pacoteId);

    long countByPacoteId(UUID pacoteId);
}
