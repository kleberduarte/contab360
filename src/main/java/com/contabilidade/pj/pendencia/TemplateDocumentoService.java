package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateDocumentoService {

    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final EmpresaRepository empresaRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;

    public TemplateDocumentoService(
            TemplateDocumentoRepository templateDocumentoRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository
    ) {
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
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

    @Transactional(readOnly = true)
    public List<TemplateDocumento> listarPorClientePessoaFisica(Long clientePessoaFisicaId, Usuario usuarioAtual) {
        Long idPermitido = clientePessoaFisicaId;
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getClientePessoaFisica() == null) {
                return List.of();
            }
            idPermitido = usuarioAtual.getClientePessoaFisica().getId();
        }
        return templateDocumentoRepository.findByClientePessoaFisicaIdOrderByNomeAsc(idPermitido);
    }

    @Transactional
    public TemplateDocumento criar(
            Long empresaId,
            Long clientePessoaFisicaId,
            String nome,
            boolean obrigatorio,
            Usuario usuarioAtual
    ) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode cadastrar template.");
        }
        boolean temEmpresa = empresaId != null;
        boolean temPf = clientePessoaFisicaId != null;
        if (temEmpresa == temPf) {
            throw new IllegalArgumentException("Informe empresaId ou clientePessoaFisicaId (apenas um).");
        }

        TemplateDocumento template = new TemplateDocumento();
        template.setNome(nome);
        template.setObrigatorio(obrigatorio);

        if (temEmpresa) {
            Empresa empresa = empresaRepository
                    .findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
            if (!empresa.isAtivo()) {
                throw new IllegalArgumentException("Empresa inativa. Reative-a antes de cadastrar template.");
            }
            template.setEmpresa(empresa);
        } else {
            ClientePessoaFisica pf = clientePessoaFisicaRepository
                    .findById(clientePessoaFisicaId)
                    .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
            if (!pf.isAtivo()) {
                throw new IllegalArgumentException("Cadastro PF inativo. Reative antes de cadastrar template.");
            }
            template.setClientePessoaFisica(pf);
        }

        return templateDocumentoRepository.save(template);
    }
}
