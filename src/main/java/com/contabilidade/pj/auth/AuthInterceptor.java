package com.contabilidade.pj.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")
                || path.equals("/api/health")
                || path.equals("/api/features")
                || path.equals("/api/auth/login")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            escreverErro(response, HttpServletResponse.SC_UNAUTHORIZED, "Token de acesso ausente.");
            return false;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            escreverErro(response, HttpServletResponse.SC_UNAUTHORIZED, "Token de acesso inválido.");
            return false;
        }

        try {
            Usuario usuario = authService.validarToken(token);
            AuthContext.set(usuario);
            return true;
        } catch (IllegalArgumentException ex) {
            escreverErro(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void escreverErro(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String payload = "{\"message\":\"" + message.replace("\"", "'") + "\"}";
        response.getWriter().write(payload);
    }
}
