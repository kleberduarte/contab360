package com.contabilidade.pj.auth.service;

import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.contabilidade.pj.auth.entity.*;
import com.contabilidade.pj.auth.repository.*;

@Service
public class AuthService {

    private static final int SESSION_HOURS = 24;

    private final UsuarioRepository usuarioRepository;
    private final SessaoAcessoRepository sessaoAcessoRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UsuarioRepository usuarioRepository,
            SessaoAcessoRepository sessaoAcessoRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoAcessoRepository = sessaoAcessoRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
    }

    @Transactional
    public LoginResponse login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas."));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }
        usuario = vincularClientePfAutomaticamente(usuario);

        sessaoAcessoRepository.deleteByExpiraEmBefore(LocalDateTime.now());

        SessaoAcesso sessao = new SessaoAcesso();
        sessao.setUsuario(usuario);
        sessao.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        sessao.setExpiraEm(LocalDateTime.now().plusHours(SESSION_HOURS));
        sessaoAcessoRepository.save(sessao);

        return LoginResponse.from(usuario, sessao.getToken(), sessao.getExpiraEm());
    }

    private Usuario vincularClientePfAutomaticamente(Usuario usuario) {
        if (usuario.getPerfil() != PerfilUsuario.CLIENTE || usuario.getClientePessoaFisica() != null) {
            return usuario;
        }
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            return usuario;
        }
        ClientePessoaFisica clientePf = clientePessoaFisicaRepository
                .findFirstByNomeCompletoIgnoreCase(usuario.getNome().trim())
                .orElse(null);
        if (clientePf == null) {
            return usuario;
        }
        usuario.setClientePessoaFisica(clientePf);
        return usuarioRepository.save(usuario);
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

    /** Remove a sessão do banco (logout explícito ou encerramento do cliente). */
    @Transactional
    public void revogarSessao(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessaoAcessoRepository.findByToken(token).ifPresent(sessaoAcessoRepository::delete);
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
            Long empresaId,
            boolean senhaTempAtiva,
            Long clientePessoaFisicaId
    ) {
        static LoginResponse from(Usuario usuario, String token, LocalDateTime expiraEm) {
            return new LoginResponse(
                    token,
                    expiraEm,
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getPerfil().name(),
                    usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                    usuario.isSenhaTempAtiva(),
                    usuario.getClientePessoaFisica() != null ? usuario.getClientePessoaFisica().getId() : null
            );
        }
    }
}
