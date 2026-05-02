package com.contabilidade.pj.pendencia.entity;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TipoDocumentoCatalogo {

    /** Guias de tributos federais (arrecadação RFB / DARF e afins). */
    public static final Set<String> TIPOS_GUIA_RECEITA_FEDERAL = Set.of(
            "GUIA_IMPOSTO",
            "GUIA_IRPF",
            "GUIA_IRPJ",
            "GUIA_CSLL",
            "GUIA_PIS",
            "GUIA_COFINS",
            "GUIA_IPI",
            "GUIA_IOF",
            "GUIA_IRRF",
            "GUIA_CIDE",
            "GUIA_II",
            "GUIA_ITR",
            "GUIA_FUNRURAL"
    );

    private static final Map<String, List<String>> CAMPOS_OBRIGATORIOS = Map.ofEntries(
            Map.entry("NOTA_FISCAL", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("NFE_XML", List.of("cnpjEmitente", "valor", "tipoDocumento")),
            Map.entry("NFSE_XML", List.of("cnpjEmitente", "valor", "tipoDocumento")),
            Map.entry("NFE", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("NFSE", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("NFCE", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("CTE", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("MDFE", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("CTE_OS", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("EXTRATO_BANCARIO", List.of("cnpj", "tipoDocumento")),
            Map.entry("RECIBO_DESPESA", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("GUIA_IMPOSTO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("FOLHA_PAGAMENTO", List.of("cnpj", "tipoDocumento")),
            Map.entry("CONTRATO_SOCIAL", List.of("cnpj", "tipoDocumento")),
            Map.entry("ATA_REUNIAO", List.of("tipoDocumento")),
            Map.entry("DECLARACAO_ACESSORIA", List.of("cnpj", "tipoDocumento")),
            Map.entry("SPED_FISCAL_CONTRIBUICOES", List.of("cnpj", "tipoDocumento")),
            Map.entry("SPED_CONTABIL_FISCAL", List.of("cnpj", "tipoDocumento")),
            Map.entry("BALANCETES_DEMONSTRACOES", List.of("cnpj", "tipoDocumento")),
            Map.entry("CONCILIACAO_FINANCEIRA", List.of("cnpj", "tipoDocumento")),
            Map.entry("CONTAS_PAGAR_RECEBER", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("CERTIDOES_LICENCAS", List.of("cnpj", "tipoDocumento")),
            Map.entry("PROCESSOS_FISCAIS_JURIDICOS", List.of("cnpj", "tipoDocumento")),
            Map.entry("PATRIMONIO_IMOBILIZADO", List.of("cnpj", "tipoDocumento")),
            Map.entry("ESTOQUE_CUSTO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("COMERCIO_EXTERIOR_CAMBIO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("CERTIFICADO_DIGITAL", List.of("cnpj", "tipoDocumento")),
            Map.entry("EMPRESTIMO_FINANCIAMENTO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("SEGUROS_APOLICES", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("RELATORIOS_GERENCIAIS", List.of("cnpj", "tipoDocumento")),
            Map.entry("FLUXO_CAIXA", List.of("cnpj", "tipoDocumento")),
            Map.entry("DESCONHECIDO", List.of("tipoDocumento"))
    );

    private TipoDocumentoCatalogo() {
    }

    public static boolean ehGuiaReceitaFederal(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return false;
        }
        return TIPOS_GUIA_RECEITA_FEDERAL.contains(tipoDocumento.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Ajusta o tipo genérico de guia (IRPF) para o tributo federal indicado no texto.
     * Tipos fora de guias federais são devolvidos sem alteração; {@code GUIA_IRPJ} pode ser
     * corrigido para {@code GUIA_IRPF} quando o texto é claramente comprovante/informe de PF.
     */
    public static String refinarGuiaReceitaFederal(String texto, String tipoAtual) {
        if (tipoAtual == null || tipoAtual.isBlank()) {
            return tipoAtual;
        }
        String tipo = tipoAtual.trim().toUpperCase(Locale.ROOT);
        String u = texto == null ? "" : texto.toUpperCase(Locale.ROOT);
        if ("GUIA_IRPJ".equals(tipo) && indiciosInformeIrpf(u)) {
            return "GUIA_IRPF";
        }
        if (!"GUIA_IRPF".equals(tipo) && !"GUIA_IMPOSTO".equals(tipo)) {
            return tipoAtual;
        }
        if (u.contains("FGTS") && !u.contains("DARF")) {
            return "OUTROS";
        }
        if (u.contains("GPS") && (u.contains("INSS") || u.contains("PREVID"))) {
            return "OUTROS";
        }
        if (palavra(u, "FUNRURAL") || u.contains("FUNREA")) {
            return "GUIA_FUNRURAL";
        }
        if (palavra(u, "ITR") || u.contains("IMPOSTO TERRITORIAL RURAL")) {
            return "GUIA_ITR";
        }
        if (u.contains("IMPOSTO DE IMPORTA") || u.contains("IMPOSTO DE IMPORTACAO")
                || (palavra(u, "II") && (u.contains("DI ") || u.contains("DUIMP") || u.contains("ADUANEIR")))) {
            return "GUIA_II";
        }
        /* "CIDE" como substring casa em "INCIDÊNCIA"/"INCIDENCIA" — usar limite de palavra. */
        if (palavra(u, "CIDE")) {
            return "GUIA_CIDE";
        }
        if (palavra(u, "IOF") || u.contains("IMPOSTO SOBRE OPERAÇÕES FINANCEIRAS")
                || u.contains("IMPOSTO SOBRE OPERACOES FINANCEIRAS")) {
            return "GUIA_IOF";
        }
        if (palavra(u, "IPI") || u.contains("IMPOSTO SOBRE PRODUTOS INDUSTRIALIZADOS")) {
            return "GUIA_IPI";
        }
        /*
         * IRPF antes de IRPJ: comprovantes/informes DIRF citam "fonte pagadora pessoa jurídica" e "imposto de renda",
         * o que não é DARF de PJ (regimes de lucro continuam indicando IRPJ depois).
         */
        if (indiciosInformeIrpf(u)) {
            return "GUIA_IRPF";
        }
        if (palavra(u, "IRPJ")
                || ((u.contains("PESSOA JURÍDICA") || u.contains("PESSOA JURIDICA")) && u.contains("IMPOSTO DE RENDA"))
                || u.contains("LUCRO PRESUMIDO") || u.contains("LUCRO REAL") || u.contains("LUCRO ARBITRADO")) {
            return "GUIA_IRPJ";
        }
        if (palavra(u, "CSLL") || u.contains("CONTRIBUIÇÃO SOCIAL SOBRE O LUCRO")
                || u.contains("CONTRIBUICAO SOCIAL SOBRE O LUCRO")) {
            return "GUIA_CSLL";
        }
        if (palavra(u, "COFINS") || u.contains("CONTRIBUIÇÃO PARA O FINANCIAMENTO")
                || u.contains("CONTRIBUICAO PARA O FINANCIAMENTO")) {
            return "GUIA_COFINS";
        }
        if (palavra(u, "PIS") || u.contains("PIS/PASEP") || u.contains("CONTRIBUIÇÃO PARA O PIS")
                || u.contains("CONTRIBUICAO PARA O PIS")) {
            return "GUIA_PIS";
        }
        if (palavra(u, "IRRF") || u.contains("IMPOSTO SOBRE A RENDA RETIDO NA FONTE")
                || u.contains("RETIDO NA FONTE")) {
            return "GUIA_IRRF";
        }
        return "GUIA_IRPF";
    }

    private static boolean palavra(String textoUpper, String palavra) {
        return Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b").matcher(textoUpper).find();
    }

    /** Comprovante DIRF, informe de rendimentos, etc. — não confundir com DARF IRPJ. */
    private static boolean indiciosInformeIrpf(String u) {
        return palavra(u, "IRPF")
                || u.contains("COMPROVANTE DE RENDIMENTOS")
                || u.contains("FONTE PAGADORA")
                || u.contains("INFORME DE RENDIMENTOS") || u.contains("INFORME DE RENDIMENTO")
                || u.contains("INFORMAÇÃO DE RENDIMENTOS") || u.contains("INFORMACAO DE RENDIMENTOS")
                || u.contains("DIRF")
                || u.contains("ANO-CALENDÁRIO") || u.contains("ANO-CALENDARIO")
                || u.contains("RENDIMENTOS TRIBUTÁVEIS") || u.contains("RENDIMENTOS TRIBUTAVEIS")
                || (u.contains("RETIDO NA FONTE")
                        && (u.contains("PESSOA JURÍDICA") || u.contains("PESSOA JURIDICA")))
                || ((u.contains("PESSOA FÍSICA") || u.contains("PESSOA FISICA")) && u.contains("IMPOSTO DE RENDA"))
                || u.contains("CARNÊ-LEÃO") || u.contains("CARNE-LEAO") || u.contains("DECLARAÇÃO DE AJUSTE")
                || u.contains("DECLARACAO DE AJUSTE");
    }

    public static String detectarPorTexto(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        // Comprovante anual de IRRF (DIRF/IRPF): pode mencionar "contra-cheque" em campos complementares
        // e ser confundido com holerite. Prioriza essa identificação antes das regras de folha.
        if (base.contains("comprovante de rendimentos pagos")
                || base.contains("comprovante de rendimentos")
                || base.contains("fonte pagadora pessoa jurídica")
                || base.contains("fonte pagadora")
                || base.contains("ano-calendário")) {
            return "GUIA_IRPF";
        }
        if (base.contains("imposto sobre a renda retido na fonte")) {
            return "GUIA_IRRF";
        }
        if (base.contains("nfc-e") || base.contains("nfce") || base.contains("nota fiscal do consumidor")) {
            return "NFCE";
        }
        if (base.contains("ct-e os") || base.contains("cte os") || base.contains("cteos")) {
            return "CTE_OS";
        }
        if (base.contains("mdf-e") || base.contains("mdfe")) {
            return "MDFE";
        }
        if (base.contains("ct-e") || base.contains("conhecimento de transporte") || base.contains("cte ")) {
            return "CTE";
        }
        if (base.contains("nfs-e") || base.contains("nfse") || base.contains("nota fiscal")) {
            return "NOTA_FISCAL";
        }
        if (base.contains("balancete") || base.contains("balanço patrimonial") || base.contains("balanco patrimonial")) {
            return "BALANCETES_DEMONSTRACOES";
        }
        if (base.contains("conciliação financeira") || base.contains("conciliacao financeira")) {
            return "CONCILIACAO_FINANCEIRA";
        }
        if (base.contains("contas a pagar") || base.contains("contas a receber") || base.contains("arquivo remessa")
                || base.contains("arquivo retorno")) {
            return "CONTAS_PAGAR_RECEBER";
        }
        if (base.contains("certidão") || base.contains("certidao") || base.contains("alvará de funcionamento")
                || base.contains("alvara de funcionamento") || base.contains("licença de funcionamento")
                || base.contains("licenca de funcionamento")) {
            return "CERTIDOES_LICENCAS";
        }
        if (base.contains("notificação fiscal") || base.contains("notificacao fiscal")
                || base.contains("intimação") || base.contains("intimacao")
                || base.contains("auto de infração") || base.contains("auto de infracao")
                || base.contains("processo administrativo fiscal")) {
            return "PROCESSOS_FISCAIS_JURIDICOS";
        }
        if (base.contains("patrimônio imobilizado") || base.contains("patrimonio imobilizado")
                || base.contains("controle patrimonial")) {
            return "PATRIMONIO_IMOBILIZADO";
        }
        if (base.contains("inventário de estoque") || base.contains("inventario de estoque")
                || base.contains("posição de estoque") || base.contains("posicao de estoque")) {
            return "ESTOQUE_CUSTO";
        }
        if (base.contains("declaração de importação") || base.contains("declaracao de importacao")
                || base.contains("due ") || base.contains("invoice comercial")) {
            return "COMERCIO_EXTERIOR_CAMBIO";
        }
        if (base.contains("certificado digital a1") || base.contains("certificado digital a3")
                || base.contains("e-cnpj")) {
            return "CERTIFICADO_DIGITAL";
        }
        if (base.contains("apólice de seguro") || base.contains("apolice de seguro")) {
            return "SEGUROS_APOLICES";
        }
        if (base.contains("relatório gerencial") || base.contains("relatorio gerencial")
                || base.contains("indicadores kpi") || base.contains("dashboard gerencial")) {
            return "RELATORIOS_GERENCIAIS";
        }
        if (base.contains("extrato")) {
            return "EXTRATO_BANCARIO";
        }
        /* Folha / holerite antes de "recibo", pois contracheque traz "recibo de pagamento de salário". */
        if (base.contains("holerite")
                || base.contains("contra-cheque")
                || base.contains("contra cheque")
                || base.contains("folha de pagamento")
                || base.contains("demonstrativo de pagamento")
                || base.contains("pro-labore")
                || base.contains("pro labore")
                || base.contains("recibo de pagamento")) {
            return "FOLHA_PAGAMENTO";
        }
        if (base.contains("recibo") || base.contains("comprovante de despesa")) {
            return "RECIBO_DESPESA";
        }
        if (base.contains("iss") || base.contains("icms")) {
            return "DESCONHECIDO";
        }
        if (base.contains("pis") || base.contains("cofins") || base.contains("irpj") || base.contains("csll")) {
            return refinarGuiaReceitaFederal(texto, "GUIA_IRPF");
        }
        if (base.contains("contrato social") || base.contains("alteração societária")
                || base.contains("alteracao societaria") || base.contains("quadro societário")
                || base.contains("quadro societario")) {
            return "CONTRATO_SOCIAL";
        }
        if (base.contains("ata de reuniao") || base.contains("ata assembleia")) {
            return "ATA_REUNIAO";
        }
        if (base.contains("ecd") || base.contains("ecf")) {
            return "SPED_CONTABIL_FISCAL";
        }
        if ((base.contains("sped fiscal") || base.contains("efd-contribuições") || base.contains("efd-contribuicoes"))
                && (base.contains("contrib") || base.contains("efd"))) {
            return "SPED_FISCAL_CONTRIBUICOES";
        }
        if (base.contains("efd contribuições") || base.contains("efd contribuicoes")) {
            return "SPED_FISCAL_CONTRIBUICOES";
        }
        if (base.contains("sped") || base.contains("dctf") || base.contains("efd-reinf")) {
            return "DECLARACAO_ACESSORIA";
        }
        if (base.contains("emprestimo") || base.contains("financiamento")) {
            return "EMPRESTIMO_FINANCIAMENTO";
        }
        if (base.contains("fluxo de caixa") || base.contains("controle de caixa")) {
            return "FLUXO_CAIXA";
        }
        return "DESCONHECIDO";
    }

    public static String detectarPorXml(boolean isNfe, boolean isNfse) {
        if (isNfe) {
            return "NFE_XML";
        }
        if (isNfse) {
            return "NFSE_XML";
        }
        return "NOTA_FISCAL";
    }

    public static List<String> camposObrigatorios(String tipoDocumento) {
        if (tipoDocumento != null && ehGuiaReceitaFederal(tipoDocumento.trim().toUpperCase(Locale.ROOT))) {
            return CAMPOS_OBRIGATORIOS.get("GUIA_IMPOSTO");
        }
        return CAMPOS_OBRIGATORIOS.getOrDefault(tipoDocumento, CAMPOS_OBRIGATORIOS.get("DESCONHECIDO"));
    }
}
