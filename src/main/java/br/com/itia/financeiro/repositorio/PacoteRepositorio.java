package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Pacote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacoteRepositorio extends JpaRepository<Pacote, UUID> {

    List<Pacote> findByEmpresaIdOrderByCodigo(UUID empresaId);

    List<Pacote> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<Pacote> findByIdAndEmpresaId(UUID id, UUID empresaId);

    @Query("select coalesce(max(p.codigo), '') from Pacote p where p.empresa.id = :empresaId")
    String ultimoCodigo(@Param("empresaId") UUID empresaId);
}
