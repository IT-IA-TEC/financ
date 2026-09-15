package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.UsoDaIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsoDaIaRepositorio extends JpaRepository<UsoDaIa, UUID> {

    List<UsoDaIa> findTop50ByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    long countByEmpresaId(UUID empresaId);

    long countByEmpresaIdAndExecutadaTrue(UUID empresaId);
}
