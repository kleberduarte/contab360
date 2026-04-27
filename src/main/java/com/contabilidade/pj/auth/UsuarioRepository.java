package com.contabilidade.pj.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    long countByEmpresa_Id(Long empresaId);

    long countByClientePessoaFisica_Id(Long clientePessoaFisicaId);
}
