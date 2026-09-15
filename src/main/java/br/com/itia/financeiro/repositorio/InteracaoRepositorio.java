package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteracaoRepositorio extends JpaRepository<Interacao, UUID> {

    List<Interacao> findByPagadorIdOrderByOcorridoEmDesc(UUID pagadorId);

    List<Interacao> findByUnidadeIdOrderByOcorridoEmDesc(UUID unidadeId);

    List<Interacao> findByEmpresaIdAndTipoAndSituacaoOrderByDataPrometida(
            UUID empresaId, String tipo, String situacao);

    Optional<Interacao> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
