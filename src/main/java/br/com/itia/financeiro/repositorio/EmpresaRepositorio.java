package br.com.itia.financeiro.repositorio;

import br.com.itia.financeiro.dominio.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {

    Optional<Empresa> findByApelido(String apelido);

    List<Empresa> findByAtivaTrueOrderByNome();
}
