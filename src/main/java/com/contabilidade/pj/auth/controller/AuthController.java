package com.contabilidade.pj.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.contabilidade.pj.auth.service.AuthService;
import com.contabilidade.pj.auth.service.UsuarioService;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.entity.SessaoAcesso;
import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.service.AuthContext;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public AuthService.LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.senha());
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
