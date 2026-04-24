package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TipoDocumentoCatalogo {

    private static final Map<String, List<String>> CAMPOS_OBRIGATORIOS = Map.ofEntries(
            Map.entry("NOTA_FISCAL", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("NFE_XML", List.of("cnpjEmitente", "valor", "tipoDocumento")),
            Map.entry("NFSE_XML", List.of("cnpjEmitente", "valor", "tipoDocumento")),
            Map.entry("EXTRATO_BANCARIO", List.of("cnpj", "tipoDocumento")),
            Map.entry("RECIBO_DESPESA", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("GUIA_IMPOSTO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("FOLHA_PAGAMENTO", List.of("cnpj", "tipoDocumento")),
            Map.entry("CONTRATO_SOCIAL", List.of("cnpj", "tipoDocumento")),
            Map.entry("ATA_REUNIAO", List.of("tipoDocumento")),
            Map.entry("DECLARACAO_ACESSORIA", List.of("cnpj", "tipoDocumento")),
            Map.entry("EMPRESTIMO_FINANCIAMENTO", List.of("cnpj", "valor", "tipoDocumento")),
            Map.entry("FLUXO_CAIXA", List.of("cnpj", "tipoDocumento")),
            Map.entry("DESCONHECIDO", List.of("tipoDocumento"))
    );

    private TipoDocumentoCatalogo() {
    }

    public static String detectarPorTexto(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        if (base.contains("nfs-e") || base.contains("nfse") || base.contains("nota fiscal")) {
            return "NOTA_FISCAL";
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
        if (base.contains("iss") || base.contains("icms") || base.contains("pis")
                || base.contains("cofins") || base.contains("irpj") || base.contains("csll")) {
            return "GUIA_IMPOSTO";
        }
        if (base.contains("contrato social") || base.contains("alteracao societaria")) {
            return "CONTRATO_SOCIAL";
        }
        if (base.contains("ata de reuniao") || base.contains("ata assembleia")) {
            return "ATA_REUNIAO";
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
        return CAMPOS_OBRIGATORIOS.getOrDefault(tipoDocumento, CAMPOS_OBRIGATORIOS.get("DESCONHECIDO"));
    }
}
