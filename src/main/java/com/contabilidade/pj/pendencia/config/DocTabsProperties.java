package com.contabilidade.pj.pendencia.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contab360.doc-tabs")
public class DocTabsProperties {

    private List<String> ordem = new ArrayList<>();
    private Map<String, String> titulos = new LinkedHashMap<>();
    private Map<String, String> tipoDetectado = new LinkedHashMap<>();
    private List<TemplateRule> templateRules = new ArrayList<>();

    public DocTabsProperties() {
        titulos.put("NFE", "NF-e (produto)");
        titulos.put("NFSE", "NFS-e (serviço)");
        titulos.put("NFCE", "NFC-e (consumidor)");
        titulos.put("CTE", "CT-e (transporte)");
        titulos.put("MDFE", "MDF-e (manifesto)");
        titulos.put("CTE_OS", "CT-e OS");
        titulos.put("NOTA_FISCAL", "Nota fiscal");
        titulos.put("FOLHA_PAGAMENTO", "Holerite / Folha");
        titulos.put("GUIA_IRPF", "IRPF");
        titulos.put("GUIA_IRPJ", "IRPJ");
        titulos.put("GUIA_CSLL", "CSLL");
        titulos.put("GUIA_PIS", "PIS / PASEP");
        titulos.put("GUIA_COFINS", "COFINS");
        titulos.put("GUIA_IPI", "IPI");
        titulos.put("GUIA_IOF", "IOF");
        titulos.put("GUIA_IRRF", "IRRF");
        titulos.put("GUIA_CIDE", "CIDE");
        titulos.put("GUIA_II", "II (importação)");
        titulos.put("GUIA_ITR", "ITR");
        titulos.put("GUIA_FUNRURAL", "FUNRURAL");
        titulos.put("DECLARACAO_OBRIGACAO", "Declarações / Obrigações");
        titulos.put("SPED_FISCAL_CONTRIBUICOES", "SPED fiscal / contribuições");
        titulos.put("SPED_CONTABIL_FISCAL", "SPED contábil / fiscal");
        titulos.put("BALANCETES_DEMONSTRACOES", "Balancetes / Demonstrações");
        titulos.put("EXTRATO_BANCARIO", "Extrato bancário");
        titulos.put("CONCILIACAO_FINANCEIRA", "Conciliação financeira");
        titulos.put("CONTAS_PAGAR_RECEBER", "Contas a pagar / receber");
        titulos.put("RECIBO_DESPESA", "Recibo / Despesa");
        titulos.put("CONTRATOS_SOCIOS", "Contratos / Sócios");
        titulos.put("ATAS_E_REGISTROS", "Atas / Registros");
        titulos.put("CERTIDOES_LICENCAS", "Certidões / Licenças");
        titulos.put("PROCESSOS_FISCAIS_JURIDICOS", "Processos fiscais / jurídicos");
        titulos.put("PATRIMONIO_IMOBILIZADO", "Patrimônio / Imobilizado");
        titulos.put("ESTOQUE_CUSTO", "Estoque / Custo");
        titulos.put("COMERCIO_EXTERIOR_CAMBIO", "Comércio exterior / Câmbio");
        titulos.put("CERTIFICADO_DIGITAL", "Certificado digital");
        titulos.put("EMPRESTIMO_FINANCIAMENTO", "Empréstimo / Financiamento");
        titulos.put("SEGUROS_APOLICES", "Seguros / Apólices");
        titulos.put("RELATORIOS_GERENCIAIS", "Relatórios gerenciais");
        titulos.put("FLUXO_CAIXA", "Fluxo de caixa");
        titulos.put("OUTROS", "Outros");

        ordem = new ArrayList<>(titulos.keySet());

        tipoDetectado.put("NOTA_FISCAL", "NOTA_FISCAL");
        tipoDetectado.put("NFE_XML", "NFE");
        tipoDetectado.put("NFSE_XML", "NFSE");
        tipoDetectado.put("NFE", "NFE");
        tipoDetectado.put("NFSE", "NFSE");
        tipoDetectado.put("NFCE", "NFCE");
        tipoDetectado.put("CTE", "CTE");
        tipoDetectado.put("MDFE", "MDFE");
        tipoDetectado.put("CTE_OS", "CTE_OS");
        tipoDetectado.put("FOLHA_PAGAMENTO", "FOLHA_PAGAMENTO");
        tipoDetectado.put("GUIA_IMPOSTO", "GUIA_IRPF");
        tipoDetectado.put("GUIA_IRPF", "GUIA_IRPF");
        tipoDetectado.put("GUIA_IRPJ", "GUIA_IRPJ");
        tipoDetectado.put("GUIA_CSLL", "GUIA_CSLL");
        tipoDetectado.put("GUIA_PIS", "GUIA_PIS");
        tipoDetectado.put("GUIA_COFINS", "GUIA_COFINS");
        tipoDetectado.put("GUIA_IPI", "GUIA_IPI");
        tipoDetectado.put("GUIA_IOF", "GUIA_IOF");
        tipoDetectado.put("GUIA_IRRF", "GUIA_IRRF");
        tipoDetectado.put("GUIA_CIDE", "GUIA_CIDE");
        tipoDetectado.put("GUIA_II", "GUIA_II");
        tipoDetectado.put("GUIA_ITR", "GUIA_ITR");
        tipoDetectado.put("GUIA_FUNRURAL", "GUIA_FUNRURAL");
        tipoDetectado.put("DECLARACAO_OBRIGACAO", "DECLARACAO_OBRIGACAO");
        tipoDetectado.put("DECLARACAO_ACESSORIA", "DECLARACAO_OBRIGACAO");
        tipoDetectado.put("SPED_FISCAL_CONTRIBUICOES", "SPED_FISCAL_CONTRIBUICOES");
        tipoDetectado.put("SPED_CONTABIL_FISCAL", "SPED_CONTABIL_FISCAL");
        tipoDetectado.put("BALANCETES_DEMONSTRACOES", "BALANCETES_DEMONSTRACOES");
        tipoDetectado.put("EXTRATO_BANCARIO", "EXTRATO_BANCARIO");
        tipoDetectado.put("CONCILIACAO_FINANCEIRA", "CONCILIACAO_FINANCEIRA");
        tipoDetectado.put("CONTAS_PAGAR_RECEBER", "CONTAS_PAGAR_RECEBER");
        tipoDetectado.put("RECIBO_DESPESA", "RECIBO_DESPESA");
        tipoDetectado.put("CONTRATOS_SOCIOS", "CONTRATOS_SOCIOS");
        tipoDetectado.put("CONTRATO_SOCIAL", "CONTRATOS_SOCIOS");
        tipoDetectado.put("ATAS_E_REGISTROS", "ATAS_E_REGISTROS");
        tipoDetectado.put("ATA_REUNIAO", "ATAS_E_REGISTROS");
        tipoDetectado.put("CERTIDOES_LICENCAS", "CERTIDOES_LICENCAS");
        tipoDetectado.put("PROCESSOS_FISCAIS_JURIDICOS", "PROCESSOS_FISCAIS_JURIDICOS");
        tipoDetectado.put("PATRIMONIO_IMOBILIZADO", "PATRIMONIO_IMOBILIZADO");
        tipoDetectado.put("ESTOQUE_CUSTO", "ESTOQUE_CUSTO");
        tipoDetectado.put("COMERCIO_EXTERIOR_CAMBIO", "COMERCIO_EXTERIOR_CAMBIO");
        tipoDetectado.put("CERTIFICADO_DIGITAL", "CERTIFICADO_DIGITAL");
        tipoDetectado.put("EMPRESTIMO_FINANCIAMENTO", "EMPRESTIMO_FINANCIAMENTO");
        tipoDetectado.put("SEGUROS_APOLICES", "SEGUROS_APOLICES");
        tipoDetectado.put("RELATORIOS_GERENCIAIS", "RELATORIOS_GERENCIAIS");
        tipoDetectado.put("FLUXO_CAIXA", "FLUXO_CAIXA");
        tipoDetectado.put("DESCONHECIDO", "OUTROS");

        templateRules.add(new TemplateRule("NOTA_FISCAL", List.of("nota")));
        templateRules.add(new TemplateRule("FOLHA_PAGAMENTO",
                List.of("holerite", "folha", "contracheque", "contra-cheque", "pro-labore", "pro labore")));
        templateRules.add(new TemplateRule("GUIA_FUNRURAL", List.of("funrural", "funrea")));
        templateRules.add(new TemplateRule("GUIA_ITR", List.of("itr", "territorial rural")));
        templateRules.add(new TemplateRule("GUIA_II", List.of("imposto de importacao", "imposto de importação", "duimp")));
        templateRules.add(new TemplateRule("GUIA_CIDE", List.of("cide")));
        templateRules.add(new TemplateRule("GUIA_IOF", List.of("iof")));
        templateRules.add(new TemplateRule("GUIA_IPI", List.of("ipi", "produtos industrializados")));
        templateRules.add(new TemplateRule("GUIA_IRRF", List.of("irrf", "retido na fonte")));
        templateRules.add(new TemplateRule("GUIA_COFINS", List.of("cofins")));
        templateRules.add(new TemplateRule("GUIA_PIS", List.of("pis/pasep", "pis pasep", "pis ")));
        templateRules.add(new TemplateRule("GUIA_CSLL", List.of("csll")));
        templateRules.add(new TemplateRule("GUIA_IRPJ", List.of("irpj", "lucro presumido", "lucro real")));
        templateRules.add(new TemplateRule("GUIA_IRPF", List.of("irpf", "carne-leao", "carnê-leão")));
        templateRules.add(new TemplateRule("GUIA_IRPF", List.of("guia", "imposto", "darf", "das")));
        templateRules.add(new TemplateRule("DECLARACAO_OBRIGACAO",
                List.of("declar", "dctf", "reinf", "esocial", "pgdas", "defis", "dirf", "dmed", "dimob")));
        templateRules.add(new TemplateRule("SPED_FISCAL_CONTRIBUICOES", List.of("sped", "efd")));
        templateRules.add(new TemplateRule("SPED_CONTABIL_FISCAL", List.of("ecd", "ecf")));
        templateRules.add(new TemplateRule("BALANCETES_DEMONSTRACOES", List.of("balancete", "dre", "bp")));
        templateRules.add(new TemplateRule("EXTRATO_BANCARIO", List.of("extrato")));
        templateRules.add(new TemplateRule("CONCILIACAO_FINANCEIRA", List.of("concili")));
        templateRules.add(new TemplateRule("CONTAS_PAGAR_RECEBER",
                List.of("contas a pagar", "contas a receber", "boleto", "cobranca", "cobrança", "remessa", "retorno")));
        templateRules.add(new TemplateRule("RECIBO_DESPESA", List.of("recibo", "despesa")));
        templateRules.add(new TemplateRule("CONTRATOS_SOCIOS", List.of("contrato", "socio", "sócio")));
        templateRules.add(new TemplateRule("ATAS_E_REGISTROS", List.of("ata", "registro", "assembleia")));
        templateRules.add(new TemplateRule("CERTIDOES_LICENCAS",
                List.of("certidao", "certidão", "alvara", "alvará", "licenca", "licença")));
        templateRules.add(new TemplateRule("PROCESSOS_FISCAIS_JURIDICOS",
                List.of("auto de infracao", "auto de infração", "intimacao", "intimação", "notificacao", "notificação",
                        "processo")));
        templateRules.add(new TemplateRule("PATRIMONIO_IMOBILIZADO",
                List.of("imobilizado", "patrimonio", "patrimônio", "depreciacao", "depreciação")));
        templateRules.add(new TemplateRule("ESTOQUE_CUSTO", List.of("estoque", "inventario", "inventário", "custo")));
        templateRules.add(new TemplateRule("COMERCIO_EXTERIOR_CAMBIO",
                List.of("importacao", "importação", "exportacao", "exportação", "cambio", "câmbio", "invoice")));
        templateRules.add(new TemplateRule("CERTIFICADO_DIGITAL",
                List.of("certificado digital", "e-cnpj", "ecnpj", "procuracao eletronic", "procuração eletrônic")));
        templateRules.add(new TemplateRule("EMPRESTIMO_FINANCIAMENTO", List.of("emprest", "financi")));
        templateRules.add(new TemplateRule("SEGUROS_APOLICES", List.of("seguro", "apolice", "apólice")));
        templateRules.add(new TemplateRule("RELATORIOS_GERENCIAIS",
                List.of("relatorio", "relatório", "indicador", "kpi", "dashboard")));
        templateRules.add(new TemplateRule("FLUXO_CAIXA", List.of("fluxo", "caixa")));
    }

    public List<String> getOrdem() {
        return ordem;
    }

    public void setOrdem(List<String> ordem) {
        this.ordem = ordem;
    }

    public Map<String, String> getTitulos() {
        return titulos;
    }

    public void setTitulos(Map<String, String> titulos) {
        this.titulos = titulos;
    }

    public Map<String, String> getTipoDetectado() {
        return tipoDetectado;
    }

    public void setTipoDetectado(Map<String, String> tipoDetectado) {
        this.tipoDetectado = tipoDetectado;
    }

    public List<TemplateRule> getTemplateRules() {
        return templateRules;
    }

    public void setTemplateRules(List<TemplateRule> templateRules) {
        this.templateRules = templateRules;
    }

    public static class TemplateRule {
        private String aba;
        private List<String> keywords = List.of();

        public TemplateRule() {
        }

        public TemplateRule(String aba, List<String> keywords) {
            this.aba = aba;
            this.keywords = keywords;
        }

        public String getAba() {
            return aba;
        }

        public void setAba(String aba) {
            this.aba = aba;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }
    }
}
