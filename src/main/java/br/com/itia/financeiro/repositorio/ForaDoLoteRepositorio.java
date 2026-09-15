package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ForaDoLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ForaDoLoteRepositorio extends JpaRepository<ForaDoLote, UUID> {

    List<ForaDoLote> findByLoteIdOrderByQuem(UUID loteId);

    void deleteByLoteId(UUID loteId);
}
