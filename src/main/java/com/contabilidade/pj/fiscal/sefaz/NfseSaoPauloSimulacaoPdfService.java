package com.contabilidade.pj.fiscal.sefaz;

import com.contabilidade.pj.fiscal.NotaFiscal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Service;

/**
 * Layout alinhado ao cartao NFS-e da Prefeitura de Sao Paulo (demonstracao — sem valor fiscal).
 */
@Service
public class NfseSaoPauloSimulacaoPdfService {

    private static final float M = 28;
    private static final float CW = PDRectangle.A4.getWidth() - 2 * M;
    private static final DateTimeFormatter DH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DV = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DEF_RAZAO = "INSCRICAO PARA TESTE NFE - PJ/0001";
    private static final String DEF_END = "R PEDRO AMERICO 00032, 27 ANDAR - CENTRO - CEP: 01045-010";
    private static final String DEF_IM_PREST = "3.961.999-4";
    private static final String DEF_IM_TOM = "3.961.999-0";
    private static final String DEF_DISC = "teste";
    private static final String DEF_EMAIL = "-----";
    private static final String DEF_COD_SERV = "08567 - Centros de emagrecimento, spa e cong\u00EAneres.";

    /** Arquivo em {@code src/main/resources/fiscal/pmsp-brasao.png} (troque por outra arte, mantenha o nome). */
    private static final String BRASAO_PMSP_RESOURCE = "fiscal/pmsp-brasao.png";

