package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TituloRepositorio extends JpaRepository<Titulo, UUID> {

    List<Titulo> findByEmpresaIdOrderByVencimentoDesc(UUID empresaId);

    List<Titulo> findByEmpresaIdAndSituacaoInOrderByVencimento(
            UUID empresaId, List<SituacaoTitulo> situacoes);

    List<Titulo> findByClienteIdOrderByVencimentoDesc(UUID clienteId);

    Optional<Titulo> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Titulo> findByEmpresaIdAndIdentificadorPix(UUID empresaId, String identificadorPix);

    /**
     * O proximo numero da empresa. Cada empresa tem a sua propria contagem:
     * a YOU tem o titulo 1 e a 40% tambem.
     *
     * Se duas pessoas lancarem no mesmo instante, o banco barra pela chave
     * unica (empresa, numero) e uma delas recebe o aviso para repetir.
     */
    @Query("""
            select coalesce(max(t.numero), 0) + 1 from Titulo t
            where t.empresa.id = :empresaId
            """)
    Long proximoNumero(@Param("empresaId") UUID empresaId);

    @Query("""
            select coalesce(sum(t.valor), 0) from Titulo t
            where t.empresa.id = :empresaId and t.situacao in ('ABERTO', 'PARCIAL')
            """)
    BigDecimal somarEmAberto(@Param("empresaId") UUID empresaId);

    @Query("""
            select coalesce(sum(t.valor), 0) from Titulo t
            where t.empresa.id = :empresaId
              and t.situacao in ('ABERTO', 'PARCIAL')
              and t.vencimento < :hoje
            """)
    BigDecimal somarVencido(@Param("empresaId") UUID empresaId, @Param("hoje") LocalDate hoje);

    long countByEmpresaIdAndSituacao(UUID empresaId, SituacaoTitulo situacao);

    long countByEmpresaId(UUID empresaId);
}
