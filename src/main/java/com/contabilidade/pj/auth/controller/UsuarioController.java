package com.contabilidade.pj.auth.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.contabilidade.pj.auth.entity.*;
import com.contabilidade.pj.auth.service.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioService.UsuarioResponse> listar() {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        return usuarioService.listar(usuario);
    }

    @PostMapping
    public ResponseEntity<UsuarioService.UsuarioResponse> criar(@Valid @RequestBody UsuarioService.UsuarioRequest request) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        UsuarioService.UsuarioResponse resp = usuarioService.criar(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    public UsuarioService.UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody UsuarioService.UsuarioRequest request) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        return usuarioService.editar(id, request, usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        usuarioService.desativar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        usuarioService.reativar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/redefinir-senha")
    public UsuarioService.UsuarioResponse redefinirSenha(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        return usuarioService.redefinirSenha(id, usuario);
    }
}
