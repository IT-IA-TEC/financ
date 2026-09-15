package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.FonteDeTarefas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FonteDeTarefasRepositorio extends JpaRepository<FonteDeTarefas, UUID> {

    List<FonteDeTarefas> findByEmpresaIdOrderByNome(UUID empresaId);

    List<FonteDeTarefas> findByEmpresaIdAndAtivaTrueOrderByNome(UUID empresaId);

    Optional<FonteDeTarefas> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
