package com.contabilidade.pj.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.contabilidade.pj.auth.service.AuthService;
import com.contabilidade.pj.auth.service.LoginRateLimiter;
import com.contabilidade.pj.auth.service.UsuarioService;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.service.AuthContext;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService authService, UsuarioService usuarioService, LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public AuthService.LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpRequest) {
        String ip = obterIp(httpRequest);
        if (rateLimiter.isoBloqueado(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Aguarde 15 minutos.");
        }
        try {
            AuthService.LoginResponse resposta = authService.login(req.email(), req.senha());
            rateLimiter.registrarSucesso(ip);
            return resposta;
        } catch (IllegalArgumentException ex) {
            rateLimiter.registrarFalha(ip);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String token = extrairBearerToken(httpRequest);
        authService.revogarSessao(token);
        return ResponseEntity.noContent().build();
    }

    private static String extrairBearerToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token de acesso ausente.");
        }
        return authHeader.substring("Bearer ".length()).trim();
    }

    private String obterIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocarSenhaRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        usuarioService.trocarSenha(req.senhaAtual(), req.novaSenha(), usuario);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/minha-conta")
    public ResponseEntity<Void> atualizarNome(@Valid @RequestBody AtualizarNomeRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        usuarioService.atualizarNome(req.nome(), usuario);
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String senha
    ) {}

    public record TrocarSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank String novaSenha
    ) {}

    public record AtualizarNomeRequest(
            @NotBlank String nome
    ) {}

    public record DefinirNovaSenhaRequest(
            @NotBlank @Size(min = 6) String novaSenha
    ) {}

    @PostMapping("/definir-nova-senha")
    public ResponseEntity<Void> definirNovaSenha(@Valid @RequestBody DefinirNovaSenhaRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        usuarioService.definirNovaSenha(req.novaSenha(), usuario);
        return ResponseEntity.noContent().build();
    }
}
