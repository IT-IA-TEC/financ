package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.AgenteIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgenteIaRepositorio extends JpaRepository<AgenteIa, UUID> {
}
