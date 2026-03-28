package com.contabilidade.pj.fiscal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaFiscalRepository extends JpaRepository<AlertaFiscal, Long> {
    List<AlertaFiscal> findByResolvidoFalseOrderByDataAlvoAsc();
}
