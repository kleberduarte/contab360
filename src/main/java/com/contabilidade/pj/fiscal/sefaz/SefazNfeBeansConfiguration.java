package com.contabilidade.pj.fiscal.sefaz;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SefazNfeProperties.class)
public class SefazNfeBeansConfiguration {

    @Bean
    public AutorizacaoFiscalPort autorizacaoFiscalPort(SefazNfeProperties properties) {
        if (properties.getModo() == SefazNfeProperties.Modo.REAL) {
            return new AutorizacaoFiscalRealStub(properties);
        }
        return new AutorizacaoFiscalSimulacao();
    }
}
