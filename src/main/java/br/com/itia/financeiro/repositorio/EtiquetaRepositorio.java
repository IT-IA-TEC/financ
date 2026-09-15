package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtiquetaRepositorio extends JpaRepository<Etiqueta, UUID> {

    List<Etiqueta> findByEmpresaIdOrderByNome(UUID empresaId);

    List<Etiqueta> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    List<Etiqueta> findByEmpresaIdAndEscopoAndAtivoTrueOrderByNome(UUID empresaId, String escopo);

    Optional<Etiqueta> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Etiqueta> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
