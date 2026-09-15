package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetenciaRepositorio extends JpaRepository<Competencia, UUID> {

    Optional<Competencia> findByEmpresaIdAndReferencia(UUID empresaId, String referencia);

    List<Competencia> findByEmpresaIdOrderByReferenciaDesc(UUID empresaId);
}
