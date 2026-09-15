package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.LoteDeMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoteDeMensagemRepositorio extends JpaRepository<LoteDeMensagem, UUID> {

    List<LoteDeMensagem> findByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

    Optional<LoteDeMensagem> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
