package com.contabilidade.pj.fiscal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.fiscal.entity.CadastroDocumentoFiscal;
import com.contabilidade.pj.fiscal.entity.TipoCadastroDocumento;

public interface CadastroDocumentoFiscalRepository extends JpaRepository<CadastroDocumentoFiscal, Long> {
}
