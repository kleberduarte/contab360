package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.entity.NotaFiscal;
import com.contabilidade.pj.fiscal.entity.TipoDocumentoFiscal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Service;

@Service
public class DanfeSimulacaoPdfService {

    private static final float MARGIN = 40;
    private static final float LINE = 13;

    private final NfseSaoPauloSimulacaoPdfService nfseSaoPauloSimulacaoPdfService;

    public DanfeSimulacaoPdfService(NfseSaoPauloSimulacaoPdfService nfseSaoPauloSimulacaoPdfService) {
        this.nfseSaoPauloSimulacaoPdfService = nfseSaoPauloSimulacaoPdfService;
    }

    public byte[] gerar(NotaFiscal nota) throws IOException {
        if (nota.getTipoDocumento() == TipoDocumentoFiscal.NFSE) {
            return nfseSaoPauloSimulacaoPdfService.gerar(nota);
        }
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        String chave = nota.getChaveAcesso();
        if (chave == null || chave.isBlank()) {
            chave = "00000000000000000000000000000000000000000000";
        }

        List<String> linhas = new ArrayList<>();
        linhas.add("DOCUMENTO AUXILIAR DA NOTA FISCAL ELETRONICA - DANFE");
        linhas.add("(SIMULACAO - SEM VALOR FISCAL - NAO VALIDO COMO DOCUMENTO FISCAL)");
        linhas.add("");
        linhas.add("Chave de acesso (simulada): " + formatarChaveVisual(chave));
        linhas.add("Protocolo de autorizacao (simulado): " + sanitize(nota.getProtocoloAutorizacao()));
        linhas.add("Modo SEFAZ: " + sanitize(nota.getSefazModo()));
        linhas.add("");
        linhas.add("EMITENTE CNPJ: " + formatarCnpjVisual(nota.getDocumentoEmitente()));
        linhas.add("DESTINATARIO CNPJ: " + formatarCnpjVisual(nota.getDocumentoDestinatario()));
        linhas.add("Tipo documento: " + nota.getTipoDocumento().name());
        linhas.add("Operacao: " + nota.getTipoOperacao().name());
        linhas.add("Data emissao: " + nota.getDataEmissao());
        linhas.add("Valor total: " + String.format(Locale.ROOT, "%.2f", nota.getValorTotal()));
        linhas.add("Municipio / UF: " + sanitize(nota.getMunicipio()) + " / " + sanitize(nota.getUf()));
        linhas.add("");
        linhas.add("Observacao: " + sanitize(nota.getSefazMensagem()));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageH = page.getMediaBox().getHeight();
            float pageW = page.getMediaBox().getWidth();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                marcarAguaSimulacao(cs, pageW, pageH, fontBold);

                cs.beginText();
                cs.setFont(font, 9);
                cs.newLineAtOffset(MARGIN, pageH - MARGIN);
                for (String linha : linhas) {
                    String t = linha.length() > 95 ? linha.substring(0, 92) + "..." : linha;
                    if (t.isEmpty()) {
                        t = " ";
                    }
                    cs.showText(t);
                    cs.newLineAtOffset(0, -LINE);
                }
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static void marcarAguaSimulacao(PDPageContentStream cs, float pageW, float pageH, PDFont fontBold) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.14f);
        cs.saveGraphicsState();
        cs.setGraphicsStateParameters(gs);
        cs.setNonStrokingColor(0.85f, 0.25f, 0.25f);
        cs.beginText();
        cs.setFont(fontBold, 28);
        cs.newLineAtOffset(MARGIN, pageH / 2);
        cs.showText("SIMULACAO - SEM VALOR FISCAL");
        cs.endText();
        cs.restoreGraphicsState();
    }

    private static String formatarChaveVisual(String chave) {
        String d = chave.replaceAll("\\D", "");
        if (d.length() != 44) {
            return chave;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 44; i += 4) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(d, i, i + 4);
        }
        return sb.toString();
    }

    private static String formatarCnpjVisual(String c14) {
        if (c14 == null || c14.length() != 14) {
            return c14 == null ? "" : c14;
        }
        return c14.substring(0, 2) + "." + c14.substring(2, 5) + "." + c14.substring(5, 8) + "/"
                + c14.substring(8, 12) + "-" + c14.substring(12);
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
