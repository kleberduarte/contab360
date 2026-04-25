package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.entity.NotaFiscal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Transmite lote RPS (layout ABRASF base) via SOAP 1.2 com TLS cliente (certificado A1).
 * Ajuste envelopes/tags aos XSD/WSDL oficiais da PMSP quando integrar em homologação.
 */
public final class NfseSaoPauloClienteImpl implements NfseSaoPauloCliente {

    @Override
    public ResultadoEmissaoSefaz transmitirNfse(
            NotaFiscal nota,
            NfseProperties.SaoPaulo saoPaulo,
            String certificadoPathAbsoluto,
            String certificadoSenha
    ) {
        try {
            Document doc = NfseAbrasfEnvioLoteBuilder.montar(nota, saoPaulo);
            Element infRps = NfseAbrasfEnvioLoteBuilder.encontrarInfRps(doc, saoPaulo.getAbrasfNamespace());
            if (!saoPaulo.isPularAssinaturaXml()) {
                NfseXmlSignerPkcs12.assinarInfRps(
                        infRps,
                        certificadoPathAbsoluto,
                        certificadoSenha,
                        saoPaulo.isAssinaturaSha1()
                );
            }
            String xmlAssinado = NfseDomWriter.documentToString(doc);
            String soap = NfseSoapEnvelopeBuilder.montarEnvelope(xmlAssinado, saoPaulo);
            String resposta = NfseSoapClientTls.post(
                    saoPaulo.getEndpointEnvio(),
                    soap,
                    certificadoPathAbsoluto,
                    certificadoSenha,
                    saoPaulo
            );
            return NfseRespostaEnvioParser.interpretar(resposta);
        } catch (Exception e) {
            return ResultadoEmissaoSefaz.nfseRealErro(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
