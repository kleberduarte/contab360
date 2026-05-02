package com.contabilidade.pj.empresa.service;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.repository.UsuarioRepository;
import com.contabilidade.pj.fiscal.repository.CertificadoDigitalPedidoRepository;
import com.contabilidade.pj.pendencia.repository.DocumentoDadoExtraidoRepository;
import com.contabilidade.pj.pendencia.repository.DocumentoProcessamentoRepository;
import com.contabilidade.pj.pendencia.repository.EntregaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.PendenciaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.RevisaoDocumentoHistoricoRepository;
import com.contabilidade.pj.pendencia.repository.TemplateDocumentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.contabilidade.pj.empresa.entity.*;
import com.contabilidade.pj.empresa.repository.*;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;
    private final DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository;
    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final CertificadoDigitalPedidoRepository certificadoDigitalPedidoRepository;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository,
            DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository,
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            EntregaDocumentoRepository entregaDocumentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            CertificadoDigitalPedidoRepository certificadoDigitalPedidoRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
        this.documentoDadoExtraidoRepository = documentoDadoExtraidoRepository;
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.entregaDocumentoRepository = entregaDocumentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.certificadoDigitalPedidoRepository = certificadoDigitalPedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<Empresa> listar(Usuario usuarioAtual, boolean incluirInativas) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() == null) {
                return List.of();
            }
            return empresaRepository.findById(usuarioAtual.getEmpresa().getId()).stream().toList();
        }
        if (incluirInativas) {
            return empresaRepository.findAll();
        }
        return empresaRepository.findAllByAtivoTrue();
    }

    @Transactional
    public Empresa criar(Empresa empresa, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Cliente não pode cadastrar empresa.");
        }
        String cnpjDigits = empresa.getCnpj() == null ? "" : empresa.getCnpj().replaceAll("\\D", "");
        if (cnpjDigits.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos.");
        }
        if (empresa.getRazaoSocial() == null || empresa.getRazaoSocial().isBlank()) {
            throw new IllegalArgumentException("Razão social é obrigatória.");
        }
        empresa.setCnpj(cnpjDigits);
        empresa.setRazaoSocial(empresa.getRazaoSocial().trim());
        if (empresaRepository.existsByCnpj(cnpjDigits)) {
            throw new IllegalArgumentException(
                    "Já existe empresa com este CNPJ (ativa ou inativa). Reative o cadastro existente (opção \"Mostrar empresas inativas\") ou informe outro CNPJ.");
        }
        String cpfDigits = empresa.getCpfResponsavel() == null ? "" : empresa.getCpfResponsavel().replaceAll("\\D", "");
        if (cpfDigits.isEmpty()) {
            empresa.setCpfResponsavel(null);
        } else if (cpfDigits.length() != 11) {
            throw new IllegalArgumentException("CPF do responsável deve ter 11 dígitos.");
        } else {
            empresa.setCpfResponsavel(cpfDigits);
        }
        empresa.setAtivo(true);
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa atualizar(Long id, Empresa dados, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR && usuarioAtual.getPerfil() != PerfilUsuario.ADM) {
            throw new IllegalArgumentException("Sem permissão para editar empresa.");
        }
        Empresa existente = empresaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
        if (!existente.isAtivo()) {
            throw new IllegalArgumentException("Empresa inativa. Reative-a antes de alterar os dados.");
        }
        String novoCnpj = dados.getCnpj() == null ? "" : dados.getCnpj().replaceAll("\\D", "");
        if (novoCnpj.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos.");
        }
        if (!existente.getCnpj().equals(novoCnpj) && empresaRepository.existsByCnpjAndIdNot(novoCnpj, id)) {
            throw new IllegalArgumentException("Já existe outra empresa com este CNPJ.");
        }
        existente.setCnpj(novoCnpj);
        if (dados.getRazaoSocial() == null || dados.getRazaoSocial().isBlank()) {
            throw new IllegalArgumentException("Razão social é obrigatória.");
        }
        existente.setRazaoSocial(dados.getRazaoSocial().trim());
        String cpfDigits = dados.getCpfResponsavel() == null ? "" : dados.getCpfResponsavel().replaceAll("\\D", "");
        if (cpfDigits.isEmpty()) {
            existente.setCpfResponsavel(null);
        } else if (cpfDigits.length() != 11) {
            throw new IllegalArgumentException("CPF do responsável deve ter 11 dígitos.");
        } else {
            existente.setCpfResponsavel(cpfDigits);
        }
        existente.setMei(dados.isMei());
        existente.setVencimentoDas(dados.getVencimentoDas());
        existente.setVencimentoCertificadoMei(dados.getVencimentoCertificadoMei());
        return empresaRepository.save(existente);
    }

    /**
     * Contador: exclusão lógica. Administrador: remove empresa, templates, pendências (e filhos) e pedidos de certificado;
     * usuários com este vínculo ficam sem empresa.
     */
    @Transactional
    public void excluir(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR && usuarioAtual.getPerfil() != PerfilUsuario.ADM) {
            throw new IllegalArgumentException("Sem permissão para desativar empresa.");
        }
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
        if (usuarioAtual.getPerfil() == PerfilUsuario.ADM) {
            excluirEmpresaDefinitivamente(id);
            return;
        }
        if (empresa.isAtivo()) {
            empresaRepository.atualizarAtivo(id, false);
        }
    }

    private void excluirEmpresaDefinitivamente(Long empresaId) {
        List<Long> pendenciaIds = pendenciaDocumentoRepository.findIdsByEmpresa_Id(empresaId);
        if (!pendenciaIds.isEmpty()) {
            revisaoDocumentoHistoricoRepository.deleteByProcessamento_Entrega_Pendencia_IdIn(pendenciaIds);
            documentoDadoExtraidoRepository.deleteByProcessamento_Entrega_Pendencia_IdIn(pendenciaIds);
            documentoProcessamentoRepository.deleteByEntrega_Pendencia_IdIn(pendenciaIds);
            entregaDocumentoRepository.deleteByPendencia_IdIn(pendenciaIds);
            pendenciaDocumentoRepository.deleteByEmpresa_Id(empresaId);
        }
        templateDocumentoRepository.deleteByEmpresa_Id(empresaId);
        certificadoDigitalPedidoRepository.deleteByEmpresa_Id(empresaId);
        usuarioRepository.clearEmpresaByEmpresaId(empresaId);
        empresaRepository.deleteById(empresaId);
    }

    @Transactional
    public Empresa reativar(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR && usuarioAtual.getPerfil() != PerfilUsuario.ADM) {
            throw new IllegalArgumentException("Sem permissão para reativar empresa.");
        }
        Empresa empresa = empresaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
        if (!empresa.isAtivo()) {
            empresaRepository.atualizarAtivo(id, true);
            empresa.setAtivo(true);
        }
        return empresa;
    }
}
