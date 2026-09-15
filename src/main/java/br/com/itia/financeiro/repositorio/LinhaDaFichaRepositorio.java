package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.LinhaDaFicha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinhaDaFichaRepositorio extends JpaRepository<LinhaDaFicha, UUID> {

    List<LinhaDaFicha> findByEmpresaIdOrderByOrdem(UUID empresaId);

    List<LinhaDaFicha> findByEmpresaIdAndAtivaTrueOrderByOrdem(UUID empresaId);

    Optional<LinhaDaFicha> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
