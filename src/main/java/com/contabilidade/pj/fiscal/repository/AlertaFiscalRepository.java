package com.contabilidade.pj.fiscal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.fiscal.entity.AlertaFiscal;
import com.contabilidade.pj.fiscal.entity.TipoAlertaFiscal;

public interface AlertaFiscalRepository extends JpaRepository<AlertaFiscal, Long> {
    List<AlertaFiscal> findByResolvidoFalseOrderByDataAlvoAsc();
}
