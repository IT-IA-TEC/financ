package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.PermissaoLigada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissaoLigadaRepositorio extends JpaRepository<PermissaoLigada, UUID> {

    List<PermissaoLigada> findByEmpresaId(UUID empresaId);

    Optional<PermissaoLigada> findByEmpresaIdAndChave(UUID empresaId, String chave);
}
