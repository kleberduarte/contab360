package com.contabilidade.pj.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoAcessoRepository extends JpaRepository<SessaoAcesso, Long> {
    Optional<SessaoAcesso> findByToken(String token);
    void deleteByExpiraEmBefore(LocalDateTime dataHora);
}
