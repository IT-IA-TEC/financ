package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.AnaliseDeCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnaliseDeCreditoRepositorio extends JpaRepository<AnaliseDeCredito, UUID> {
}
