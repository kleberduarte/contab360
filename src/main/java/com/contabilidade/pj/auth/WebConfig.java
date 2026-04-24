package com.contabilidade.pj.auth;

import com.contabilidade.pj.ia.IaObservadoraInterceptor;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final IaObservadoraInterceptor iaObservadoraInterceptor;

    @Value("${contab360.cors.origins:}")
    private String corsOrigins;

    public WebConfig(
            AuthInterceptor authInterceptor,
            IaObservadoraInterceptor iaObservadoraInterceptor
    ) {
        this.authInterceptor = authInterceptor;
        this.iaObservadoraInterceptor = iaObservadoraInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsOrigins == null || corsOrigins.isBlank()) return;
        String[] origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
        registry.addInterceptor(iaObservadoraInterceptor);
    }
}
