package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.clientepf.ClientePessoaFisica;
import com.contabilidade.pj.clientepf.ClientePessoaFisicaRepository;
import com.contabilidade.pj.empresa.entity.Empresa;
import com.contabilidade.pj.empresa.repository.EmpresaRepository;
import com.contabilidade.pj.ia.service.AuditoriaService;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.contabilidade.pj.pendencia.entity.*;
import com.contabilidade.pj.pendencia.repository.*;

@Service
public class TemplateDocumentoService {

    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final EmpresaRepository empresaRepository;
    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository;
    private final RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository;
    private final DocumentoProcessamentoRepository documentoProcessamentoRepository;
    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final DocTabMapperService docTabMapperService;
    private final AuditoriaService auditoriaService;

    public TemplateDocumentoService(
            TemplateDocumentoRepository templateDocumentoRepository,
            EmpresaRepository empresaRepository,
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            DocumentoDadoExtraidoRepository documentoDadoExtraidoRepository,
            RevisaoDocumentoHistoricoRepository revisaoDocumentoHistoricoRepository,
            DocumentoProcessamentoRepository documentoProcessamentoRepository,
            EntregaDocumentoRepository entregaDocumentoRepository,
            DocTabMapperService docTabMapperService,
            AuditoriaService auditoriaService
    ) {
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.documentoDadoExtraidoRepository = documentoDadoExtraidoRepository;
        this.revisaoDocumentoHistoricoRepository = revisaoDocumentoHistoricoRepository;
        this.documentoProcessamentoRepository = documentoProcessamentoRepository;
        this.entregaDocumentoRepository = entregaDocumentoRepository;
        this.docTabMapperService = docTabMapperService;
        this.auditoriaService = auditoriaService;
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

    @Transactional(readOnly = true)
    public List<String> listarSugestoesNomes() {
        return docTabMapperService.ordemAbas().stream()
                .filter(id -> !"OUTROS".equals(id))
                .map(docTabMapperService::tituloAba)
                .toList();
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
        String nomeNormalizado = nome != null ? nome.trim() : "";
        if (nomeNormalizado.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do documento.");
        }

        TemplateDocumento template = new TemplateDocumento();
        template.setNome(nomeNormalizado);
        template.setObrigatorio(obrigatorio);

        if (temEmpresa) {
            Empresa empresa = empresaRepository
                    .findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
            if (!empresa.isAtivo()) {
                throw new IllegalArgumentException("Empresa inativa. Reative-a antes de cadastrar template.");
            }
            if (templateDocumentoRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nomeNormalizado)) {
                throw new IllegalArgumentException("Já existe um template com esse nome para esta empresa.");
            }
            template.setEmpresa(empresa);
        } else {
            ClientePessoaFisica pf = clientePessoaFisicaRepository
                    .findById(clientePessoaFisicaId)
                    .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
            if (!pf.isAtivo()) {
                throw new IllegalArgumentException("Cadastro PF inativo. Reative antes de cadastrar template.");
            }
            if (templateDocumentoRepository
                    .existsByClientePessoaFisicaIdAndNomeIgnoreCase(clientePessoaFisicaId, nomeNormalizado)) {
                throw new IllegalArgumentException("Já existe um template com esse nome para esta pessoa física.");
            }
            template.setClientePessoaFisica(pf);
        }

        try {
            return templateDocumentoRepository.save(template);
        } catch (DataIntegrityViolationException ex) {
            throw traduzirErroPersistencia(ex);
        }
    }

    @Transactional
    public TemplateDocumento atualizar(Long id, String nome, boolean obrigatorio, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode atualizar template.");
        }
        TemplateDocumento template = templateDocumentoRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template não encontrado."));
        String nomeNormalizado = nome != null ? nome.trim() : "";
        if (nomeNormalizado.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do documento.");
        }

