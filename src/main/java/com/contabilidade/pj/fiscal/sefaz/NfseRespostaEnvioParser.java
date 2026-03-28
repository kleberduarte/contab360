package com.contabilidade.pj.fiscal.sefaz;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class NfseRespostaEnvioParser {

    private NfseRespostaEnvioParser() {
    }

    static ResultadoEmissaoSefaz interpretar(String respostaSoap) {
        try {
            Document doc = parse(respostaSoap);
            String fault = textoXPath(doc, "string(//*[local-name()='faultstring'])");
            if (!fault.isBlank()) {
                return ResultadoEmissaoSefaz.nfseRealErro("SOAP Fault: " + trunc(fault, 900));
            }

            Document alvo = doc;
            String cdataInterno = textoXPath(doc, "string(//*[local-name()='MensagemXML']/text())");
            if (cdataInterno != null && cdataInterno.contains("<")) {
                try {
                    alvo = parse(cdataInterno);
                } catch (Exception ignored) {
                    alvo = doc;
                }
            }

            String erros = coletarMensagensRetorno(alvo);
            if (!erros.isBlank()) {
                return ResultadoEmissaoSefaz.nfseRealErro(trunc(erros, 900));
            }

            String numero = firstNonBlank(
                    textoXPath(alvo, "string(//*[local-name()='NumeroNfse'])"),
                    textoXPath(alvo, "string(//*[local-name()='NumeroNFSe'])"),
                    textoXPath(alvo, "string(//*[local-name()='NumeroNota'])"),
                    textoXPath(alvo, "string(//*[local-name()='Numero'])")
            );
            String codVer = firstNonBlank(
                    textoXPath(alvo, "string(//*[local-name()='CodigoVerificacao'])"),
                    textoXPath(alvo, "string(//*[local-name()='CodigoAutenticidade'])")
            );
            String prot = firstNonBlank(
                    textoXPath(alvo, "string(//*[local-name()='Protocolo'])"),
                    textoXPath(alvo, "string(//*[local-name()='NumeroProtocolo'])")
            );

            if (numero.isBlank() && codVer.isBlank()) {
                return ResultadoEmissaoSefaz.nfseRealErro(
                        "Resposta sem Numero/CodigoVerificacao reconhecidos. Trecho: "
                                + trunc(respostaSoap.replaceAll("\\s+", " "), 700)
                );
            }

            String ident = numero.isBlank() ? codVer : numero;
            return ResultadoEmissaoSefaz.nfseRealSucesso(
                    ident,
                    prot,
                    numero,
                    codVer,
                    "NFS-e municipal: retorno analisado com sucesso (valide namespaces no ambiente real)."
            );
        } catch (Exception e) {
            return ResultadoEmissaoSefaz.nfseRealErro("Falha ao interpretar resposta SOAP: " + e.getMessage());
        }
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            /* feature opcional */
        }
        return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String textoXPath(Document doc, String expr) throws Exception {
        var xp = XPathFactory.newInstance().newXPath();
        String t = (String) xp.evaluate(expr, doc, XPathConstants.STRING);
        return t == null ? "" : t.trim();
    }

    private static String coletarMensagensRetorno(Document doc) throws Exception {
        var xp = XPathFactory.newInstance().newXPath();
        NodeList nos = (NodeList) xp.evaluate(
                "//*[local-name()='ListaMensagemRetorno']//*[local-name()='MensagemRetorno']",
                doc,
                XPathConstants.NODESET
        );
        if (nos == null || nos.getLength() == 0) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < nos.getLength(); i++) {
            Node n = nos.item(i);
            if (n instanceof org.w3c.dom.Element el) {
                String cod = textoFilho(el, "Codigo");
                String msg = textoFilho(el, "Mensagem");
                if (!msg.isBlank()) {
                    partes.add((cod.isBlank() ? "" : cod + " - ") + msg);
                }
            }
        }
        return partes.isEmpty() ? "" : String.join("; ", partes);
    }

    private static String textoFilho(org.w3c.dom.Element el, String local) {
        NodeList nl = el.getElementsByTagNameNS("*", local);
        if (nl.getLength() == 0) {
            return "";
        }
        Node n = nl.item(0);
        return n == null || n.getTextContent() == null ? "" : n.getTextContent().trim();
    }

    private static String firstNonBlank(String... v) {
        if (v == null) {
            return "";
        }
        for (String s : v) {
            if (s != null && !s.isBlank()) {
                return s.trim();
            }
        }
        return "";
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
