package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.ServicoDoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServicoDoContratoRepositorio extends JpaRepository<ServicoDoContrato, UUID> {

    List<ServicoDoContrato> findByContratoId(UUID contratoId);
}
