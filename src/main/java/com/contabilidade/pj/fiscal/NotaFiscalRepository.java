package com.contabilidade.pj.fiscal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {
    List<NotaFiscal> findByTipoOperacaoOrderByDataEmissaoDesc(TipoOperacaoFiscal tipoOperacao);

    List<NotaFiscal> findByTipoDocumentoOrderByDataEmissaoDesc(TipoDocumentoFiscal tipoDocumento);
}
