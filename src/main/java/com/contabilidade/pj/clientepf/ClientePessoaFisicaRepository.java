package com.contabilidade.pj.clientepf;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientePessoaFisicaRepository extends JpaRepository<ClientePessoaFisica, Long> {

    Optional<ClientePessoaFisica> findByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<ClientePessoaFisica> findAllByAtivoTrueOrderByNomeCompletoAsc();
}
