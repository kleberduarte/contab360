package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateDocumentoService {

    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final EmpresaRepository empresaRepository;

    public TemplateDocumentoService(
            TemplateDocumentoRepository templateDocumentoRepository,
            EmpresaRepository empresaRepository
    ) {
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateDocumento> listarPorEmpresa(Long empresaId, Usuario usuarioAtual) {
        Long empresaPermitida = empresaId;
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() == null) {
                return List.of();
            }
            empresaPermitida = usuarioAtual.getEmpresa().getId();
        }
        return templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(empresaPermitida);
    }

    @Transactional
    public TemplateDocumento criar(Long empresaId, String nome, boolean obrigatorio, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode cadastrar template.");
        }
        Empresa empresa = empresaRepository
                .findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));

        TemplateDocumento template = new TemplateDocumento();
        template.setEmpresa(empresa);
        template.setNome(nome);
        template.setObrigatorio(obrigatorio);
        return templateDocumentoRepository.save(template);
    }
}
