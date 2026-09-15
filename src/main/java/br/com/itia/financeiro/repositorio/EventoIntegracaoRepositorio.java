package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.EventoIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoIntegracaoRepositorio extends JpaRepository<EventoIntegracao, Long> {

    List<EventoIntegracao> findTop50ByIntegracaoIdOrderByOcorridoEmDesc(UUID integracaoId);

    List<EventoIntegracao> findTop30ByEmpresaIdOrderByOcorridoEmDesc(UUID empresaId);

    long countByIntegracaoIdAndStatus(UUID integracaoId, String status);
}
