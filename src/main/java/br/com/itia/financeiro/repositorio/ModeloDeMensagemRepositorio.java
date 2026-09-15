package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ModeloDeMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModeloDeMensagemRepositorio extends JpaRepository<ModeloDeMensagem, UUID> {

    List<ModeloDeMensagem> findByEmpresaIdOrderByNome(UUID empresaId);

    List<ModeloDeMensagem> findByEmpresaIdAndAtivoTrueOrderByNome(UUID empresaId);

    Optional<ModeloDeMensagem> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByEmpresaId(UUID empresaId);
}
