package com.contabilidade.pj.auth.service;

import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final AuthService authService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            AuthService authService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Sem permissão para listar usuários.");
        }
        return usuarioRepository.findAllComEmpresa().stream()
                .filter(u -> usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR || u.getPerfil() == PerfilUsuario.CLIENTE)
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
        ClientePessoaFisica clientePessoaFisica = resolverClientePessoaFisica(
                request.clientePessoaFisicaId(),
                novoPerfil,
                request.nome(),
                null,
                empresa == null
        );
        String senhaTemp = gerarSenhaTemp();
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(novoPerfil);
        usuario.setEmpresa(empresa);
        usuario.setClientePessoaFisica(clientePessoaFisica);
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
        Empresa empresaNova = resolverEmpresa(request.empresaId());
        target.setEmpresa(empresaNova);
        boolean exigePfClienteNaEdicao = novoPerfil == PerfilUsuario.CLIENTE
                && empresaNova == null
                && target.getClientePessoaFisica() == null;
        target.setClientePessoaFisica(resolverClientePessoaFisica(
                request.clientePessoaFisicaId(),
                novoPerfil,
                request.nome(),
                target.getClientePessoaFisica(),
                exigePfClienteNaEdicao
        ));
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

    private ClientePessoaFisica resolverClientePessoaFisica(
            Long clientePessoaFisicaId,
            PerfilUsuario perfil,
            String nomeUsuario,
            ClientePessoaFisica vinculoAtual,
            boolean obrigatorioQuandoCliente
    ) {
        if (perfil != PerfilUsuario.CLIENTE) {
            return null;
        }
        if (clientePessoaFisicaId != null) {
            return clientePessoaFisicaRepository.findById(clientePessoaFisicaId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente pessoa física não encontrado."));
        }
        if (nomeUsuario != null && !nomeUsuario.isBlank()) {
            String nome = nomeUsuario.trim();
            ClientePessoaFisica exato = clientePessoaFisicaRepository
                    .findFirstByNomeCompletoIgnoreCase(nome)
                    .orElse(null);
            if (exato != null) {
                return exato;
            }

            String nomeNormalizado = normalizarNome(nome);
            List<ClientePessoaFisica> candidatos = new ArrayList<>();
            for (ClientePessoaFisica pf : clientePessoaFisicaRepository.findAllByAtivoTrueOrderByNomeCompletoAsc()) {
                String pfNome = pf.getNomeCompleto() == null ? "" : pf.getNomeCompleto().trim();
                if (pfNome.isBlank()) {
                    continue;
                }
                String pfNormalizado = normalizarNome(pfNome);
                if (pfNormalizado.equals(nomeNormalizado)) {
                    candidatos.add(pf);
                    continue;
                }
                if (pfNormalizado.contains(nomeNormalizado) || nomeNormalizado.contains(pfNormalizado)) {
                    candidatos.add(pf);
                }
            }
            if (candidatos.size() == 1) {
                return candidatos.get(0);
            }
            if (candidatos.size() > 1) {
                throw new IllegalArgumentException(
                        "Há mais de um cliente PF compatível com o nome informado. Ajuste o nome do usuário para ficar igual ao cadastro PF."
                );
            }
        }
        if (obrigatorioQuandoCliente) {
            throw new IllegalArgumentException(
                    "Não foi possível vincular automaticamente o cliente PF. Use o mesmo nome do cadastro em Cliente PF."
            );
        }
        return vinculoAtual;
    }

    private static String normalizarNome(String nome) {
        String base = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return base;
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
                u.getClientePessoaFisica() != null ? u.getClientePessoaFisica().getId() : null,
                u.getClientePessoaFisica() != null ? u.getClientePessoaFisica().getNomeCompleto() : null,
                u.isAtivo(),
                senhaTempRevelada
        );
    }

    public record UsuarioRequest(
            @NotBlank String nome,
            @Email @NotBlank String email,
            @NotBlank String perfil,
            Long empresaId,
            Long clientePessoaFisicaId
    ) {}

    public record UsuarioResponse(
            Long id,
            String nome,
            String email,
            String perfil,
            Long empresaId,
            String empresaNome,
            Long clientePessoaFisicaId,
            String clientePessoaFisicaNome,
            boolean ativo,
            String senhaTempRevelada
    ) {}
}
