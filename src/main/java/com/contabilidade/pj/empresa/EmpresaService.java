package com.contabilidade.pj.empresa;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
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
        empresa.setAtivo(true);
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa atualizar(Long id, Empresa dados, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode editar empresa.");
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

    /** Exclusão lógica: empresa deixa de ser listada nas operações correntes; histórico e vínculos permanecem. */
    @Transactional
    public void excluir(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode desativar empresa.");
        }
        Empresa empresa = empresaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
        if (!empresa.isAtivo()) {
            return;
        }
        empresa.setAtivo(false);
        empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa reativar(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode reativar empresa.");
        }
        Empresa empresa = empresaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada."));
        if (empresa.isAtivo()) {
            return empresa;
        }
        empresa.setAtivo(true);
        return empresaRepository.save(empresa);
    }
}
