package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PendenciaDocumentoSchemaConfig {

    private static final Logger log = LoggerFactory.getLogger(PendenciaDocumentoSchemaConfig.class);

    @Bean
    CommandLineRunner ajustarSchemaPendenciasDocumentos(JdbcTemplate jdbcTemplate) {
        return args -> {
            tornarEmpresaNullable(jdbcTemplate);
            removerIndiceUnicoLegadoEmpresaTemplateCompetencia(jdbcTemplate);
        };
    }

    private void tornarEmpresaNullable(JdbcTemplate jdbcTemplate) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT IS_NULLABLE, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'pendencias_documentos' " +
                            "AND COLUMN_NAME = 'empresa_id'"
            );
            if (rows.isEmpty()) {
                return;
            }
            String nullable = String.valueOf(rows.get(0).get("IS_NULLABLE"));
            String columnType = String.valueOf(rows.get(0).get("COLUMN_TYPE"));
            if ("YES".equalsIgnoreCase(nullable)) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE pendencias_documentos MODIFY COLUMN empresa_id " + columnType + " NULL");
            log.warn("Schema ajustado: pendencias_documentos.empresa_id alterada para NULL.");
        } catch (Exception ex) {
            log.debug("Nao foi possivel ajustar nulidade de pendencias_documentos.empresa_id: {}", ex.getMessage());
        }
    }

    private void removerIndiceUnicoLegadoEmpresaTemplateCompetencia(JdbcTemplate jdbcTemplate) {
        try {
            List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                    "SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE, SEQ_IN_INDEX " +
                            "FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pendencias_documentos' " +
                            "ORDER BY INDEX_NAME, SEQ_IN_INDEX"
            );
            Map<String, List<Map<String, Object>>> porIndice = stats.stream()
                    .collect(java.util.stream.Collectors.groupingBy(r -> String.valueOf(r.get("INDEX_NAME"))));
            for (Map.Entry<String, List<Map<String, Object>>> entry : porIndice.entrySet()) {
                String idx = entry.getKey();
                if ("PRIMARY".equalsIgnoreCase(idx)) {
                    continue;
                }
                List<Map<String, Object>> cols = entry.getValue();
                if (cols.isEmpty()) {
                    continue;
                }
                boolean unique = String.valueOf(cols.get(0).get("NON_UNIQUE")).equals("0");
                if (!unique) {
                    continue;
                }
                boolean temEmpresa = cols.stream().anyMatch(c -> "empresa_id".equalsIgnoreCase(String.valueOf(c.get("COLUMN_NAME"))));
                boolean temTemplate = cols.stream().anyMatch(c -> "template_documento_id".equalsIgnoreCase(String.valueOf(c.get("COLUMN_NAME"))));
                boolean temCompetencia = cols.stream().anyMatch(c -> "competencia_id".equalsIgnoreCase(String.valueOf(c.get("COLUMN_NAME"))));
                boolean temTomador = cols.stream().anyMatch(c -> "tomador_uid".equalsIgnoreCase(String.valueOf(c.get("COLUMN_NAME"))));
                if (temEmpresa && temTemplate && temCompetencia && !temTomador) {
                    // Garante índice simples para FK de empresa antes de remover o índice único legado.
                    if (!porIndice.containsKey("idx_pendencias_empresa_id")) {
                        jdbcTemplate.execute("CREATE INDEX idx_pendencias_empresa_id ON pendencias_documentos (empresa_id)");
                    }
                    jdbcTemplate.execute("ALTER TABLE pendencias_documentos DROP INDEX `" + idx + "`");
                    log.warn("Schema ajustado: indice unico legado removido em pendencias_documentos: {}", idx);
                }
            }
        } catch (Exception ex) {
            log.debug("Nao foi possivel remover indice unico legado de pendencias_documentos: {}", ex.getMessage());
        }
    }
}
