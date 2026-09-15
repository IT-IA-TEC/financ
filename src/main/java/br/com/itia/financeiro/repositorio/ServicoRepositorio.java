package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepositorio extends JpaRepository<Servico, UUID> {

    List<Servico> findByEmpresaIdOrderByCodigo(UUID empresaId);

    Optional<Servico> findByIdAndEmpresaId(UUID id, UUID empresaId);

    @Query("select coalesce(max(s.codigo), '') from Servico s where s.empresa.id = :empresaId")
    String ultimoCodigo(@Param("empresaId") UUID empresaId);
}
