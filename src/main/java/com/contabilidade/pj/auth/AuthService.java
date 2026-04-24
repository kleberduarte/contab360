package com.contabilidade.pj.auth;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int SESSION_HOURS = 12;

    private final UsuarioRepository usuarioRepository;
    private final SessaoAcessoRepository sessaoAcessoRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UsuarioRepository usuarioRepository,
            SessaoAcessoRepository sessaoAcessoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoAcessoRepository = sessaoAcessoRepository;
    }

    @Transactional
    public LoginResponse login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas."));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }

        sessaoAcessoRepository.deleteByExpiraEmBefore(LocalDateTime.now());

        SessaoAcesso sessao = new SessaoAcesso();
        sessao.setUsuario(usuario);
        sessao.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        sessao.setExpiraEm(LocalDateTime.now().plusHours(SESSION_HOURS));
        sessaoAcessoRepository.save(sessao);

        return LoginResponse.from(usuario, sessao.getToken(), sessao.getExpiraEm());
    }

    @Transactional(readOnly = true)
    public Usuario validarToken(String token) {
        SessaoAcesso sessao = sessaoAcessoRepository.findByTokenComUsuario(token)
                .orElseThrow(() -> new IllegalArgumentException("Sessão inválida."));
        if (sessao.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Sessão expirada.");
        }
        Usuario usuario = sessao.getUsuario();
        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuário desativado.");
        }
        return usuario;
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    public record LoginResponse(
            String token,
            LocalDateTime expiraEm,
            Long usuarioId,
            String nome,
            String email,
            String perfil,
            Long empresaId
    ) {
        static LoginResponse from(Usuario usuario, String token, LocalDateTime expiraEm) {
            return new LoginResponse(
                    token,
                    expiraEm,
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getPerfil().name(),
                    usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null
            );
        }
    }
}
