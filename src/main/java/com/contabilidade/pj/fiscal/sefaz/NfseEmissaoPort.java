package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;

/**
 * Porta municipal (NFS-e). Separada de {@link AutorizacaoFiscalPort} (NFe/SEFAZ estadual).
 */
public interface NfseEmissaoPort {

    /**
     * Chamado após persistir a nota ({@link NotaFiscal#getId()} disponível).
     */
    ResultadoEmissaoSefaz emitir(NotaFiscal nota);
}
