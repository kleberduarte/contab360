package com.contabilidade.pj.auth;

import com.contabilidade.pj.ia.IaObservadoraInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final IaObservadoraInterceptor iaObservadoraInterceptor;

    public WebConfig(
            AuthInterceptor authInterceptor,
            IaObservadoraInterceptor iaObservadoraInterceptor
    ) {
        this.authInterceptor = authInterceptor;
        this.iaObservadoraInterceptor = iaObservadoraInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
        registry.addInterceptor(iaObservadoraInterceptor);
    }
}
