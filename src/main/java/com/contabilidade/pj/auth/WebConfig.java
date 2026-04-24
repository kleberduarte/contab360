package com.contabilidade.pj.auth;

import com.contabilidade.pj.ia.IaObservadoraInterceptor;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] DEFAULT_CORS_ORIGINS = {
            "https://contab360.vercel.app"
    };

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

    @Bean
    public CorsFilter corsFilter() {
        String[] origins = (corsOrigins == null || corsOrigins.isBlank())
                ? DEFAULT_CORS_ORIGINS
                : Arrays.stream(corsOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toArray(String[]::new);
        if (origins.length == 0) origins = DEFAULT_CORS_ORIGINS;

        CorsConfiguration config = new CorsConfiguration();
        for (String origin : origins) {
            config.addAllowedOrigin(origin);
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
        registry.addInterceptor(iaObservadoraInterceptor);
    }
}
