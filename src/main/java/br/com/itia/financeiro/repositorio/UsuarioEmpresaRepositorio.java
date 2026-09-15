package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioEmpresaRepositorio
        extends JpaRepository<UsuarioEmpresa, UsuarioEmpresa.Chave> {

    @Query("select ue from UsuarioEmpresa ue where ue.chave.usuarioId = :usuarioId")
    List<UsuarioEmpresa> crachasDe(@Param("usuarioId") UUID usuarioId);

    @Query("""
            select ue from UsuarioEmpresa ue
            where ue.chave.usuarioId = :usuarioId and ue.chave.empresaId = :empresaId
            """)
    Optional<UsuarioEmpresa> cracha(@Param("usuarioId") UUID usuarioId,
                                    @Param("empresaId") UUID empresaId);

    @Query("select ue from UsuarioEmpresa ue where ue.chave.empresaId = :empresaId")
    List<UsuarioEmpresa> daEmpresa(@Param("empresaId") UUID empresaId);

    @Query("select count(ue) from UsuarioEmpresa ue where ue.chave.empresaId = :empresaId")
    long quantosNaEmpresa(@Param("empresaId") UUID empresaId);
}
