package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Obrigacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda busca aqui pede a empresa. Nao existe consulta que atravesse duas.
 */
public interface ObrigacaoRepositorio extends JpaRepository<Obrigacao, UUID> {

    List<Obrigacao> findByEmpresaIdOrderByVencimento(UUID empresaId);

    Optional<Obrigacao> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Obrigacao> findByGrupoParcelasOrderByParcela(UUID grupoParcelas);

    List<Obrigacao> findByRecorrenciaIdAndCompetencia(UUID recorrenciaId,
                                                      java.time.LocalDate competencia);

    @Query("select coalesce(max(o.numero), 0) + 1 from Obrigacao o where o.empresa.id = :empresaId")
    Long proximoNumero(@Param("empresaId") UUID empresaId);
}
