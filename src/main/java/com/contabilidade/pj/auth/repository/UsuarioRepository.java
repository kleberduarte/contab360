package com.contabilidade.pj.auth.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.entity.PerfilUsuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    long countByEmpresa_Id(Long empresaId);

    long countByClientePessoaFisica_Id(Long clientePessoaFisicaId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa LEFT JOIN FETCH u.clientePessoaFisica ORDER BY u.nome")
    List<Usuario> findAllComEmpresa();
}
