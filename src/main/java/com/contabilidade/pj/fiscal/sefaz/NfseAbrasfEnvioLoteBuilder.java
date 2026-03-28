package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Monta documento ABRASF {@code EnviarLoteRpsEnvio} (base 2.03). A PMSP pode exigir outro root/namespaces
 * — ajuste {@link NfseProperties.SaoPaulo#getAbrasfNamespace()} e este builder conforme XSD oficial.
 */
final class NfseAbrasfEnvioLoteBuilder {

    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private NfseAbrasfEnvioLoteBuilder() {
    }

    static Document montar(NotaFiscal nota, NfseProperties.SaoPaulo sp) throws Exception {
        String ns = sp.getAbrasfNamespace();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();

        Element root = doc.createElementNS(ns, "EnviarLoteRpsEnvio");
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", ns);
        doc.appendChild(root);

        Element lote = doc.createElementNS(ns, "LoteRps");
        lote.setAttribute("Id", "Lote" + nota.getId());
        lote.setAttribute("versao", sp.getAbrasfVersaoLayout());
        root.appendChild(lote);

        appendText(doc, lote, ns, "NumeroLote", String.valueOf(nota.getId()));
        appendText(doc, lote, ns, "Cnpj", nota.getDocumentoEmitente());
        appendText(doc, lote, ns, "InscricaoMunicipal", soDigitosIm(nota.getNfseInscricaoMunicipalEmitente()));
        appendText(doc, lote, ns, "QuantidadeRps", "1");

        Element listaRps = doc.createElementNS(ns, "ListaRps");
        lote.appendChild(listaRps);

        Element rps = doc.createElementNS(ns, "Rps");
        listaRps.appendChild(rps);

        String rpsId = "rps" + nota.getId();
        Element infRps = doc.createElementNS(ns, "InfRps");
        infRps.setAttribute("Id", rpsId);
        rps.appendChild(infRps);

        Element idRps = doc.createElementNS(ns, "IdentificacaoRps");
        infRps.appendChild(idRps);
        appendText(doc, idRps, ns, "Numero", String.valueOf(nota.getId()));
        appendText(doc, idRps, ns, "Serie", sp.getSerieRpsPadrao());
        appendText(doc, idRps, ns, "Tipo", "1");

        LocalDateTime emissao = nota.getCriadoEm() != null
                ? nota.getCriadoEm()
                : nota.getDataEmissao().atStartOfDay();
        appendText(doc, infRps, ns, "DataEmissao", ISO_TS.format(emissao));

        appendText(doc, infRps, ns, "NaturezaOperacao", "1");
        appendText(doc, infRps, ns, "RegimeEspecialTributacao", "0");
        appendText(doc, infRps, ns, "OptanteSimplesNacional", "2");
        appendText(doc, infRps, ns, "IncentivadorCultural", "2");
        appendText(doc, infRps, ns, "Status", "1");

        Element servico = doc.createElementNS(ns, "Servico");
        infRps.appendChild(servico);
        appendText(doc, servico, ns, "ItemListaServico", extrairItemListaServico(nota));
        appendText(doc, servico, ns, "CodigoTributacaoMunicipal", "");
        appendText(doc, servico, ns, "Discriminacao", textoDiscriminacao(nota));
        appendText(doc, servico, ns, "CodigoMunicipio", sp.getCodigoMunicipioIbge());
        appendText(doc, servico, ns, "IssRetido", "2");

        BigDecimal total = nota.getValorTotal() != null ? nota.getValorTotal() : BigDecimal.ZERO;
        BigDecimal ded = nota.getNfseValorDeducoes() != null ? nota.getNfseValorDeducoes() : BigDecimal.ZERO;
        BigDecimal aliq = nota.getNfseAliquotaIss() != null ? nota.getNfseAliquotaIss() : new BigDecimal("5.00");
        BigDecimal base = total.subtract(ded).max(BigDecimal.ZERO);
        BigDecimal vIss = base.multiply(aliq).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Element valores = doc.createElementNS(ns, "Valores");
        servico.appendChild(valores);
        appendText(doc, valores, ns, "ValorServicos", money(total));
        appendText(doc, valores, ns, "ValorDeducoes", money(ded));
        appendText(doc, valores, ns, "ValorPis", "0.00");
        appendText(doc, valores, ns, "ValorCofins", "0.00");
        appendText(doc, valores, ns, "ValorInss", "0.00");
        appendText(doc, valores, ns, "ValorIr", "0.00");
        appendText(doc, valores, ns, "ValorCsll", "0.00");
        appendText(doc, valores, ns, "OutrasRetencoes", "0.00");
        appendText(doc, valores, ns, "ValorIss", money(vIss));
        appendText(doc, valores, ns, "Aliquota", money(aliq));
        appendText(doc, valores, ns, "BaseCalculo", money(base));
        appendText(doc, valores, ns, "DescontoIncondicionado", "0.00");
        appendText(doc, valores, ns, "DescontoCondicionado", "0.00");

        Element prest = doc.createElementNS(ns, "Prestador");
        infRps.appendChild(prest);
        appendText(doc, prest, ns, "Cnpj", nota.getDocumentoEmitente());
        appendText(doc, prest, ns, "InscricaoMunicipal", soDigitosIm(nota.getNfseInscricaoMunicipalEmitente()));

        Element tom = doc.createElementNS(ns, "Tomador");
        infRps.appendChild(tom);
        Element idTom = doc.createElementNS(ns, "IdentificacaoTomador");
        tom.appendChild(idTom);
        Element cpfCnpj = doc.createElementNS(ns, "CpfCnpj");
        idTom.appendChild(cpfCnpj);
        appendText(doc, cpfCnpj, ns, "Cnpj", nota.getDocumentoDestinatario());
        appendText(doc, tom, ns, "RazaoSocial", razaoTomador(nota));
        return doc;
    }

    static Element encontrarInfRps(Document doc, String ns) {
        var list = doc.getElementsByTagNameNS(ns, "InfRps");
        if (list.getLength() == 0) {
            throw new IllegalStateException("InfRps nao encontrado no XML montado.");
        }
        return (Element) list.item(0);
    }

    private static void appendText(Document doc, Element parent, String ns, String name, String value) {
        Element e = doc.createElementNS(ns, name);
        e.setTextContent(value == null ? "" : value);
        parent.appendChild(e);
    }

    private static String soDigitosIm(String im) {
        if (im == null || im.isBlank()) {
            return "0";
        }
        String d = im.replaceAll("\\D", "");
        return d.isEmpty() ? "0" : d;
    }

    private static String extrairItemListaServico(NotaFiscal nota) {
        String cod = nota.getNfseCodigoServicoTexto();
        if (cod != null) {
            String digits = cod.replaceAll("\\D", "");
            if (digits.length() >= 4) {
                return digits.substring(0, 2) + "." + digits.substring(2, 4);
            }
        }
        return "01.07";
    }

    private static String textoDiscriminacao(NotaFiscal nota) {
        String d = nota.getNfseDiscriminacao();
        if (d != null && !d.isBlank()) {
            return d.trim();
        }
        return "Servico prestado";
    }

    private static String razaoTomador(NotaFiscal nota) {
        String r = nota.getNfseRazaoTomador();
        if (r != null && !r.isBlank()) {
            return r.trim();
        }
        return "Tomador";
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
