package com.contabilidade.pj.auth;

import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthSeedConfig {

    @Bean
    CommandLineRunner seedUsuariosIniciais(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            AuthService authService
    ) {
        return args -> {
            if (usuarioRepository.findByEmail("contador@demo.com").isEmpty()) {
                Usuario contador = new Usuario();
                contador.setNome("Conta Demo");
                contador.setEmail("contador@demo.com");
                contador.setSenhaHash(authService.passwordEncoder().encode("123456"));
                contador.setPerfil(PerfilUsuario.CONTADOR);
                usuarioRepository.save(contador);
            }

            List<Empresa> empresas = empresaRepository.findAll();
            if (!empresas.isEmpty() && usuarioRepository.findByEmail("cliente@demo.com").isEmpty()) {
                Usuario cliente = new Usuario();
                cliente.setNome("Cliente Demo");
                cliente.setEmail("cliente@demo.com");
                cliente.setSenhaHash(authService.passwordEncoder().encode("123456"));
                cliente.setPerfil(PerfilUsuario.CLIENTE);
                cliente.setEmpresa(empresas.get(0));
                usuarioRepository.save(cliente);
            }
        };
    }
}
