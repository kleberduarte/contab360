package com.contabilidade.pj.auth.service;

import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.contabilidade.pj.auth.entity.*;
import com.contabilidade.pj.auth.repository.*;
import com.contabilidade.pj.auth.entity.PerfilUsuario;

@Service
public class UsuarioService {

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final AuthService authService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            AuthService authService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para listar usuários.");
        }
        return usuarioRepository.findAllComEmpresa().stream()
                .map(u -> toResponse(u, null))
                .toList();
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Clientes não podem criar usuários.");
        }
        PerfilUsuario novoPerfil = parsePerfil(request.perfil());
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && novoPerfil != PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Contador só pode criar usuários do tipo CLIENTE.");
        }
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail.");
        }
        Empresa empresa = resolverEmpresa(request.empresaId());
        String senhaTemp = gerarSenhaTemp();
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(novoPerfil);
        usuario.setEmpresa(empresa);
        usuario.setSenhaHash(authService.passwordEncoder().encode(senhaTemp));
        usuario.setAtivo(true);
        usuario.setSenhaTempAtiva(true);
        usuarioRepository.save(usuario);
        return toResponse(usuario, senhaTemp);
    }

    @Transactional
    public UsuarioResponse editar(Long id, UsuarioRequest request, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para editar usuários.");
        }
        if (id.equals(usuarioAtual.getId())) {
            throw new IllegalArgumentException("Use 'Minha conta' para alterar seus próprios dados.");
        }
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        PerfilUsuario novoPerfil = parsePerfil(request.perfil());
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR) {
            if (target.getPerfil() != PerfilUsuario.CLIENTE) {
                throw new IllegalArgumentException("Contador só pode editar usuários do tipo CLIENTE.");
            }
            if (novoPerfil != PerfilUsuario.CLIENTE) {
                throw new IllegalArgumentException("Contador não pode alterar o perfil para " + novoPerfil + ".");
            }
        }
        String email = request.email().trim().toLowerCase();
        if (!target.getEmail().equals(email) && usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("Já existe outro usuário com este e-mail.");
        }
        target.setNome(request.nome().trim());
        target.setEmail(email);
        target.setPerfil(novoPerfil);
        target.setEmpresa(resolverEmpresa(request.empresaId()));
        usuarioRepository.save(target);
        return toResponse(target, null);
    }

    @Transactional
    public void desativar(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para desativar usuários.");
        }
        if (id.equals(usuarioAtual.getId())) {
            throw new IllegalArgumentException("Você não pode desativar a sua própria conta.");
        }
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && target.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Contador só pode desativar usuários do tipo CLIENTE.");
        }
        target.setAtivo(false);
        usuarioRepository.save(target);
    }

    @Transactional
    public void reativar(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para reativar usuários.");
        }
        if (id.equals(usuarioAtual.getId())) {
            throw new IllegalArgumentException("Você não pode reativar a sua própria conta.");
        }
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && target.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Contador só pode reativar usuários do tipo CLIENTE.");
        }
        target.setAtivo(true);
        usuarioRepository.save(target);
    }

    @Transactional
    public UsuarioResponse redefinirSenha(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para redefinir senhas.");
        }
        if (id.equals(usuarioAtual.getId())) {
            throw new IllegalArgumentException("Use 'Minha conta' para alterar sua própria senha.");
        }
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && target.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Contador só pode redefinir senhas de usuários do tipo CLIENTE.");
        }
        String senhaTemp = gerarSenhaTemp();
        target.setSenhaHash(authService.passwordEncoder().encode(senhaTemp));
        target.setSenhaTempAtiva(true);
        usuarioRepository.save(target);
        return toResponse(target, senhaTemp);
    }

    @Transactional
    public void trocarSenha(String senhaAtual, String novaSenha, Usuario usuarioAtual) {
        Usuario u = usuarioRepository.findById(usuarioAtual.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!authService.passwordEncoder().matches(senhaAtual, u.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");
        }
        u.setSenhaHash(authService.passwordEncoder().encode(novaSenha));
        u.setSenhaTempAtiva(false);
        usuarioRepository.save(u);
    }

    @Transactional
    public void definirNovaSenha(String novaSenha, Usuario usuarioAtual) {
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }
        Usuario u = usuarioRepository.findById(usuarioAtual.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        u.setSenhaHash(authService.passwordEncoder().encode(novaSenha));
        u.setSenhaTempAtiva(false);
        usuarioRepository.save(u);
    }

    @Transactional
    public void atualizarNome(String novoNome, Usuario usuarioAtual) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser vazio.");
        }
        Usuario u = usuarioRepository.findById(usuarioAtual.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        u.setNome(novoNome.trim());
        usuarioRepository.save(u);
    }

    private Empresa resolverEmpresa(Long empresaId) {
        if (empresaId == null) return null;
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
    }

    private PerfilUsuario parsePerfil(String perfil) {
        try {
            return PerfilUsuario.valueOf(perfil);
        } catch (Exception e) {
            throw new IllegalArgumentException("Perfil inválido: " + perfil);
        }
    }

    private String gerarSenhaTemp() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private UsuarioResponse toResponse(Usuario u, String senhaTempRevelada) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil().name(),
                u.getEmpresa() != null ? u.getEmpresa().getId() : null,
                u.getEmpresa() != null ? u.getEmpresa().getRazaoSocial() : null,
                u.isAtivo(),
                senhaTempRevelada
        );
    }

    public record UsuarioRequest(
            @NotBlank String nome,
            @Email @NotBlank String email,
            @NotBlank String perfil,
            Long empresaId
    ) {}

    public record UsuarioResponse(
            Long id,
            String nome,
            String email,
            String perfil,
            Long empresaId,
            String empresaNome,
            boolean ativo,
            String senhaTempRevelada
    ) {}
}
