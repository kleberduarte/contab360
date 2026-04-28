package com.contabilidade.pj.lgpd;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.repository.SessaoAcessoRepository;
import com.contabilidade.pj.auth.repository.UsuarioRepository;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LgpdService {

    static final String VERSAO_POLITICA_ATUAL = "v1.0";

    private final ConsentimentoTitularRepository consentimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessaoAcessoRepository sessaoAcessoRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final EmpresaRepository empresaRepository;

    public LgpdService(
            ConsentimentoTitularRepository consentimentoRepository,
            UsuarioRepository usuarioRepository,
            SessaoAcessoRepository sessaoAcessoRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            EmpresaRepository empresaRepository) {
        this.consentimentoRepository = consentimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.sessaoAcessoRepository = sessaoAcessoRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.empresaRepository = empresaRepository;
    }

    /** Verifica se o usuário já consentiu com a versão atual da política. */
    @Transactional(readOnly = true)
    public boolean consentimentoPendente(Long usuarioId) {
        return !consentimentoRepository.existsByUsuario_IdAndVersaoPolitica(usuarioId, VERSAO_POLITICA_ATUAL);
    }

    /** Registra o aceite da política de privacidade pelo titular. */
    @Transactional
    public void registrarConsentimento(Usuario usuario, HttpServletRequest request) {
        if (consentimentoRepository.existsByUsuario_IdAndVersaoPolitica(usuario.getId(), VERSAO_POLITICA_ATUAL)) {
            return;
        }
        ConsentimentoTitular c = new ConsentimentoTitular();
        c.setUsuario(usuario);
        c.setDataHora(LocalDateTime.now());
        c.setVersaoPolitica(VERSAO_POLITICA_ATUAL);
        c.setIpOrigem(resolverIp(request));
        consentimentoRepository.save(c);
    }

    /**
     * Direito ao esquecimento (LGPD art. 18, IV): anonimiza os dados pessoais
     * do titular. Dados fiscais/contábeis são mantidos por obrigação legal,
     * mas desvinculados de identificação pessoal.
     *
     * Apenas ADM pode acionar para qualquer titular; o próprio CLIENTE pode solicitar
     * para si mesmo.
     */
    @Transactional
    public void aplicarEsquecimento(Long usuarioAlvoId, Usuario solicitante) {
        boolean eSiMesmo = solicitante.getId().equals(usuarioAlvoId);
        boolean eAdmin = solicitante.getPerfil() == PerfilUsuario.ADM;
        if (!eSiMesmo && !eAdmin) {
            throw new IllegalArgumentException("Sem permissão para aplicar direito ao esquecimento.");
        }

        Usuario alvo = usuarioRepository.findById(usuarioAlvoId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        String sufixo = "anonimizado-" + alvo.getId();

        // Anonimizar dados do usuário
        alvo.setNome("Titular Anonimizado");
        alvo.setEmail(sufixo + "@esquecimento.lgpd");
        alvo.setSenhaHash("$2a$10$LGPD_ANONIMIZADO_SEM_ACESSO");
        alvo.setAtivo(false);
        usuarioRepository.save(alvo);

        // Encerrar todas as sessões
        sessaoAcessoRepository.deleteByUsuario_Id(alvo.getId());

        // Anonimizar cadastro PF vinculado (se houver e não tiver outros vínculos ativos)
        ClientePessoaFisica cpf = alvo.getClientePessoaFisica();
        if (cpf != null) {
            cpf.setNomeCompleto("Anonimizado " + sufixo);
            cpf.setCpf(gerarCpfAnonimizado(cpf.getId()));
            cpf.setAtivo(false);
            clientePessoaFisicaRepository.save(cpf);
        }

        // Anonimizar empresa vinculada (apenas se for MEI — responsabilidade pessoal)
        Empresa empresa = alvo.getEmpresa();
        if (empresa != null && empresa.isMei()) {
            empresa.setCpfResponsavel(null);
            empresaRepository.save(empresa);
        }
    }

    /** Gera um CPF-placeholder único e inválido para anonimização. */
    private static String gerarCpfAnonimizado(Long id) {
        // "00000" + id com 6 dígitos (total 11) — sempre inválido como CPF real.
        return String.format("00000%06d", id % 1_000_000L);
    }

    private static String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
