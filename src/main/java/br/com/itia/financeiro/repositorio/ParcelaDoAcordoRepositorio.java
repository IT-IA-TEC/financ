package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ParcelaDoAcordo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParcelaDoAcordoRepositorio extends JpaRepository<ParcelaDoAcordo, UUID> {

    List<ParcelaDoAcordo> findByAcordoIdOrderByOrdem(UUID acordoId);
}
