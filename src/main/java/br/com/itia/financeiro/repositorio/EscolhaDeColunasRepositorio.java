package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.EscolhaDeColunas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EscolhaDeColunasRepositorio extends JpaRepository<EscolhaDeColunas, UUID> {
}
