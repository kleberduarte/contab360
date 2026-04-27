package com.contabilidade.pj.clientepf;

import com.contabilidade.pj.auth.AuthContext;
import com.contabilidade.pj.auth.Usuario;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes-pessoa-fisica")
public class ClientePessoaFisicaController {

    private final ClientePessoaFisicaService clientePessoaFisicaService;

    public ClientePessoaFisicaController(ClientePessoaFisicaService clientePessoaFisicaService) {
        this.clientePessoaFisicaService = clientePessoaFisicaService;
    }

    @GetMapping
    public List<ClientePessoaFisica> listar(@RequestParam(required = false) Boolean incluirInativas) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        boolean incluir = Boolean.TRUE.equals(incluirInativas);
        return clientePessoaFisicaService.listar(usuario, incluir);
    }

    @PostMapping
    public ResponseEntity<ClientePessoaFisica> criar(@Valid @RequestBody ClientePessoaFisica dados) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        ClientePessoaFisica salvo = clientePessoaFisicaService.criar(dados, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    public ClientePessoaFisica atualizar(@PathVariable Long id, @RequestBody ClientePessoaFisica dados) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return clientePessoaFisicaService.atualizar(id, dados, usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        clientePessoaFisicaService.excluir(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reativar")
    public ClientePessoaFisica reativar(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return clientePessoaFisicaService.reativar(id, usuario);
    }
}
