package com.contabilidade.pj.fiscal.sefaz;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NfseProperties.class)
public class NfseBeansConfiguration {

    @Bean
    public NfseSaoPauloCliente nfseSaoPauloCliente() {
        return new NfseSaoPauloClienteImpl();
    }

    @Bean
    public NfseEmissaoPort nfseEmissaoPort(
            NfseProperties nfseProperties,
            SefazNfeProperties sefazNfeProperties,
            NfseSaoPauloCliente nfseSaoPauloCliente
    ) {
        if (!nfseProperties.isIntegracaoPmspAtiva()) {
            return switch (nfseProperties.getModo()) {
                case DESLIGADO -> new NfseEmissaoDesligada();
                case SIMULACAO, REAL -> new NfseEmissaoSimulacao();
            };
        }
        return switch (nfseProperties.getModo()) {
            case DESLIGADO -> new NfseEmissaoDesligada();
            case SIMULACAO -> new NfseEmissaoSimulacao();
            case REAL -> new NfseEmissaoRealSaoPaulo(nfseProperties, sefazNfeProperties, nfseSaoPauloCliente);
        };
    }
}
