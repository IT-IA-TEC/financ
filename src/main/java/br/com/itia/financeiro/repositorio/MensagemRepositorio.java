package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensagemRepositorio extends JpaRepository<Mensagem, UUID> {

    List<Mensagem> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    List<Mensagem> findByLoteIdOrderByCriadoEm(UUID loteId);

    List<Mensagem> findByEmpresaIdAndUnidadeIdOrderByCriadoEm(UUID empresaId, UUID unidadeId);

    List<Mensagem> findByEmpresaIdAndSituacaoOrderByCriadoEm(UUID empresaId, String situacao);

    Optional<Mensagem> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Mensagem> findByEmpresaIdAndIdExterno(UUID empresaId, String idExterno);

    void deleteByLoteId(UUID loteId);

    /**
     * Se este titulo ja recebeu mensagem por este canal depois de tal hora.
     *
     * E o que impede o mesmo cliente de levar tres cobrancas do mesmo boleto
     * no mesmo dia, quando alguem dispara duas vezes sem querer.
     */
    @Query("""
            select count(m) from Mensagem m
            where m.empresaId = :empresaId
              and m.tituloId = :tituloId
              and m.canal = :canal
              and m.direcao = 'SAIDA'
              and m.situacao <> 'CANCELADA'
              and m.criadoEm >= :desde
            """)
    long quantasJaForam(@Param("empresaId") UUID empresaId,
                        @Param("tituloId") UUID tituloId,
                        @Param("canal") String canal,
                        @Param("desde") OffsetDateTime desde);

    long countByEmpresaIdAndSituacao(UUID empresaId, String situacao);
}
