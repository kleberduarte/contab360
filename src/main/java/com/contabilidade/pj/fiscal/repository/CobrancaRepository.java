package com.contabilidade.pj.fiscal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.fiscal.entity.Cobranca;
import com.contabilidade.pj.fiscal.entity.StatusCobranca;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {
}
