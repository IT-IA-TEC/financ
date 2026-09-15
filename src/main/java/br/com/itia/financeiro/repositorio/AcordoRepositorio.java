package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Acordo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcordoRepositorio extends JpaRepository<Acordo, UUID> {

    List<Acordo> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    List<Acordo> findByEmpresaIdAndUnidadeIdOrderByCriadoEmDesc(UUID empresaId, UUID unidadeId);

    Optional<Acordo> findByIdAndEmpresaId(UUID id, UUID empresaId);

    /** O proximo numero de acordo desta empresa. Cada empresa tem a sua contagem. */
    @Query("""
            select coalesce(max(a.numero), 0) + 1 from Acordo a
            where a.empresaId = :empresaId
            """)
    Integer proximoNumero(@Param("empresaId") UUID empresaId);
}
