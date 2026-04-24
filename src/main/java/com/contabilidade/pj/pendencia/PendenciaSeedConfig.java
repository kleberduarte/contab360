package com.contabilidade.pj.pendencia;

import com.contabilidade.pj.empresa.Empresa;
import com.contabilidade.pj.empresa.EmpresaRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PendenciaSeedConfig {

    @Bean
    CommandLineRunner seedTemplatesIniciais(
            EmpresaRepository empresaRepository,
            TemplateDocumentoRepository templateDocumentoRepository
    ) {
        return args -> {
            List<Empresa> empresas = empresaRepository.findAll();
            for (Empresa empresa : empresas) {
                boolean temTemplate = !templateDocumentoRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId()).isEmpty();
                if (temTemplate) {
                    continue;
                }
                criarTemplate(templateDocumentoRepository, empresa, "Extrato bancário");
                criarTemplate(templateDocumentoRepository, empresa, "Notas fiscais emitidas");
                criarTemplate(templateDocumentoRepository, empresa, "Notas fiscais recebidas");
            }
        };
    }

    private void criarTemplate(
            TemplateDocumentoRepository templateDocumentoRepository,
            Empresa empresa,
            String nome
    ) {
        TemplateDocumento template = new TemplateDocumento();
        template.setEmpresa(empresa);
        template.setNome(nome);
        template.setObrigatorio(true);
        templateDocumentoRepository.save(template);
    }
}
