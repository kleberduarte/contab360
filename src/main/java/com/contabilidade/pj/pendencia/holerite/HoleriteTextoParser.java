package com.contabilidade.pj.pendencia.holerite;

import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleriteBases;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleriteEmpresa;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleriteFuncionario;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleritePeriodo;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleriteRubrica;
import com.contabilidade.pj.pendencia.holerite.HoleriteDetalhado.HoleriteTotais;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta holerite/contracheque em texto simples (exportação, OCR, TXT) e preenche
 * {@link HoleriteDetalhado}. Layout esperado: blocos EMPREGADOR/EMPREGADO, tabelas PROVENTOS/DESCONTOS.
 */
public final class HoleriteTextoParser {

    private static final Pattern RAZAO = Pattern.compile("(?i)Razão Social:\\s*(.+?)(?:\\r?\\n|$)");
    /** PDFs às vezes perdem acento em "Razão". */
    private static final Pattern RAZAO_SEM_ACENTO = Pattern.compile("(?i)Razao\\s+Social:\\s*(.+?)(?:\\r?\\n|$)");
    private static final Pattern CNPJ_LINHA = Pattern.compile("(?i)CNPJ:\\s*(.+?)(?:\\r?\\n|$)");
    private static final Pattern CNPJ_FORMATADO = Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");
    private static final Pattern ENDERECO = Pattern.compile("(?i)Endereço:\\s*(.+?)(?:\\n|$)");
    private static final Pattern NOME_FUNC = Pattern.compile("(?i)Nome:\\s*(.+?)(?:\\r?\\n|$)");
    private static final Pattern CPF_FUNC = Pattern.compile("(?i)CPF:\\s*(.+?)(?:\\r?\\n|$)");
    private static final Pattern PIS = Pattern.compile("(?i)PIS/PASEP:\\s*(.+?)(?:\\n|$)");
    private static final Pattern CARGO = Pattern.compile("(?i)Cargo:\\s*(.+?)(?:\\n|$)");
    private static final Pattern DEPT = Pattern.compile("(?i)Departamento:\\s*(.+?)(?:\\n|$)");
    private static final Pattern ADMISSAO = Pattern.compile("(?i)Admissão:\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern COMPETENCIA = Pattern.compile("(?i)COMPETÊNCIA:\\s*(\\d{2}/\\d{4})");
    private static final Pattern TIPO_FOLHA = Pattern.compile("(?i)Tipo de folha:\\s*(.+?)(?:\\n|$)");
    private static final Pattern DIAS = Pattern.compile("(?i)Dias trabalhados:\\s*(\\d+)");
    private static final Pattern TOTAL_PROV = Pattern.compile("(?i)TOTAL DE PROVENTOS\\s+([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})");
    private static final Pattern TOTAL_DESC = Pattern.compile("(?i)TOTAL DE DESCONTOS\\s+([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})");
    private static final Pattern VALOR_LIQUIDO_LINHA = Pattern.compile(
            "(?i)VALOR\\s+L[IÍ]QUIDO\\s+A\\s+RECEBER:\\s*R\\$\\s*([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})");
    private static final Pattern LINHA_VALOR_FINAL = Pattern.compile("^.+?([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})\\s*$");
    private static final Pattern FERIAS = Pattern.compile("(?i)Férias vencidas em:\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern DATA_DOC = Pattern.compile("(?i)^Data:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*$", Pattern.MULTILINE);

    private HoleriteTextoParser() {
    }

    public static HoleriteDetalhado parse(String texto) {
        if (texto == null || texto.isBlank()) {
            return vazio();
        }
        String t = texto.replace('\u00A0', ' ');
        if (!pareceHolerite(t)) {
            return vazio();
        }

        HoleriteEmpresa empresa = parseEmpresa(t);
        HoleriteFuncionario func = parseFuncionario(t);
        HoleritePeriodo periodo = parsePeriodo(t);
        List<HoleriteRubrica> proventos = parseSecaoRubricas(t, "PROVENTOS", "TOTAL DE PROVENTOS");
        List<HoleriteRubrica> descontos = parseSecaoRubricas(t, "DESCONTOS", "TOTAL DE DESCONTOS");
        HoleriteTotais totais = parseTotais(t);
        HoleriteBases bases = parseBases(t);
        Map<String, String> extras = new LinkedHashMap<>();
        Matcher mf = FERIAS.matcher(t);
        if (mf.find()) {
            extras.put("feriasVencimento", mf.group(1));
        }
        Matcher md = DATA_DOC.matcher(t);
        if (md.find()) {
            extras.put("dataDocumento", md.group(1));
        }

        return new HoleriteDetalhado(empresa, func, periodo, proventos, descontos, totais, bases, extras);
    }

    private static boolean pareceHolerite(String t) {
        String lower = t.toLowerCase(Locale.ROOT);
        return lower.contains("holerite")
                || lower.contains("contra-cheque")
                || lower.contains("recibo de pagamento")
                || lower.contains("folha de pagamento")
                || (lower.contains("proventos") && lower.contains("descontos"));
    }

    private static HoleriteDetalhado vazio() {
        return new HoleriteDetalhado(
                new HoleriteEmpresa("", "", "", ""),
                new HoleriteFuncionario("", "", "", "", "", ""),
                new HoleritePeriodo("", "", ""),
                List.of(),
                List.of(),
                new HoleriteTotais("", "", "", "", "", ""),
                new HoleriteBases("", "", "", "", "", "", "", ""),
                Map.of()
        );
    }

    private static HoleriteEmpresa parseEmpresa(String t) {
        String raz = buscar(RAZAO, t, 1);
        if (raz.isBlank()) {
            raz = buscar(RAZAO_SEM_ACENTO, t, 1);
        }
        String cnpjFmt = buscar(CNPJ_LINHA, t, 1).replaceAll("\\s+", " ").trim();
        String end = buscar(ENDERECO, t, 1).trim();
        String digitos = cnpjFmt.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            digitos = "";
            String trecho = t.length() > 4000 ? t.substring(0, 4000) : t;
            Matcher mc = CNPJ_FORMATADO.matcher(trecho);
            if (mc.find()) {
                cnpjFmt = mc.group().trim();
                digitos = cnpjFmt.replaceAll("\\D", "");
            }
        }
        return new HoleriteEmpresa(raz.trim(), cnpjFmt, digitos, end);
    }

    private static HoleriteFuncionario parseFuncionario(String t) {
        return new HoleriteFuncionario(
                buscar(NOME_FUNC, t, 1).trim(),
                buscar(CPF_FUNC, t, 1).trim(),
                buscar(PIS, t, 1).trim(),
                buscar(CARGO, t, 1).trim(),
                buscar(DEPT, t, 1).trim(),
                buscar(ADMISSAO, t, 1).trim()
        );
    }

    private static HoleritePeriodo parsePeriodo(String t) {
        String comp = buscar(COMPETENCIA, t, 1).trim();
        String tipo = buscar(TIPO_FOLHA, t, 1).trim();
        String dias = buscar(DIAS, t, 1).trim();
        return new HoleritePeriodo(comp, tipo, dias);
    }

    private static HoleriteTotais parseTotais(String t) {
        String tpBr = buscar(TOTAL_PROV, t, 1).trim();
        String tdBr = buscar(TOTAL_DESC, t, 1).trim();
        Matcher ml = VALOR_LIQUIDO_LINHA.matcher(t);
        String liqBr = "";
        if (ml.find()) {
            liqBr = ml.group(1).trim();
        }
        return new HoleriteTotais(
                tpBr, moedaParaNum(tpBr),
                tdBr, moedaParaNum(tdBr),
                liqBr, moedaParaNum(liqBr)
        );
    }

    private static HoleriteBases parseBases(String t) {
        String sal = buscarRs("Salário base \\(eSocial\\)", t);
        String bFgts = buscarRs("Base cálculo FGTS", t);
        String fgtsMes = buscarRsFgtsMes(t);
        String bInss = buscarRs("Base cálculo INSS", t);
        return new HoleriteBases(
                sal, moedaParaNum(sal),
                bFgts, moedaParaNum(bFgts),
                fgtsMes, moedaParaNum(fgtsMes),
                bInss, moedaParaNum(bInss)
        );
    }

    /** Ex.: {@code FGTS do mês (8%): R$ 312,30} — o sufixo {@code (8%)} quebra o padrão fixo "mês:". */
    private static String buscarRsFgtsMes(String texto) {
        Pattern p = Pattern.compile(
                "(?i)FGTS\\s+do\\s+mês(?:\\s*\\([^)]*\\))?\\s*:\\s*R\\$\\s*([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})");
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1).trim() : "";
    }

