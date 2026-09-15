package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.AcompanhaTarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcompanhaTarefaRepositorio extends JpaRepository<AcompanhaTarefa, UUID> {

    List<AcompanhaTarefa> findByTarefaId(UUID tarefaId);

    List<AcompanhaTarefa> findByEmpresaIdAndQuem(UUID empresaId, String quem);
}
