package com.contabilidade.pj.pendencia.entity;

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
            Map.entry("GUIA_IMPOSTO", "GUIA_IRPF"),
            Map.entry("GUIA_IRPF", "GUIA_IRPF"),
            Map.entry("GUIA_IRPJ", "GUIA_IRPJ"),
            Map.entry("GUIA_CSLL", "GUIA_CSLL"),
            Map.entry("GUIA_PIS", "GUIA_PIS"),
            Map.entry("GUIA_COFINS", "GUIA_COFINS"),
            Map.entry("GUIA_IPI", "GUIA_IPI"),
            Map.entry("GUIA_IOF", "GUIA_IOF"),
            Map.entry("GUIA_IRRF", "GUIA_IRRF"),
            Map.entry("GUIA_CIDE", "GUIA_CIDE"),
            Map.entry("GUIA_II", "GUIA_II"),
            Map.entry("GUIA_ITR", "GUIA_ITR"),
            Map.entry("GUIA_FUNRURAL", "GUIA_FUNRURAL"),
            Map.entry("DECLARACAO_OBRIGACAO", "DECLARACAO_OBRIGACAO"),
            Map.entry("DECLARACAO_ACESSORIA", "DECLARACAO_OBRIGACAO"),
            Map.entry("SPED_FISCAL_CONTRIBUICOES", "SPED_FISCAL_CONTRIBUICOES"),
            Map.entry("SPED_CONTABIL_FISCAL", "SPED_CONTABIL_FISCAL"),
            Map.entry("BALANCETES_DEMONSTRACOES", "BALANCETES_DEMONSTRACOES"),
            Map.entry("EXTRATO_BANCARIO", "EXTRATO_BANCARIO"),
            Map.entry("CONCILIACAO_FINANCEIRA", "CONCILIACAO_FINANCEIRA"),
            Map.entry("CONTAS_PAGAR_RECEBER", "CONTAS_PAGAR_RECEBER"),
            Map.entry("RECIBO_DESPESA", "RECIBO_DESPESA"),
            Map.entry("CONTRATOS_SOCIOS", "CONTRATOS_SOCIOS"),
            Map.entry("CONTRATO_SOCIAL", "CONTRATOS_SOCIOS"),
            Map.entry("ATAS_E_REGISTROS", "ATAS_E_REGISTROS"),
            Map.entry("ATA_REUNIAO", "ATAS_E_REGISTROS"),
            Map.entry("CERTIDOES_LICENCAS", "CERTIDOES_LICENCAS"),
            Map.entry("PROCESSOS_FISCAIS_JURIDICOS", "PROCESSOS_FISCAIS_JURIDICOS"),
            Map.entry("PATRIMONIO_IMOBILIZADO", "PATRIMONIO_IMOBILIZADO"),
            Map.entry("ESTOQUE_CUSTO", "ESTOQUE_CUSTO"),
            Map.entry("COMERCIO_EXTERIOR_CAMBIO", "COMERCIO_EXTERIOR_CAMBIO"),
            Map.entry("CERTIFICADO_DIGITAL", "CERTIFICADO_DIGITAL"),
            Map.entry("EMPRESTIMO_FINANCIAMENTO", "EMPRESTIMO_FINANCIAMENTO"),
            Map.entry("SEGUROS_APOLICES", "SEGUROS_APOLICES"),
            Map.entry("RELATORIOS_GERENCIAIS", "RELATORIOS_GERENCIAIS"),
            Map.entry("FLUXO_CAIXA", "FLUXO_CAIXA"),
            Map.entry("DESCONHECIDO", "OUTROS")
    );

    private static final Map<String, String> TITULO_ABA = new LinkedHashMap<>();

    static {
        TITULO_ABA.put("NOTA_FISCAL", "Nota fiscal");
        TITULO_ABA.put("FOLHA_PAGAMENTO", "Holerite / Folha");
        TITULO_ABA.put("GUIA_IRPF", "IRPF");
        TITULO_ABA.put("GUIA_IRPJ", "IRPJ");
        TITULO_ABA.put("GUIA_CSLL", "CSLL");
        TITULO_ABA.put("GUIA_PIS", "PIS / PASEP");
        TITULO_ABA.put("GUIA_COFINS", "COFINS");
        TITULO_ABA.put("GUIA_IPI", "IPI");
        TITULO_ABA.put("GUIA_IOF", "IOF");
        TITULO_ABA.put("GUIA_IRRF", "IRRF");
        TITULO_ABA.put("GUIA_CIDE", "CIDE");
        TITULO_ABA.put("GUIA_II", "II (importação)");
        TITULO_ABA.put("GUIA_ITR", "ITR");
        TITULO_ABA.put("GUIA_FUNRURAL", "FUNRURAL");
        TITULO_ABA.put("DECLARACAO_OBRIGACAO", "Declarações / Obrigações");
        TITULO_ABA.put("SPED_FISCAL_CONTRIBUICOES", "SPED fiscal / contribuições");
        TITULO_ABA.put("SPED_CONTABIL_FISCAL", "SPED contábil / fiscal");
        TITULO_ABA.put("BALANCETES_DEMONSTRACOES", "Balancetes / Demonstrações");
        TITULO_ABA.put("EXTRATO_BANCARIO", "Extrato bancário");
        TITULO_ABA.put("CONCILIACAO_FINANCEIRA", "Conciliação financeira");
        TITULO_ABA.put("CONTAS_PAGAR_RECEBER", "Contas a pagar / receber");
        TITULO_ABA.put("RECIBO_DESPESA", "Recibo / Despesa");
        TITULO_ABA.put("CONTRATOS_SOCIOS", "Contratos / Sócios");
        TITULO_ABA.put("ATAS_E_REGISTROS", "Atas / Registros");
        TITULO_ABA.put("CERTIDOES_LICENCAS", "Certidões / Licenças");
        TITULO_ABA.put("PROCESSOS_FISCAIS_JURIDICOS", "Processos fiscais / jurídicos");
        TITULO_ABA.put("PATRIMONIO_IMOBILIZADO", "Patrimônio / Imobilizado");
        TITULO_ABA.put("ESTOQUE_CUSTO", "Estoque / Custo");
        TITULO_ABA.put("COMERCIO_EXTERIOR_CAMBIO", "Comércio exterior / Câmbio");
        TITULO_ABA.put("CERTIFICADO_DIGITAL", "Certificado digital");
        TITULO_ABA.put("EMPRESTIMO_FINANCIAMENTO", "Empréstimo / Financiamento");
        TITULO_ABA.put("SEGUROS_APOLICES", "Seguros / Apólices");
        TITULO_ABA.put("RELATORIOS_GERENCIAIS", "Relatórios gerenciais");
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

    public static String idAbaParaTemplateNome(String templateNome) {
        if (templateNome == null || templateNome.isBlank()) {
            return "OUTROS";
        }
        String t = templateNome.trim().toLowerCase(Locale.ROOT);
        if (t.contains("nota")) {
            return "NOTA_FISCAL";
        }
        if (t.contains("holerite") || t.contains("folha") || t.contains("contracheque")
                || t.contains("contra-cheque") || t.contains("pro-labore") || t.contains("pro labore")) {
            return "FOLHA_PAGAMENTO";
        }
        if (t.contains("guia") || t.contains("imposto") || t.contains("darf") || t.contains("das")) {
            return "GUIA_IRPF";
        }
        if (t.contains("declar") || t.contains("dctf") || t.contains("reinf")
                || t.contains("esocial") || t.contains("pgdas") || t.contains("defis")
                || t.contains("dirf") || t.contains("dmed") || t.contains("dimob")) {
            return "DECLARACAO_OBRIGACAO";
        }
        if (t.contains("sped") || t.contains("efd")) {
            return "SPED_FISCAL_CONTRIBUICOES";
        }
        if (t.contains("ecd") || t.contains("ecf")) {
            return "SPED_CONTABIL_FISCAL";
        }
        if (t.contains("balancete") || t.contains("dre") || t.contains("bp")) {
            return "BALANCETES_DEMONSTRACOES";
        }
        if (t.contains("extrato")) {
            return "EXTRATO_BANCARIO";
        }
        if (t.contains("concili")) {
            return "CONCILIACAO_FINANCEIRA";
        }
        if (t.contains("contas a pagar") || t.contains("contas a receber")
                || t.contains("boleto") || t.contains("cobranca")
                || t.contains("cobrança") || t.contains("remessa") || t.contains("retorno")) {
            return "CONTAS_PAGAR_RECEBER";
        }
        if (t.contains("recibo") || t.contains("despesa")) {
            return "RECIBO_DESPESA";
        }
        if (t.contains("contrato") || t.contains("socio") || t.contains("sócio")) {
            return "CONTRATOS_SOCIOS";
        }
        if (t.contains("ata") || t.contains("registro") || t.contains("assembleia")) {
            return "ATAS_E_REGISTROS";
        }
        if (t.contains("certidao") || t.contains("certidão") || t.contains("alvara")
                || t.contains("alvará") || t.contains("licenca") || t.contains("licença")) {
            return "CERTIDOES_LICENCAS";
        }
        if (t.contains("auto de infracao") || t.contains("auto de infração") || t.contains("intimacao")
                || t.contains("intimação") || t.contains("notificacao") || t.contains("notificação")
                || t.contains("processo")) {
            return "PROCESSOS_FISCAIS_JURIDICOS";
        }
        if (t.contains("imobilizado") || t.contains("patrimonio") || t.contains("patrimônio")
                || t.contains("depreciacao") || t.contains("depreciação")) {
            return "PATRIMONIO_IMOBILIZADO";
        }
        if (t.contains("estoque") || t.contains("inventario") || t.contains("inventário")
                || t.contains("custo")) {
            return "ESTOQUE_CUSTO";
        }
        if (t.contains("importacao") || t.contains("importação") || t.contains("exportacao")
                || t.contains("exportação") || t.contains("cambio") || t.contains("câmbio")
                || t.contains("invoice")) {
            return "COMERCIO_EXTERIOR_CAMBIO";
        }
        if (t.contains("certificado digital") || t.contains("e-cnpj") || t.contains("ecnpj")
                || t.contains("procuracao eletronic") || t.contains("procuração eletrônic")) {
            return "CERTIFICADO_DIGITAL";
        }
        if (t.contains("emprest") || t.contains("financi")) {
            return "EMPRESTIMO_FINANCIAMENTO";
        }
        if (t.contains("seguro") || t.contains("apolice") || t.contains("apólice")) {
            return "SEGUROS_APOLICES";
        }
        if (t.contains("relatorio") || t.contains("relatório")
                || t.contains("indicador") || t.contains("kpi") || t.contains("dashboard")) {
            return "RELATORIOS_GERENCIAIS";
        }
        if (t.contains("fluxo") && t.contains("caixa")) {
            return "FLUXO_CAIXA";
        }
        return "OUTROS";
    }

    public static String tituloAba(String idAba) {
        return TITULO_ABA.getOrDefault(idAba, "Outros");
    }

    /** Ordem fixa das abas na UI (todas aparecem, mesmo vazias). */
    public static List<String> ordemAbas() {
        return new ArrayList<>(TITULO_ABA.keySet());
    }
}
