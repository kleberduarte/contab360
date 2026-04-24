package com.contabilidade.pj.fiscal.sefaz;

final class NfseSoapEnvelopeBuilder {

    private NfseSoapEnvelopeBuilder() {
    }

    static String montarEnvelope(String xmlAssinadoUtf8, NfseProperties.SaoPaulo sp) {
        String cdata = xmlAssinadoUtf8.replace("]]>", "]]]]><![CDATA[>");
        String ns = sp.getSoapNamespace();
        String req = sp.getSoapRequestElementLocal();
        String msgEl = sp.getSoapMensagemXmlElement();
        String versao = escapeXml(sp.getSoapVersaoElemento());
        String prefix = "nfe";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\" "
                + "xmlns:" + prefix + "=\"" + escapeXml(ns) + "\">\n"
                + "  <soap12:Header/>\n"
                + "  <soap12:Body>\n"
                + "    <" + prefix + ":" + req + ">\n"
                + "      <" + prefix + ":Versao>" + versao + "</" + prefix + ":Versao>\n"
                + "      <" + prefix + ":" + msgEl + "><![CDATA[" + cdata + "]]></" + prefix + ":" + msgEl + ">\n"
                + "    </" + prefix + ":" + req + ">\n"
                + "  </soap12:Body>\n"
                + "</soap12:Envelope>";
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
