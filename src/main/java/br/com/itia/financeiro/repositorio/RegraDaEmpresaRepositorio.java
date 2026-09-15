package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.RegraDaEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegraDaEmpresaRepositorio extends JpaRepository<RegraDaEmpresa, UUID> {
}
