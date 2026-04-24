package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;

/** Modo explicitamente desligado: nota gravada sem autorização municipal automática. */
public final class NfseEmissaoDesligada implements NfseEmissaoPort {

    @Override
    public ResultadoEmissaoSefaz emitir(NotaFiscal nota) {
        return ResultadoEmissaoSefaz.nfseModoDesligado(
                "NFSe municipal: modo DESLIGADO (contab360.nfse.modo=DESLIGADO). "
                        + "Nenhuma transmissao a PMSP. Use SIMULACAO para demo ou REAL para integração futura."
        );
    }
}
