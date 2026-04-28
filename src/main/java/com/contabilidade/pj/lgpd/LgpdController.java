package com.contabilidade.pj.lgpd;

import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.service.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lgpd")
public class LgpdController {

    private final LgpdService lgpdService;

    public LgpdController(LgpdService lgpdService) {
        this.lgpdService = lgpdService;
    }

    /** Verifica se o usuário autenticado ainda precisa consentir com a política atual. */
    @GetMapping("/consentimento/pendente")
    public ConsentimentoPendenteResponse consentimentoPendente() {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        boolean pendente = lgpdService.consentimentoPendente(usuario.getId());
        return new ConsentimentoPendenteResponse(pendente, LgpdService.VERSAO_POLITICA_ATUAL);
    }

    /** Registra o aceite da política de privacidade pelo titular autenticado. */
    @PostMapping("/consentimento")
    public ResponseEntity<Void> registrarConsentimento(HttpServletRequest request) {
        Usuario usuario = AuthContext.get();
        if (usuario == null) throw new IllegalArgumentException("Usuário não autenticado.");
        lgpdService.registrarConsentimento(usuario, request);
        return ResponseEntity.noContent().build();
    }

    /** Retorna o texto da política de privacidade vigente. */
    @GetMapping("/politica-privacidade")
    public PoliticaPrivacidadeResponse politicaPrivacidade() {
        return new PoliticaPrivacidadeResponse(LgpdService.VERSAO_POLITICA_ATUAL, POLITICA_TEXTO);
    }

    /**
     * Direito ao esquecimento: anonimiza os dados pessoais do titular indicado.
     * O próprio titular pode solicitar para si mesmo; ADM pode solicitar para qualquer um.
     */
    @DeleteMapping("/esquecimento/{usuarioId}")
    public ResponseEntity<Void> aplicarEsquecimento(@PathVariable Long usuarioId) {
        Usuario solicitante = AuthContext.get();
        if (solicitante == null) throw new IllegalArgumentException("Usuário não autenticado.");
        lgpdService.aplicarEsquecimento(usuarioId, solicitante);
        return ResponseEntity.noContent().build();
    }

    public record ConsentimentoPendenteResponse(boolean pendente, String versaoPolitica) {}
    public record PoliticaPrivacidadeResponse(String versao, String texto) {}

    private static final String POLITICA_TEXTO = """
            POLÍTICA DE PRIVACIDADE — CONTAB360
            Versão 1.0 | Vigente a partir de 28/04/2026

            1. QUEM SOMOS
            O Contab360 é um sistema de gestão contábil que processa dados pessoais de \
            titulares (clientes e responsáveis por empresas) para prestação de serviços \
            contábeis, fiscais e trabalhistas.

            2. DADOS COLETADOS
            Coletamos: nome completo, CPF, CNPJ, endereço de e-mail e documentos enviados \
            pelo titular (holerites, notas fiscais e outros documentos contábeis).

            3. FINALIDADE E BASE LEGAL
            Os dados são tratados com base nas seguintes hipóteses legais (LGPD art. 7.º):
            • Execução de contrato: para prestar os serviços contratados;
            • Obrigação legal: para cumprimento de obrigações fiscais e trabalhistas;
            • Legítimo interesse: para melhoria e segurança do sistema.

            4. COMPARTILHAMENTO
            Dados podem ser compartilhados com a Receita Federal, prefeituras municipais \
            e outros órgãos públicos exigidos por lei. Não vendemos dados a terceiros.

            5. RETENÇÃO
            Dados fiscais e contábeis são retidos pelo período legal mínimo de 5 anos. \
            Demais dados pessoais são excluídos ou anonimizados mediante solicitação \
            (direito ao esquecimento), respeitados os prazos legais.

            6. SEGURANÇA
            CPF e CNPJ são armazenados criptografados (AES-256). O acesso é protegido \
            por autenticação e controle de perfis.

            7. SEUS DIREITOS (LGPD art. 18)
            Você pode solicitar: acesso, correção, portabilidade, anonimização, \
            bloqueio ou eliminação dos seus dados. Entre em contato com o responsável \
            pelo seu escritório contábil ou pelo e-mail de suporte do sistema.

            8. CONTATO
            Dúvidas sobre esta política? Contate o Encarregado pelo tratamento de dados (DPO) \
            pelo canal indicado no seu contrato de prestação de serviços contábeis.
            """;
}
