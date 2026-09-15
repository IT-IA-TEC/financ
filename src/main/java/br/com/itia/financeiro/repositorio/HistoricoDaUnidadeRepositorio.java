package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.HistoricoDaUnidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoDaUnidadeRepositorio extends JpaRepository<HistoricoDaUnidade, UUID> {

    List<HistoricoDaUnidade> findByUnidadeIdOrderByQuandoDesc(UUID unidadeId);
}
