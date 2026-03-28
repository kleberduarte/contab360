package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;

/**
 * Cliente municipal NFS-e São Paulo. Substitua o stub por chamadas reais (SOAP/REST, assinatura XML, lote, etc.)
 * seguindo o manual técnico vigente da PMSP.
 */
public interface NfseSaoPauloCliente {

    ResultadoEmissaoSefaz transmitirNfse(
            NotaFiscal nota,
            NfseProperties.SaoPaulo saoPaulo,
            String certificadoPathAbsoluto,
            String certificadoSenha
    );
}
