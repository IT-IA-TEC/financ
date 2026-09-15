package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Cobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CobrancaRepositorio extends JpaRepository<Cobranca, UUID> {

    List<Cobranca> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    Optional<Cobranca> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Cobranca> findByPagadorIdOrderByCriadoEmDesc(UUID pagadorId);

    Optional<Cobranca> findByRealizadoId(UUID realizadoId);

    /** Ja existe cobranca desta contratacao neste periodo? */
    Optional<Cobranca> findByContratacaoIdAndReferencia(UUID contratacaoId, String referencia);
}
