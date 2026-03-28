package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendenciaDocumentoService {

    private final CompetenciaMensalRepository competenciaMensalRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final EmpresaRepository empresaRepository;
    private final CompetenciaArquivamentoService competenciaArquivamentoService;

    public PendenciaDocumentoService(
            CompetenciaMensalRepository competenciaMensalRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            EmpresaRepository empresaRepository,
            CompetenciaArquivamentoService competenciaArquivamentoService
    ) {
        this.competenciaMensalRepository = competenciaMensalRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.empresaRepository = empresaRepository;
        this.competenciaArquivamentoService = competenciaArquivamentoService;
    }

    @Transactional
    public int gerarPendencias(Integer ano, Integer mes, Integer diaVencimento, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode gerar pendências.");
        }
        if (ano == null || mes == null) {
            throw new IllegalArgumentException("Ano e mês são obrigatórios.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }
        if (diaVencimento == null || diaVencimento < 1 || diaVencimento > 31) {
            throw new IllegalArgumentException("Dia de vencimento inválido.");
        }

        CompetenciaMensal competencia = competenciaMensalRepository
                .findByAnoAndMes(ano, mes)
                .orElseGet(() -> {
                    CompetenciaMensal nova = new CompetenciaMensal();
                    nova.setAno(ano);
                    nova.setMes(mes);
                    return competenciaMensalRepository.save(nova);
                });

        List<Empresa> empresas = empresaRepository.findAll();
        int totalCriadas = 0;

        for (Empresa empresa : empresas) {
            List<TemplateDocumento> templates = templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId());
            for (TemplateDocumento template : templates) {
                boolean existe = pendenciaDocumentoRepository
                        .findByEmpresaIdAndTemplateDocumentoIdAndCompetenciaId(
                                empresa.getId(),
                                template.getId(),
                                competencia.getId()
                        )
                        .isPresent();
                if (existe) {
                    continue;
                }
                PendenciaDocumento pendencia = new PendenciaDocumento();
                pendencia.setEmpresa(empresa);
                pendencia.setTemplateDocumento(template);
                pendencia.setCompetencia(competencia);
                pendencia.setStatus(PendenciaStatus.PENDENTE);
                pendencia.setVencimento(calcularVencimento(ano, mes, diaVencimento));
                pendenciaDocumentoRepository.save(pendencia);
                totalCriadas++;
            }
        }
        if (totalCriadas > 0) {
            competenciaArquivamentoService.sincronizarArquivamentoCompetencia(competencia.getId());
        }
        return totalCriadas;
    }

    @Transactional(readOnly = true)
    public List<PendenciaDocumento> listar(
            Integer ano,
            Integer mes,
            Long empresaId,
            PendenciaStatus status,
            Usuario usuarioAtual,
            boolean incluirArquivadas
    ) {
        if (ano == null || mes == null) {
            throw new IllegalArgumentException("Ano e mês são obrigatórios.");
        }

        Long empresaFiltrada = empresaId;
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getEmpresa() == null) {
                return List.of();
            }
            empresaFiltrada = usuarioAtual.getEmpresa().getId();
        }

        if (usuarioAtual.getPerfil() == PerfilUsuario.CONTADOR && !incluirArquivadas) {
            if (competenciaMensalRepository.findByAnoAndMes(ano, mes).map(CompetenciaMensal::isArquivada).orElse(false)) {
                return List.of();
            }
        }

        List<PendenciaDocumento> pendencias = empresaFiltrada == null
                ? pendenciaDocumentoRepository
                        .findByCompetenciaAnoAndCompetenciaMesOrderByEmpresaRazaoSocialAscTemplateDocumentoNomeAsc(ano, mes)
                : pendenciaDocumentoRepository
                        .findByCompetenciaAnoAndCompetenciaMesAndEmpresaIdOrderByTemplateDocumentoNomeAsc(
                                ano,
                                mes,
                                empresaFiltrada
                        );

        if (status == null) {
            return pendencias;
        }

        List<PendenciaDocumento> filtradas = new ArrayList<>();
        for (PendenciaDocumento pendencia : pendencias) {
            if (pendencia.getStatus() == status) {
                filtradas.add(pendencia);
            }
        }
        return filtradas;
    }

    @Transactional
    public PendenciaDocumento atualizarStatus(Long pendenciaId, PendenciaStatus novoStatus, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode validar/rejeitar pendências.");
        }
        if (novoStatus == null) {
            throw new IllegalArgumentException("Status é obrigatório.");
        }
        PendenciaDocumento pendencia = pendenciaDocumentoRepository.findById(pendenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Pendência não encontrada."));
        pendencia.setStatus(novoStatus);
        PendenciaDocumento salva = pendenciaDocumentoRepository.save(pendencia);
        competenciaArquivamentoService.sincronizarArquivamentoCompetencia(pendencia.getCompetencia().getId());
        return salva;
    }

    private LocalDate calcularVencimento(Integer ano, Integer mes, Integer diaVencimento) {
        int ultimoDia = LocalDate.of(ano, mes, 1).lengthOfMonth();
        int diaSeguro = Math.min(diaVencimento, ultimoDia);
        try {
            return LocalDate.of(ano, mes, diaSeguro);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Competência inválida.");
        }
    }

    @Transactional(readOnly = true)
    public CompetenciaArquivoInfo obterInfoArquivoCompetencia(Integer ano, Integer mes, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            return new CompetenciaArquivoInfo(false, false, null);
        }
        return competenciaMensalRepository.findByAnoAndMes(ano, mes)
                .map(c -> new CompetenciaArquivoInfo(true, c.isArquivada(), c.getArquivadaEm()))
                .orElse(new CompetenciaArquivoInfo(false, false, null));
    }

    public record CompetenciaArquivoInfo(boolean existeCompetencia, boolean arquivada, LocalDateTime arquivadaEm) {
    }
}
