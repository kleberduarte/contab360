package com.contabilidade.pj.fiscal.sefaz;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração para integração futura com SEFAZ (NFe). Em {@code SIMULACAO}, gera chave/protocolo fictícios e PDF de demonstração.
 */
@ConfigurationProperties(prefix = "contab360.sefaz")
public class SefazNfeProperties {

    /**
     * {@code SIMULACAO}: fluxo local + DANFE simulado. {@code REAL}: usar implementação que chamará webservices (a implementar).
     */
    private Modo modo = Modo.SIMULACAO;

    /** HOMOLOGACAO ou PRODUCAO — usado quando {@code modo=REAL}. */
    private String ambiente = "HOMOLOGACAO";

    /** Caminho do PKCS12 (.pfx) do certificado A1 — quando integração real estiver pronta. */
    private String certificadoPath = "";

    private String certificadoSenha = "";

    /** URL do serviço NFeAutorizacao4 da UF — preencher por ambiente/UF. */
    private String nfeAutorizacaoUrl = "";

    public Modo getModo() {
        return modo;
    }

    public void setModo(Modo modo) {
        this.modo = modo;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getCertificadoPath() {
        return certificadoPath;
    }

    public void setCertificadoPath(String certificadoPath) {
        this.certificadoPath = certificadoPath;
    }

    public String getCertificadoSenha() {
        return certificadoSenha;
    }

    public void setCertificadoSenha(String certificadoSenha) {
        this.certificadoSenha = certificadoSenha;
    }

    public String getNfeAutorizacaoUrl() {
        return nfeAutorizacaoUrl;
    }

    public void setNfeAutorizacaoUrl(String nfeAutorizacaoUrl) {
        this.nfeAutorizacaoUrl = nfeAutorizacaoUrl;
    }

    public enum Modo {
        SIMULACAO,
        REAL
    }
}
