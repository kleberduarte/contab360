package com.contabilidade.pj.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.contabilidade.pj.auth.entity.SessaoAcesso;
import com.contabilidade.pj.auth.entity.Usuario;

public interface SessaoAcessoRepository extends JpaRepository<SessaoAcesso, Long> {
    Optional<SessaoAcesso> findByToken(String token);
    @Query("""
            select s from SessaoAcesso s join fetch s.usuario u
            left join fetch u.empresa
            left join fetch u.clientePessoaFisica
            where s.token = :token
            """)
    Optional<SessaoAcesso> findByTokenComUsuario(String token);
    void deleteByExpiraEmBefore(LocalDateTime dataHora);
}
