package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.VinculoDeEtiqueta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VinculoDeEtiquetaRepositorio extends JpaRepository<VinculoDeEtiqueta, UUID> {

    List<VinculoDeEtiqueta> findByEntidadeAndEntidadeId(String entidade, UUID entidadeId);

    List<VinculoDeEtiqueta> findByEmpresaIdAndEntidade(UUID empresaId, String entidade);

    void deleteByEntidadeAndEntidadeId(String entidade, UUID entidadeId);
}
