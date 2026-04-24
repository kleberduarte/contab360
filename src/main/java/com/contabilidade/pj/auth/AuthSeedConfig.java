package com.contabilidade.pj.auth;

import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Garante usuários demo e vínculo com empresas. Se a 2ª empresa foi cadastrada depois da primeira subida,
 * o {@code cliente2@demo.com} é criado/atualizado na próxima inicialização.
 * <p>
 * {@code contab360.demo-reset-passwords}: quando true (padrão), redefine a senha dos e-mails demo para {@code 123456}
 * (útil se o hash no MySQL estiver incorreto). Desative em produção.
 */
@Configuration
public class AuthSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthSeedConfig.class);
    private static final String DEMO_SENHA = "123456";

    @Value("${contab360.demo-reset-passwords:true}")
    private boolean demoResetPasswords;

    @Bean
    CommandLineRunner seedUsuariosIniciais(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            AuthService authService,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            garantirPerfilComAdm(jdbcTemplate);
            List<Empresa> empresas = empresaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

            syncAdmin(usuarioRepository, authService);
            syncContador(usuarioRepository, authService);
            syncCliente1(usuarioRepository, authService, empresas);
            syncCliente2(usuarioRepository, authService, empresas);
        };
    }

    private void garantirPerfilComAdm(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("ALTER TABLE usuarios MODIFY COLUMN perfil ENUM('ADM','CONTADOR','CLIENTE') NOT NULL");
        } catch (Exception ex) {
            log.debug("Não foi necessário/possível alterar enum de perfil automaticamente: {}", ex.getMessage());
        }
        try {
            // Corrige usuários que ficaram com ativo=0 por migration sem DEFAULT.
            int corrigidos = jdbcTemplate.update(
                    "UPDATE usuarios SET ativo = 1 WHERE ativo = 0 OR ativo IS NULL"
            );
            if (corrigidos > 0) {
                log.warn("Migration ativo: {} usuário(s) reativado(s) (coluna adicionada sem DEFAULT 1).", corrigidos);
            }
        } catch (Exception ex) {
            log.debug("Migration ativo não necessária ou falhou: {}", ex.getMessage());
        }
    }

    private void syncAdmin(UsuarioRepository usuarioRepository, AuthService authService) {
        Optional<Usuario> opt = usuarioRepository.findByEmail("admin@demo.com");
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Admin Demo");
            u.setEmail("admin@demo.com");
            u.setPerfil(PerfilUsuario.ADM);
            definirSenhaDemo(u, true, authService);
            usuarioRepository.save(u);
            log.info("Usuário demo criado: admin@demo.com");
        } else if (demoResetPasswords) {
            Usuario u = opt.get();
            boolean mudou = false;
            if (u.getPerfil() != PerfilUsuario.ADM) {
                u.setPerfil(PerfilUsuario.ADM);
                mudou = true;
            }
            definirSenhaDemo(u, false, authService);
            usuarioRepository.save(u);
            if (mudou) {
                log.info("Usuário demo atualizado para ADM: admin@demo.com");
            } else {
                log.debug("Senha demo atualizada: admin@demo.com");
            }
        }
    }

    private void syncContador(UsuarioRepository usuarioRepository, AuthService authService) {
        Optional<Usuario> opt = usuarioRepository.findByEmail("contador@demo.com");
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Conta Demo");
            u.setEmail("contador@demo.com");
            u.setPerfil(PerfilUsuario.CONTADOR);
            definirSenhaDemo(u, true, authService);
            usuarioRepository.save(u);
            log.info("Usuário demo criado: contador@demo.com");
        } else if (demoResetPasswords) {
            Usuario u = opt.get();
            definirSenhaDemo(u, false, authService);
            usuarioRepository.save(u);
            log.debug("Senha demo atualizada: contador@demo.com");
        }
    }

    private void syncCliente1(UsuarioRepository usuarioRepository, AuthService authService, List<Empresa> empresas) {
        if (empresas.isEmpty()) {
            return;
        }
        Empresa primeira = empresas.get(0);
        Optional<Usuario> opt = usuarioRepository.findByEmail("cliente@demo.com");
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Cliente Demo");
            u.setEmail("cliente@demo.com");
            u.setPerfil(PerfilUsuario.CLIENTE);
            u.setEmpresa(primeira);
            definirSenhaDemo(u, true, authService);
            usuarioRepository.save(u);
            log.info("Usuário demo criado: cliente@demo.com (empresa id={})", primeira.getId());
        } else {
            Usuario u = opt.get();
            boolean mudou = false;
            if (u.getEmpresa() == null || !u.getEmpresa().getId().equals(primeira.getId())) {
                u.setEmpresa(primeira);
                mudou = true;
            }
            if (demoResetPasswords) {
                definirSenhaDemo(u, false, authService);
                mudou = true;
            }
            if (mudou) {
                usuarioRepository.save(u);
            }
        }
    }

    private void syncCliente2(UsuarioRepository usuarioRepository, AuthService authService, List<Empresa> empresas) {
        if (empresas.size() < 2) {
            return;
        }
        Empresa empresaMaisRecente = empresas.get(empresas.size() - 1);
        Optional<Usuario> opt = usuarioRepository.findByEmail("cliente2@demo.com");
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Cliente Demo 2");
            u.setEmail("cliente2@demo.com");
            u.setPerfil(PerfilUsuario.CLIENTE);
            u.setEmpresa(empresaMaisRecente);
            definirSenhaDemo(u, true, authService);
            usuarioRepository.save(u);
            log.info("Usuário demo criado: cliente2@demo.com (empresa id={}, CNPJ vinculado à empresa mais recente)",
                    empresaMaisRecente.getId());
        } else {
            Usuario u = opt.get();
            boolean mudou = false;
            if (u.getEmpresa() == null || !u.getEmpresa().getId().equals(empresaMaisRecente.getId())) {
                u.setEmpresa(empresaMaisRecente);
                mudou = true;
            }
            if (demoResetPasswords) {
                definirSenhaDemo(u, false, authService);
                mudou = true;
            }
            if (mudou) {
                usuarioRepository.save(u);
                log.info("Usuário demo atualizado: cliente2@demo.com (empresa id={})", empresaMaisRecente.getId());
            }
        }
    }

    private void definirSenhaDemo(Usuario u, boolean novoUsuario, AuthService authService) {
        if (novoUsuario || demoResetPasswords) {
            u.setSenhaHash(authService.passwordEncoder().encode(DEMO_SENHA));
        }
    }
}
