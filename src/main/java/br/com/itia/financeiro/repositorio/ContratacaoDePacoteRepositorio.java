package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ContratacaoDePacote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratacaoDePacoteRepositorio extends JpaRepository<ContratacaoDePacote, UUID> {

    List<ContratacaoDePacote> findByPacoteIdOrderByCriadoEmDesc(UUID pacoteId);

    List<ContratacaoDePacote> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    List<ContratacaoDePacote> findByPagadorIdOrderByCriadoEmDesc(UUID pagadorId);

    Optional<ContratacaoDePacote> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByPacoteId(UUID pacoteId);
}
