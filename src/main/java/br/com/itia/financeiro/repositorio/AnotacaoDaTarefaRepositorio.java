package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.AnotacaoDaTarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnotacaoDaTarefaRepositorio extends JpaRepository<AnotacaoDaTarefa, UUID> {

    List<AnotacaoDaTarefa> findByTarefaIdOrderByCriadoEm(UUID tarefaId);
}
