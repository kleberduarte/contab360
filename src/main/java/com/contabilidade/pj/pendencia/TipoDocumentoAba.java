package com.contabilidade.pj.pendencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agrupa os codigos detectados pela IA em abas fixas da interface (portal do cliente).
 */
public final class TipoDocumentoAba {

    private static final Map<String, String> CODIGO_PARA_ABA = Map.ofEntries(
            Map.entry("NOTA_FISCAL", "NOTA_FISCAL"),
            Map.entry("NFE_XML", "NOTA_FISCAL"),
            Map.entry("NFSE_XML", "NOTA_FISCAL"),
            Map.entry("FOLHA_PAGAMENTO", "FOLHA_PAGAMENTO"),
            Map.entry("EXTRATO_BANCARIO", "EXTRATO_BANCARIO"),
            Map.entry("RECIBO_DESPESA", "RECIBO_DESPESA"),
            Map.entry("GUIA_IMPOSTO", "GUIA_IMPOSTO"),
            Map.entry("CONTRATO_SOCIAL", "CONTRATO_SOCIAL"),
            Map.entry("ATA_REUNIAO", "ATA_REUNIAO"),
            Map.entry("DECLARACAO_ACESSORIA", "DECLARACAO_ACESSORIA"),
            Map.entry("EMPRESTIMO_FINANCIAMENTO", "EMPRESTIMO_FINANCIAMENTO"),
            Map.entry("FLUXO_CAIXA", "FLUXO_CAIXA"),
            Map.entry("DESCONHECIDO", "OUTROS")
    );

    private static final Map<String, String> TITULO_ABA = new LinkedHashMap<>();

    static {
        TITULO_ABA.put("NOTA_FISCAL", "Nota fiscal");
        TITULO_ABA.put("FOLHA_PAGAMENTO", "Holerite / Folha");
        TITULO_ABA.put("EXTRATO_BANCARIO", "Extrato bancário");
        TITULO_ABA.put("RECIBO_DESPESA", "Recibo / Despesa");
        TITULO_ABA.put("GUIA_IMPOSTO", "Guia de imposto");
        TITULO_ABA.put("CONTRATO_SOCIAL", "Contrato social");
        TITULO_ABA.put("ATA_REUNIAO", "Ata de reunião");
        TITULO_ABA.put("DECLARACAO_ACESSORIA", "Declaração acessória");
        TITULO_ABA.put("EMPRESTIMO_FINANCIAMENTO", "Empréstimo / Financiamento");
        TITULO_ABA.put("FLUXO_CAIXA", "Fluxo de caixa");
        TITULO_ABA.put("OUTROS", "Outros");
    }

    private TipoDocumentoAba() {
    }

    public static String idAbaParaTipoDetectado(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return "OUTROS";
        }
        return CODIGO_PARA_ABA.getOrDefault(tipoDocumento.trim().toUpperCase(Locale.ROOT), "OUTROS");
    }

    public static String tituloAba(String idAba) {
        return TITULO_ABA.getOrDefault(idAba, "Outros");
    }

    /** Ordem fixa das abas na UI (todas aparecem, mesmo vazias). */
    public static List<String> ordemAbas() {
        return new ArrayList<>(TITULO_ABA.keySet());
    }
}
