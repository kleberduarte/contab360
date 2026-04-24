package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;

/**
 * Placeholder para integração real: assinar XML NFe, SOAP NFeAutorização, tratar retorno e DANFE oficial.
 * Substitua por implementação concreta ou adapte para biblioteca terceirizada.
 */
public final class AutorizacaoFiscalRealStub implements AutorizacaoFiscalPort {

    private final SefazNfeProperties properties;

    public AutorizacaoFiscalRealStub(SefazNfeProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResultadoEmissaoSefaz solicitarAutorizacao(NotaFiscal nota) {
        String detalhe = "Implementar: XML NFe + assinatura PKCS12 (certificado em "
                + (properties.getCertificadoPath().isBlank() ? "[contab360.sefaz.certificado-path]" : properties.getCertificadoPath())
                + "), POST "
                + (properties.getNfeAutorizacaoUrl().isBlank() ? "[contab360.sefaz.nfe-autorizacao-url]" : properties.getNfeAutorizacaoUrl())
                + ", ambiente " + properties.getAmbiente() + ". Nota id=" + nota.getId() + ".";
        return ResultadoEmissaoSefaz.realPendente(detalhe);
    }
}
