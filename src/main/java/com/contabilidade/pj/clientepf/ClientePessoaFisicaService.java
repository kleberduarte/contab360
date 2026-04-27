package com.contabilidade.pj.clientepf;

import com.contabilidade.pj.auth.entity.PerfilUsuario;
import com.contabilidade.pj.auth.entity.Usuario;
import com.contabilidade.pj.auth.repository.UsuarioRepository;
import com.contabilidade.pj.pendencia.repository.PendenciaDocumentoRepository;
import com.contabilidade.pj.pendencia.repository.TemplateDocumentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientePessoaFisicaService {

    private final ClientePessoaFisicaRepository clientePessoaFisicaRepository;
    private final PendenciaDocumentoRepository pendenciaDocumentoRepository;
    private final TemplateDocumentoRepository templateDocumentoRepository;
    private final UsuarioRepository usuarioRepository;

    public ClientePessoaFisicaService(
            ClientePessoaFisicaRepository clientePessoaFisicaRepository,
            PendenciaDocumentoRepository pendenciaDocumentoRepository,
            TemplateDocumentoRepository templateDocumentoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.clientePessoaFisicaRepository = clientePessoaFisicaRepository;
        this.pendenciaDocumentoRepository = pendenciaDocumentoRepository;
        this.templateDocumentoRepository = templateDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientePessoaFisica> listar(Usuario usuarioAtual, boolean incluirInativas) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            if (usuarioAtual.getClientePessoaFisica() == null) {
                return List.of();
            }
            return clientePessoaFisicaRepository
                    .findById(usuarioAtual.getClientePessoaFisica().getId())
                    .stream()
                    .toList();
        }
        if (incluirInativas) {
            return clientePessoaFisicaRepository.findAll();
        }
        return clientePessoaFisicaRepository.findAllByAtivoTrueOrderByNomeCompletoAsc();
    }

    @Transactional
    public ClientePessoaFisica criar(ClientePessoaFisica dados, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() == PerfilUsuario.CLIENTE) {
            throw new IllegalArgumentException("Cliente não pode cadastrar pessoa física.");
        }
        String cpf = normalizarCpf(dados.getCpf());
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
        }
        if (clientePessoaFisicaRepository.findByCpf(cpf).isPresent()) {
            throw new IllegalArgumentException("Já existe cadastro com este CPF.");
        }
        if (dados.getNomeCompleto() == null || dados.getNomeCompleto().isBlank()) {
            throw new IllegalArgumentException("Nome completo é obrigatório.");
        }
        ClientePessoaFisica ent = new ClientePessoaFisica();
        ent.setCpf(cpf);
        ent.setNomeCompleto(dados.getNomeCompleto().trim());
        ent.setAtivo(true);
        return clientePessoaFisicaRepository.save(ent);
    }

    @Transactional
    public ClientePessoaFisica atualizar(Long id, ClientePessoaFisica dados, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode editar cadastro PF.");
        }
        ClientePessoaFisica existente = clientePessoaFisicaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
        if (!existente.isAtivo()) {
            throw new IllegalArgumentException("Cadastro inativo. Reative antes de alterar os dados.");
        }
        String cpf = normalizarCpf(dados.getCpf());
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
        }
        if (!existente.getCpf().equals(cpf) && clientePessoaFisicaRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new IllegalArgumentException("Já existe outro cadastro com este CPF.");
        }
        if (dados.getNomeCompleto() == null || dados.getNomeCompleto().isBlank()) {
            throw new IllegalArgumentException("Nome completo é obrigatório.");
        }
        existente.setCpf(cpf);
        existente.setNomeCompleto(dados.getNomeCompleto().trim());
        return clientePessoaFisicaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode desativar cadastro PF.");
        }
        ClientePessoaFisica ent = clientePessoaFisicaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
        if (!ent.isAtivo()) {
            return;
        }
        if (pendenciaDocumentoRepository.countByClientePessoaFisica_Id(id) > 0) {
            throw new IllegalArgumentException("Não é possível desativar: existem pendências vinculadas.");
        }
        if (templateDocumentoRepository.countByClientePessoaFisica_Id(id) > 0) {
            throw new IllegalArgumentException("Não é possível desativar: existem templates vinculados.");
        }
        if (usuarioRepository.countByClientePessoaFisica_Id(id) > 0) {
            throw new IllegalArgumentException("Não é possível desativar: existem usuários vinculados.");
        }
        ent.setAtivo(false);
        clientePessoaFisicaRepository.save(ent);
    }

    @Transactional
    public ClientePessoaFisica reativar(Long id, Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador pode reativar cadastro PF.");
        }
        ClientePessoaFisica ent = clientePessoaFisicaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro PF não encontrado."));
        if (ent.isAtivo()) {
            return ent;
        }
        ent.setAtivo(true);
        return clientePessoaFisicaRepository.save(ent);
    }

    private static String normalizarCpf(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "");
    }
}
