package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Agente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgenteRepositorio extends JpaRepository<Agente, UUID> {
}