    public byte[] gerar(NotaFiscal nota) throws IOException {
        PDFont fn = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont fb = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        String cnpjPrest = formatarCnpj(nota.getDocumentoEmitente());
        String cnpjTom = formatarCnpj(nota.getDocumentoDestinatario());
        String numero = padNumero(coalesceEmpty(nota.getNfseNumeroExibicao(), String.format(Locale.ROOT, "%08d", nota.getId())));
        String dataHora = nota.getCriadoEm() != null ? DH.format(nota.getCriadoEm()) : nota.getDataEmissao().format(DV) + " 12:00:00";
        String codVer = coalesceEmpty(nota.getNfseCodigoVerificacao(), codigoVerificacaoSimulado(nota.getId()));

        String razPrest = coalesceNull(nota.getNfseRazaoEmitente(), DEF_RAZAO);
        String razTom = coalesceNull(nota.getNfseRazaoTomador(), DEF_RAZAO);
        String endPrest = coalesceNull(nota.getNfseEnderecoEmitente(), DEF_END);
        String endTom = coalesceNull(nota.getNfseEnderecoTomador(), DEF_END);
        String imPrest = coalesceNull(nota.getNfseInscricaoMunicipalEmitente(), DEF_IM_PREST);
        String imTom = coalesceNull(nota.getNfseInscricaoMunicipalTomador(), DEF_IM_TOM);
        String disc = coalesceNull(nota.getNfseDiscriminacao(), DEF_DISC);
        String emailTom = coalesceNull(nota.getNfseEmailTomador(), DEF_EMAIL);
        String linhaCod = coalesceNull(nota.getNfseCodigoServicoTexto(), DEF_COD_SERV);

        String municipio = sanitize(nota.getMunicipio());
        String uf = sanitize(nota.getUf());
        if (municipio.isBlank()) {
            municipio = "S\u00E3o Paulo";
        }
        if (uf.isBlank()) {
            uf = "SP";
        }

        BigDecimal total = nota.getValorTotal();
        BigDecimal ded = nota.getNfseValorDeducoes() != null ? nota.getNfseValorDeducoes() : BigDecimal.ZERO;
        BigDecimal aliq = nota.getNfseAliquotaIss() != null ? nota.getNfseAliquotaIss() : new BigDecimal("5.00");
        BigDecimal base = total.subtract(ded).max(BigDecimal.ZERO);
        BigDecimal iss = base.multiply(aliq).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal creditoIptu = nota.getNfseCreditoIptu() != null ? nota.getNfseCreditoIptu() : BigDecimal.ZERO;

        LocalDate vencIss = nota.getNfseDataVencimentoIss() != null
                ? nota.getNfseDataVencimentoIss()
                : nota.getDataEmissao().plusDays(38);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float ph = page.getMediaBox().getHeight();
            float pw = page.getMediaBox().getWidth();
            PDImageXObject brasaoPmsp = carregarBrasaoPmsp(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                aguaMarca(cs, ph, fb);

                float y = ph - M;
                y = cabecalhoCartaoPmsp(cs, fn, fb, y, pw, numero, dataHora, codVer, brasaoPmsp);
                y -= 6;

                y = faixaTitulo(cs, fb, y, "PRESTADOR DE SERVICOS");
                y = corpoPrestador(cs, fn, y, cnpjPrest, imPrest, razPrest, endPrest, municipio, uf);
                y -= 6;

                y = faixaTitulo(cs, fb, y, "TOMADOR DE SERVICOS");
                y = corpoTomador(cs, fn, y, razTom, cnpjTom, imTom, endTom, municipio, uf, emailTom);
                y -= 6;

                y = faixaTitulo(cs, fb, y, "DISCRIMINACAO DOS SERVICOS");
                y = areaDiscriminacao(cs, fn, y, disc);
                y -= 8;

                y = faixaValorTotal(cs, fn, fb, y, total);
                y -= 6;

                y = linhaCodigoServico(cs, fn, y, linhaCod);
                y -= 4;

                y = quadroIss(cs, fn, fb, y, ded, base, aliq, iss, creditoIptu);
                y -= 8;

                rodapeCartao(cs, fn, y, vencIss);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static String padNumero(String n) {
        if (n == null || n.isBlank()) {
            return "00000001";
        }
        String d = n.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return "00000001";
        }
        d = d.length() > 8 ? d.substring(d.length() - 8) : d;
        return String.format(Locale.ROOT, "%8s", d).replace(' ', '0');
    }

    private static void aguaMarca(PDPageContentStream cs, float ph, PDFont fb) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.08f);
        cs.saveGraphicsState();
        cs.setGraphicsStateParameters(gs);
        cs.setNonStrokingColor(0.85f, 0.15f, 0.15f);
        cs.beginText();
        cs.setFont(fb, 20);
        cs.newLineAtOffset(M, ph * 0.48f);
        cs.showText("SIMULACAO - SEM VALOR FISCAL");
        cs.endText();
        cs.restoreGraphicsState();
    }

    private static PDImageXObject carregarBrasaoPmsp(PDDocument doc) throws IOException {
        byte[] bytes = lerRecursoBrasao();
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return PDImageXObject.createFromByteArray(doc, bytes, "pmsp-brasao");
    }

    private static byte[] lerRecursoBrasao() throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = NfseSaoPauloSimulacaoPdfService.class.getClassLoader();
        }
        InputStream stream = cl != null ? cl.getResourceAsStream(BRASAO_PMSP_RESOURCE) : null;
        if (stream == null) {
            stream = NfseSaoPauloSimulacaoPdfService.class.getResourceAsStream("/" + BRASAO_PMSP_RESOURCE);
        }
        if (stream == null) {
            return null;
        }
        try (InputStream in = stream) {
            return in.readAllBytes();
        }
    }

    private static float cabecalhoCartaoPmsp(
            PDPageContentStream cs, PDFont fn, PDFont fb, float yTop, float pw,
            String numero, String dataHora, String codVer, PDImageXObject brasaoImg
    ) throws IOException {
        float h = 86;
        float y0 = yTop - h;
        drawRect(cs, M, y0, CW, h);

        float brasao = 48;
        float boxX = M + 6;
        float boxY = y0 + h - brasao - 8;
        drawRect(cs, boxX, boxY, brasao, brasao);
        if (brasaoImg != null) {
            float pad = 3f;
            float inner = brasao - 2 * pad;
            float iw = brasaoImg.getWidth();
            float ih = brasaoImg.getHeight();
            float scale = Math.min(inner / iw, inner / ih);
            float drawW = iw * scale;
            float drawH = ih * scale;
            float imgX = boxX + (brasao - drawW) / 2f;
            float imgY = boxY + (brasao - drawH) / 2f;
            cs.drawImage(brasaoImg, imgX, imgY, drawW, drawH);
        } else {
            cs.beginText();
            cs.setFont(fn, 7);
            cs.newLineAtOffset(M + 14, boxY + 4);
            cs.showText("[Brasao]");
            cs.endText();
        }

        float tx0 = M + 8 + brasao + 8;
        cs.beginText();
        cs.setFont(fb, 9);
        cs.newLineAtOffset(tx0, yTop - 14);
        cs.showText("PREFEITURA DO MUNIC\u00CDPIO DE S\u00C3O PAULO");
        cs.endText();
        cs.beginText();
        cs.setFont(fb, 8);
        cs.newLineAtOffset(tx0, yTop - 26);
        cs.showText("SECRETARIA MUNICIPAL DE FINAN\u00C7AS");
        cs.endText();
        cs.beginText();
        cs.setFont(fb, 9);
        cs.newLineAtOffset(tx0, yTop - 40);
        cs.showText("NOTA FISCAL DE SERVICOS ELETRONICA - NFS-e");
        cs.endText();
        cs.beginText();
        cs.setFont(fn, 6);
        cs.newLineAtOffset(tx0, yTop - 54);
        cs.showText("Documento gerado em simulacao Contab360 — nao substitui NFS-e oficial.");
        cs.endText();

        float bx = pw - M - 172;
        float bw = 164;
        drawRect(cs, bx, y0 + 8, bw, h - 16);
        float ty = yTop - 18;
        campoRotuloValor(cs, fn, bx + 5, ty, "Numero da Nota:", numero);
        ty -= 11;
        campoRotuloValor(cs, fn, bx + 5, ty, "Data e Hora de Emissao:", dataHora);
        ty -= 11;
        campoRotuloValor(cs, fn, bx + 5, ty, "Codigo de Verificacao:", codVer);

        return y0 - 4;
    }

    private static void campoRotuloValor(PDPageContentStream cs, PDFont fn, float x, float y, String rot, String val)
            throws IOException {
        cs.beginText();
        cs.setFont(fn, 7);
        cs.newLineAtOffset(x, y);
        cs.showText(trunc(rot + " " + val, 44));
        cs.endText();
    }

    private static float faixaTitulo(PDPageContentStream cs, PDFont fb, float y, String titulo) throws IOException {
        float h = 16;
        float y0 = y - h;
        cs.setNonStrokingColor(0.94f, 0.94f, 0.94f);
        cs.addRect(M, y0, CW, h);
        cs.fill();
        cs.setStrokingColor(0, 0, 0);
        drawRect(cs, M, y0, CW, h);
        cs.beginText();
        cs.setNonStrokingColor(0, 0, 0);
        cs.setFont(fb, 8);
        cs.newLineAtOffset(M + 6, y0 + 4);
        cs.showText(titulo);
        cs.endText();
        return y0 - 2;
    }

    /** Ordem: CPF/CNPJ, Insc. Municipal, Raz ao, Endereco, Municipio, UF. */
    private static float corpoPrestador(
            PDPageContentStream cs, PDFont fn, float y,
            String cnpj, String im, String razao, String endereco, String municipio, String uf
    ) throws IOException {
        float h = 98;
        float y0 = y - h;
        drawRect(cs, M, y0, CW, h);
        float ty = y - 12;
        ty = linha(cs, fn, M + 6, ty, "CPF/CNPJ:", cnpj);
        ty = linha(cs, fn, M + 6, ty, "Inscricao Municipal:", im);
        ty = linha(cs, fn, M + 6, ty, "Nome/Razao Social:", razao);
        ty = linha(cs, fn, M + 6, ty, "Endereco:", trunc(endereco, 88));
        ty = linha(cs, fn, M + 6, ty, "Municipio:", municipio);
        ty = linha(cs, fn, M + 6, ty, "UF:", uf);
        return y0 - 6;
    }

    /** Ordem cartao PMSP: Nome, CNPJ, Insc., Endereco, Municipio, UF, E-mail. */
    private static float corpoTomador(
            PDPageContentStream cs, PDFont fn, float y,
            String razao, String cnpj, String im, String endereco, String municipio, String uf, String email
    ) throws IOException {
        float h = 104;
        float y0 = y - h;
        drawRect(cs, M, y0, CW, h);
        float ty = y - 12;
        ty = linha(cs, fn, M + 6, ty, "Nome/Razao Social:", razao);
        ty = linha(cs, fn, M + 6, ty, "CPF/CNPJ:", cnpj);
        ty = linha(cs, fn, M + 6, ty, "Inscricao Municipal:", im);
        ty = linha(cs, fn, M + 6, ty, "Endereco:", trunc(endereco, 88));
        ty = linha(cs, fn, M + 6, ty, "Municipio:", municipio);
        ty = linha(cs, fn, M + 6, ty, "UF:", uf);
        ty = linha(cs, fn, M + 6, ty, "E-mail:", email);
        return y0 - 6;
    }

    private static float linha(PDPageContentStream cs, PDFont fn, float x, float y, String rot, String val) throws IOException {
        cs.beginText();
        cs.setFont(fn, 8);
        cs.newLineAtOffset(x, y);
        cs.showText(trunc(rot + " " + (val == null ? "" : val), 98));
        cs.endText();
        return y - 12;
    }

    private static float areaDiscriminacao(PDPageContentStream cs, PDFont fn, float y, String texto) throws IOException {
        float h = 76;
        float y0 = y - h;
        drawRect(cs, M, y0, CW, h);
        textoMultilinha(cs, fn, M + 8, y - 14, 11, texto);
        return y0 - 6;
    }

    private static void textoMultilinha(PDPageContentStream cs, PDFont fn, float x, float yTopo, float lineH, String texto)
            throws IOException {
        String t = texto == null ? "" : sanitize(texto);
        float y = yTopo;
        for (int start = 0; start < t.length() && y > 80; ) {
            int end = Math.min(t.length(), start + 88);
            String part = t.substring(start, end);
            cs.beginText();
            cs.setFont(fn, 9);
            cs.newLineAtOffset(x, y);
            cs.showText(part);
            cs.endText();
            y -= lineH;
            start = end;
        }
    }

    private static float faixaValorTotal(PDPageContentStream cs, PDFont fn, PDFont fb, float y, BigDecimal total) throws IOException {
        float barH = 24;
        float y0 = y - barH;
        cs.setNonStrokingColor(0.9f, 0.9f, 0.92f);
        cs.addRect(M, y0, CW, barH);
        cs.fill();
        drawRect(cs, M, y0, CW, barH);
        String txt = "VALOR TOTAL DA NOTA = R$ " + brMoney(total);
        float fs = 10;
        float w = fb.getStringWidth(txt) / 1000f * fs;
        float x0 = M + (CW - w) / 2f;
        cs.beginText();
        cs.setNonStrokingColor(0, 0, 0);
        cs.setFont(fb, fs);
        cs.newLineAtOffset(x0, y0 + 7);
        cs.showText(txt);
        cs.endText();
        return y0 - 6;
    }

    private static float linhaCodigoServico(PDPageContentStream cs, PDFont fn, float y, String linha) throws IOException {
        cs.beginText();
        cs.setFont(fn, 8);
        cs.newLineAtOffset(M + 4, y);
        cs.showText(trunc("Codigo do Servico: " + linha, 96));
        cs.endText();
        return y - 14;
    }

    /**
     * Quadro ISS no padrao PMSP: rotulo em duas linhas (6 pt) e valor em destaque (9 pt bold).
     */
    private static float quadroIss(
            PDPageContentStream cs,
            PDFont fn,
            PDFont fb,
            float y,
            BigDecimal ded,
            BigDecimal base,
            BigDecimal aliq,
            BigDecimal iss,
            BigDecimal creditoIptu
    ) throws IOException {
        float h = 56;
        float y0 = y - h;
        drawRect(cs, M, y0, CW, h);
        float[] cols = {0.22f, 0.20f, 0.17f, 0.20f, 0.21f};
        float x = M;
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                cs.moveTo(x, y0);
                cs.lineTo(x, y0 + h);
                cs.stroke();
            }
            x += CW * cols[i];
        }
        float pad = 4f;
        float yLabel1 = y - 12;
        float yLabel2 = y - 20;
        float yValor = y - 38;
        float fsLabel = 6f;
        float fsValor = 9f;

        float x0 = M;
        celulaIss(cs, fn, fb, x0 + pad, cols[0] * CW - 2 * pad, yLabel1, yLabel2, yValor, fsLabel, fsValor,
                "Valor Total das", "Deduc\u00E7\u00F5es (R$):", brMoney(ded));
        x0 += CW * cols[0];
        celulaIss(cs, fn, fb, x0 + pad, cols[1] * CW - 2 * pad, yLabel1, yLabel2, yValor, fsLabel, fsValor,
                "Base de C\u00E1lculo", "(R$):", brMoney(base));
        x0 += CW * cols[1];
        celulaIss(cs, fn, fb, x0 + pad, cols[2] * CW - 2 * pad, yLabel1, yLabel2, yValor, fsLabel, fsValor,
                "Al\u00EDquota", "(%):", brMoney(aliq) + "%");
        x0 += CW * cols[2];
        celulaIss(cs, fn, fb, x0 + pad, cols[3] * CW - 2 * pad, yLabel1, yLabel2, yValor, fsLabel, fsValor,
                "Valor do ISS", "(R$):", brMoney(iss));
        x0 += CW * cols[3];
        celulaIss(cs, fn, fb, x0 + pad, cols[4] * CW - 2 * pad, yLabel1, yLabel2, yValor, fsLabel, fsValor,
                "Cr\u00E9dito p/", "Abatimento IPTU:", brMoney(creditoIptu));

        return y0 - 4;
    }

    private static void celulaIss(
            PDPageContentStream cs,
            PDFont fn,
            PDFont fb,
            float x,
            float colW,
            float yL1,
            float yL2,
            float yVal,
            float fsLab,
            float fsVal,
            String linhaRotulo1,
            String linhaRotulo2,
            String valor
    ) throws IOException {
        int maxLab = Math.max(10, (int) (colW / 2.6f));
        int maxVal = Math.max(8, (int) (colW / 3.2f));
        cs.beginText();
        cs.setFont(fn, fsLab);
        cs.newLineAtOffset(x, yL1);
        cs.showText(trunc(linhaRotulo1, maxLab));
        cs.endText();
        cs.beginText();
        cs.setFont(fn, fsLab);
        cs.newLineAtOffset(x, yL2);
        cs.showText(trunc(linhaRotulo2, maxLab));
        cs.endText();
        cs.beginText();
        cs.setFont(fb, fsVal);
        cs.newLineAtOffset(x, yVal);
        cs.showText(trunc(valor, maxVal));
        cs.endText();
    }

    private static float rodapeCartao(PDPageContentStream cs, PDFont fn, float y, LocalDate vencIss) throws IOException {
        float h = 42;
        float y0 = y - h;
        drawRect(cs, M, y0, CW, h);
        float ty = y - 11;
        cs.beginText();
        cs.setFont(fn, 7);
        cs.newLineAtOffset(M + 6, ty);
        cs.showText("Esta NFS-e foi emitida com respaldo na Lei n\u00BA 14.097/2005.");
        cs.endText();
        ty -= 10;
        cs.beginText();
        cs.setFont(fn, 7);
        cs.newLineAtOffset(M + 6, ty);
        cs.showText("Esta NFS-e n\u00E3o gera cr\u00E9dito.");
        cs.endText();
        ty -= 10;
        cs.beginText();
        cs.setFont(fn, 7);
        cs.newLineAtOffset(M + 6, ty);
        cs.showText("Data de vencimento do ISS desta NFS-e: " + vencIss.format(DV));
        cs.endText();
        return y0;
    }

    private static String brMoney(BigDecimal v) {
        if (v == null) {
            return "0,00";
        }
        return String.format(Locale.ROOT, "%.2f", v).replace('.', ',');
    }

    private static void drawRect(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(0, 0, 0);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static String formatarCnpj(String c14) {
        if (c14 == null || c14.length() != 14) {
            return c14 == null ? "" : c14;
        }
        return c14.substring(0, 2) + "." + c14.substring(2, 5) + "." + c14.substring(5, 8) + "/"
                + c14.substring(8, 12) + "-" + c14.substring(12);
    }

    private static String codigoVerificacaoSimulado(Long id) {
        String alfa = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random(id == null ? 1L : id * 7919L);
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            a.append(alfa.charAt(r.nextInt(alfa.length())));
            b.append(alfa.charAt(r.nextInt(alfa.length())));
        }
        return a + "-" + b;
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c >= 32 && c <= 255) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String coalesceNull(String a, String b) {
        return a == null || a.isBlank() ? b : a;
    }

    private static String coalesceEmpty(String a, String b) {
        return a == null || a.isBlank() ? b : a;
    }
}
