package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;
import java.time.format.DateTimeFormatter;

/**
 * Gera chave e protocolo fictícios no formato numérico esperado para demonstração e PDF DANFE simulado.
 */
public final class AutorizacaoFiscalSimulacao implements AutorizacaoFiscalPort {

    @Override
    public ResultadoEmissaoSefaz solicitarAutorizacao(NotaFiscal nota) {
        String chave = SimulacaoFiscalSupport.gerarChave44Digitos();
        String protocolo = "SIM" + nota.getDataEmissao().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%010d", nota.getId());
        String msg = "Autorizacao simulada (sem transmissao a SEFAZ). Para ambiente real: contab360.sefaz.modo=REAL, certificado e URL do webservice.";
        return ResultadoEmissaoSefaz.simulacaoOk(chave, protocolo, msg);
    }
}
