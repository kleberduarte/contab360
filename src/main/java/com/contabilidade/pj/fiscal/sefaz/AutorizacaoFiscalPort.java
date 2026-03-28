package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;

/**
 * Porta de saída para autorização eletrônica (SEFAZ NFe / extensível). Implementações: simulação local e, no futuro, cliente SOAP assinado.
 */
public interface AutorizacaoFiscalPort {

    /**
     * Executado após a nota estar persistida (com {@link NotaFiscal#getId()} disponível).
     */
    ResultadoEmissaoSefaz solicitarAutorizacao(NotaFiscal nota);
}
