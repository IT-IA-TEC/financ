package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.OperacaoIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperacaoIntegracaoRepositorio extends JpaRepository<OperacaoIntegracao, UUID> {

    List<OperacaoIntegracao> findByIntegracaoIdOrderByNome(UUID integracaoId);

    Optional<OperacaoIntegracao> findByIdAndIntegracaoId(UUID id, UUID integracaoId);
}
