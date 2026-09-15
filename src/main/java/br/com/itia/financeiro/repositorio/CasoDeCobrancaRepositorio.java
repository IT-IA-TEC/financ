package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.CasoDeCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CasoDeCobrancaRepositorio extends JpaRepository<CasoDeCobranca, UUID> {

    List<CasoDeCobranca> findByEmpresaId(UUID empresaId);

    Optional<CasoDeCobranca> findByEmpresaIdAndUnidadeId(UUID empresaId, UUID unidadeId);
}
