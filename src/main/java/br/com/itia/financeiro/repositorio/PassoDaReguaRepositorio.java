package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.PassoDaRegua;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassoDaReguaRepositorio extends JpaRepository<PassoDaRegua, UUID> {

    List<PassoDaRegua> findByEmpresaIdOrderByOrdem(UUID empresaId);

    List<PassoDaRegua> findByEmpresaIdAndAtivoTrueOrderByOrdem(UUID empresaId);

    Optional<PassoDaRegua> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