    private static String buscarRs(String rotulo, String texto) {
        Pattern p = Pattern.compile("(?i)" + rotulo + ":\\s*R\\$\\s*([\\d]{1,3}(?:\\.[\\d]{3})*,\\d{2})");
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1).trim() : "";
    }

    private static List<HoleriteRubrica> parseSecaoRubricas(String texto, String titulo, String totalLinha) {
        List<HoleriteRubrica> lista = new ArrayList<>();
        int i0 = indexOfIgnoreCase(texto, titulo);
        if (i0 < 0) {
            return lista;
        }
        int i1 = indexOfIgnoreCase(texto, totalLinha, i0 + titulo.length());
        if (i1 < 0) {
            return lista;
        }
        String trecho = texto.substring(i0, i1);
        for (String raw : trecho.split("\\R")) {
            String linha = raw.trim();
            if (linha.isEmpty() || linha.startsWith("---") || linha.toLowerCase(Locale.ROOT).contains(titulo.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (linha.toLowerCase(Locale.ROOT).contains("descrição") && linha.toLowerCase(Locale.ROOT).contains("valor")) {
                continue;
            }
            Matcher mv = LINHA_VALOR_FINAL.matcher(linha);
            if (!mv.find()) {
                continue;
            }
            String valorBr = mv.group(1);
            String prefixo = linha.substring(0, linha.length() - valorBr.length()).trim();
            String desc;
            String ref = "";
            String[] partes = prefixo.split("\\s{2,}");
            if (partes.length >= 2) {
                desc = partes[0].trim();
                ref = partes[1].trim();
            } else {
                desc = prefixo;
            }
            if (desc.isEmpty()) {
                continue;
            }
            lista.add(new HoleriteRubrica(desc, ref, valorBr, moedaParaNum(valorBr)));
        }
        return lista;
    }

    private static String buscar(Pattern p, String t, int grupo) {
        Matcher m = p.matcher(t);
        return m.find() ? m.group(grupo) : "";
    }

    private static int indexOfIgnoreCase(String hay, String needle) {
        return indexOfIgnoreCase(hay, needle, 0);
    }

    private static int indexOfIgnoreCase(String hay, String needle, int from) {
        return hay.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), from);
    }

    private static String moedaParaNum(String br) {
        if (br == null || br.isBlank() || !br.contains(",")) {
            return br == null ? "" : br.trim();
        }
        int v = br.lastIndexOf(',');
        String esq = br.substring(0, v).replace(".", "");
        String dec = br.substring(v + 1);
        return esq + "." + dec;
    }
}
