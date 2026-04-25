package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.entity.NotaFiscal;
import java.time.format.DateTimeFormatter;

public final class NfseEmissaoSimulacao implements NfseEmissaoPort {

    @Override
    public ResultadoEmissaoSefaz emitir(NotaFiscal nota) {
        String chave = SimulacaoFiscalSupport.gerarChave44Digitos();
        String protocolo = "NFSE-SIM-"
                + nota.getDataEmissao().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%010d", nota.getId());
        String msg = "NFS-e simulada (sem transmissao a Prefeitura de Sao Paulo). "
                + "Integracao real (ordem): contab360.nfse.integracao-pmsp-ativa=true, contab360.nfse.modo=REAL, "
                + "endpoints e certificado, contab360.nfse.sao-paulo.transmissao-habilitada=true.";
        return ResultadoEmissaoSefaz.simulacaoOk(chave, protocolo, msg);
    }
}
