package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Recorrencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecorrenciaRepositorio extends JpaRepository<Recorrencia, UUID> {

    List<Recorrencia> findByEmpresaIdOrderByDescricao(UUID empresaId);

    Optional<Recorrencia> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