        if (template.getEmpresa() != null) {
            Long empresaId = template.getEmpresa().getId();
            if (templateDocumentoRepository.existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(empresaId, nomeNormalizado, id)) {
                throw new IllegalArgumentException("Já existe um template com esse nome para esta empresa.");
            }
        } else if (template.getClientePessoaFisica() != null) {
            Long clientePessoaFisicaId = template.getClientePessoaFisica().getId();
            if (templateDocumentoRepository.existsByClientePessoaFisicaIdAndNomeIgnoreCaseAndIdNot(
                    clientePessoaFisicaId, nomeNormalizado, id)) {
                throw new IllegalArgumentException("Já existe um template com esse nome para esta pessoa física.");
            }
        }

        template.setNome(nomeNormalizado);
        template.setObrigatorio(obrigatorio);
        try {
            return templateDocumentoRepository.save(template);
        } catch (DataIntegrityViolationException ex) {
            throw traduzirErroPersistencia(ex);
        }
    }

    @Transactional
    public void remover(Long id, Usuario usuarioAtual) {
        boolean isContador = usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR;
        boolean isAdm = usuarioAtual.getPerfil() == PerfilUsuario.ADM;
        if (!isContador && !isAdm) {
            throw new IllegalArgumentException("Apenas contador ou ADM pode excluir template.");
        }
        TemplateDocumento template = templateDocumentoRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template não encontrado."));
        long totalPendencias = pendenciaDocumentoRepository.countByTemplateDocumento_Id(id);
        if (isContador && totalPendencias > 0) {
            throw new IllegalArgumentException("Não é possível excluir: existem pendências vinculadas a este template.");
        }
        if (isAdm && totalPendencias > 0) {
            removerPendenciasECadeiaPorTemplate(id);
        }
        templateDocumentoRepository.delete(template);
        if (isAdm) {
            auditoriaService.registrarEventoAdm(
                    "DELETE",
                    "/api/templates-documentos/" + id + "?modo=forcado-adm&pendenciasRemovidas=" + totalPendencias,
                    usuarioAtual,
                    204
            );
        }
    }

    /**
     * Remove pendências do template e toda a cadeia (dados extraídos, histórico de revisão,
     * processamento, entregas) para satisfazer FKs antes de excluir o template.
     */
    private void removerPendenciasECadeiaPorTemplate(Long templateId) {
        List<Long> pendenciaIds = pendenciaDocumentoRepository.findIdsByTemplateDocumento_Id(templateId);
        if (pendenciaIds.isEmpty()) {
            return;
        }
        documentoDadoExtraidoRepository.deleteByProcessamento_Entrega_Pendencia_IdIn(pendenciaIds);
        revisaoDocumentoHistoricoRepository.deleteByProcessamento_Entrega_Pendencia_IdIn(pendenciaIds);
        documentoProcessamentoRepository.deleteByEntrega_Pendencia_IdIn(pendenciaIds);
        entregaDocumentoRepository.deleteByPendencia_IdIn(pendenciaIds);
        pendenciaDocumentoRepository.deleteByTemplateDocumento_Id(templateId);
    }

    private IllegalArgumentException traduzirErroPersistencia(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String normalizada = msg == null ? "" : msg.toLowerCase(Locale.ROOT);
        if (normalizada.contains("duplicate")
                || normalizada.contains("duplic")
                || normalizada.contains("unique")
                || normalizada.contains("uniq_")) {
            return new IllegalArgumentException("Já existe um template com esses dados. Verifique nome e tomador.");
        }
        if (normalizada.contains("foreign key")
                || normalizada.contains("constraint fails")
                || normalizada.contains("references")) {
            return new IllegalArgumentException("Falha ao salvar template: tomador inválido ou relacionamento inexistente no banco.");
        }
        if (normalizada.contains("cannot be null")
                || normalizada.contains("null value")) {
            return new IllegalArgumentException("Falha ao salvar template: um campo obrigatório ficou nulo no banco.");
        }
        if (normalizada.contains("data too long")
                || normalizada.contains("value too long")) {
            return new IllegalArgumentException("Falha ao salvar template: nome do template excede o tamanho permitido.");
        }
        if (normalizada.contains("incorrect string value")
                || normalizada.contains("character set")
                || normalizada.contains("collation")) {
            return new IllegalArgumentException("Falha ao salvar template: erro de codificação/charset no banco.");
        }
        return new IllegalArgumentException("Falha ao salvar template. Verifique os dados do tomador e tente novamente.");
    }
}
