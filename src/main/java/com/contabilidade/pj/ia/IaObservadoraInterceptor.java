package com.contabilidade.pj.ia;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IaObservadoraInterceptor implements HandlerInterceptor {

    private final AuditoriaService auditoriaService;

    public IaObservadoraInterceptor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return;
        }
        if (path.equals("/api/health") || path.equals("/api/auth/login")) {
            return;
        }
        try {
            auditoriaService.registrarHttp(request.getMethod(), path, response.getStatus());
        } catch (Exception ignored) {
            // nao interrompe a resposta ao usuario
        }
    }
}
