package com.contabilidade.pj.lgpd;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimentoTitularRepository extends JpaRepository<ConsentimentoTitular, Long> {

    Optional<ConsentimentoTitular> findTopByUsuario_IdAndVersaoPoliticaOrderByDataHoraDesc(
            Long usuarioId, String versaoPolitica);

    boolean existsByUsuario_IdAndVersaoPolitica(Long usuarioId, String versaoPolitica);
}
