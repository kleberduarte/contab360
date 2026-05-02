package com.contabilidade.pj.pendencia.controller;

import com.contabilidade.pj.auth.service.AuthContext;
import com.contabilidade.pj.auth.entity.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import com.contabilidade.pj.pendencia.service.*;
import com.contabilidade.pj.pendencia.entity.*;

@Validated
@RestController
@RequestMapping("/api/templates-documentos")
public class TemplateDocumentoController {

    private final TemplateDocumentoService templateDocumentoService;

    public TemplateDocumentoController(TemplateDocumentoService templateDocumentoService) {
        this.templateDocumentoService = templateDocumentoService;
    }

    @GetMapping
    public List<TemplateDocumentoResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long clientePessoaFisicaId
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        if (empresaId != null && clientePessoaFisicaId != null) {
            throw new IllegalArgumentException("Informe apenas empresaId ou clientePessoaFisicaId.");
        }
        if (empresaId == null && clientePessoaFisicaId == null) {
            throw new IllegalArgumentException("Informe empresaId ou clientePessoaFisicaId.");
        }
        List<TemplateDocumento> lista = empresaId != null
                ? templateDocumentoService.listarPorEmpresa(empresaId, usuario)
                : templateDocumentoService.listarPorClientePessoaFisica(clientePessoaFisicaId, usuario);
        return lista.stream().map(TemplateDocumentoResponse::fromEntity).toList();
    }

    @GetMapping("/sugestoes")
    public List<String> listarSugestoes() {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return templateDocumentoService.listarSugestoesNomes();
    }

    @PostMapping
    public ResponseEntity<TemplateDocumentoResponse> criar(@Valid @RequestBody CriarTemplateDocumentoRequest req) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        TemplateDocumento salvo = templateDocumentoService.criar(
                req.empresaId(),
                req.clientePessoaFisicaId(),
                req.nome(),
                req.obrigatorio(),
                usuario
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateDocumentoResponse.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public TemplateDocumentoResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarTemplateDocumentoRequest req
    ) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        TemplateDocumento salvo = templateDocumentoService.atualizar(id, req.nome(), req.obrigatorio(), usuario);
        return TemplateDocumentoResponse.fromEntity(salvo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        templateDocumentoService.remover(id, usuario);
        return ResponseEntity.noContent().build();
    }

    public record CriarTemplateDocumentoRequest(
            Long empresaId,
            Long clientePessoaFisicaId,
            @NotBlank String nome,
            boolean obrigatorio
    ) {
    }

    public record AtualizarTemplateDocumentoRequest(
            @NotBlank String nome,
            boolean obrigatorio
    ) {
    }

    public record TemplateDocumentoResponse(
            Long id,
            Long empresaId,
            Long clientePessoaFisicaId,
            String nome,
            boolean obrigatorio
    ) {
        public static TemplateDocumentoResponse fromEntity(TemplateDocumento template) {
            return new TemplateDocumentoResponse(
                    template.getId(),
                    template.getEmpresa() != null ? template.getEmpresa().getId() : null,
                    template.getClientePessoaFisica() != null ? template.getClientePessoaFisica().getId() : null,
                    template.getNome(),
                    template.isObrigatorio()
            );
        }
    }
}
