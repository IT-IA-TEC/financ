package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Restricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestricaoRepositorio extends JpaRepository<Restricao, UUID> {

    List<Restricao> findByPagadorIdOrderByDataDesc(UUID pagadorId);

    Optional<Restricao> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
