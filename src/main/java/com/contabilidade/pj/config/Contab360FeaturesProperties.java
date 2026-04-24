package com.contabilidade.pj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Flags de funcionalidades. Em produção, ative por ambiente (ex.: certificado digital).
 */
@ConfigurationProperties(prefix = "contab360.features")
public class Contab360FeaturesProperties {

    /**
     * Módulo de venda/controle de certificado digital (pedidos, vínculo com empresa, status).
     * Quando {@code false}, a API retorna 403 e o menu fica oculto.
     */
    private boolean certificadoDigital = false;

    public boolean isCertificadoDigital() {
        return certificadoDigital;
    }

    public void setCertificadoDigital(boolean certificadoDigital) {
        this.certificadoDigital = certificadoDigital;
    }
}
