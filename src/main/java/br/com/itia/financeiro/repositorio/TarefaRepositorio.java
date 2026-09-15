package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TarefaRepositorio extends JpaRepository<Tarefa, UUID> {

    List<Tarefa> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    Optional<Tarefa> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Tarefa> findByEmpresaIdAndIdExterno(UUID empresaId, String idExterno);

    List<Tarefa> findByPaiIdOrderByCriadoEm(UUID paiId);

    /** O proximo numero desta empresa. Cada empresa tem a sua contagem. */
    @Query("""
            select coalesce(max(t.numero), 0) + 1 from Tarefa t
            where t.empresaId = :empresaId
            """)
    Integer proximoNumero(@Param("empresaId") UUID empresaId);

    long countByEmpresaIdAndSituacao(UUID empresaId, String situacao);
}
