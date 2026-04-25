package com.contabilidade.pj.fiscal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.contabilidade.pj.fiscal.entity.NotaFiscal;
import com.contabilidade.pj.fiscal.entity.TipoDocumentoFiscal;
import com.contabilidade.pj.fiscal.entity.TipoOperacaoFiscal;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {
    List<NotaFiscal> findByTipoOperacaoOrderByDataEmissaoDesc(TipoOperacaoFiscal tipoOperacao);

    List<NotaFiscal> findByTipoDocumentoOrderByDataEmissaoDesc(TipoDocumentoFiscal tipoDocumento);
}
