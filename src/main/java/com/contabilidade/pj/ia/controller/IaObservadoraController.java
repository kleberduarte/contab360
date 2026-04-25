package com.contabilidade.pj.ia.controller;

import com.contabilidade.pj.auth.service.AuthContext;
import com.contabilidade.pj.auth.entity.Usuario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.contabilidade.pj.ia.service.IaObservadoraService;
import com.contabilidade.pj.ia.service.AuditoriaService;

@RestController
@RequestMapping("/api/ia-observadora")
public class IaObservadoraController {

    private final IaObservadoraService iaObservadoraService;

    public IaObservadoraController(IaObservadoraService iaObservadoraService) {
        this.iaObservadoraService = iaObservadoraService;
    }

    @GetMapping("/insights")
    public IaObservadoraService.IaObservadoraResponse insights() {
        Usuario usuario = AuthContext.get();
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario nao autenticado.");
        }
        return iaObservadoraService.analisar(usuario);
    }
}
