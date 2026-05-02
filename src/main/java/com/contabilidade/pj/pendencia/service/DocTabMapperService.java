package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.pendencia.config.DocTabsProperties;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DocTabMapperService {
    private static final String ABA_FALLBACK = "OUTROS";
    private final DocTabsProperties properties;

    public DocTabMapperService(DocTabsProperties properties) {
        this.properties = properties;
    }

    public List<String> ordemAbas() {
        List<String> ordem = properties.getOrdem();
        if (ordem == null || ordem.isEmpty()) {
            return List.of(ABA_FALLBACK);
        }
        Set<String> unicos = new LinkedHashSet<>();
        for (String id : ordem) {
            String normalizado = upperTrim(id);
            if (!normalizado.isBlank()) {
                unicos.add(normalizado);
            }
        }
        unicos.add(ABA_FALLBACK);
        return new ArrayList<>(unicos);
    }

    public String tituloAba(String idAba) {
        Map<String, String> titulos = properties.getTitulos();
        if (titulos == null || titulos.isEmpty()) {
            return "Outros";
        }
        return titulos.getOrDefault(upperTrim(idAba), "Outros");
    }

    public String idAbaParaTipoDetectado(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return ABA_FALLBACK;
        }
        Map<String, String> mapa = properties.getTipoDetectado();
        if (mapa == null || mapa.isEmpty()) {
            return ABA_FALLBACK;
        }
        String id = mapa.get(upperTrim(tipoDocumento));
        return id == null || id.isBlank() ? ABA_FALLBACK : upperTrim(id);
    }

    public String idAbaParaTemplateNome(String templateNome) {
        if (templateNome == null || templateNome.isBlank()) {
            return ABA_FALLBACK;
        }
        String base = normalize(templateNome);
        List<DocTabsProperties.TemplateRule> rules = properties.getTemplateRules();
        if (rules == null || rules.isEmpty()) {
            return ABA_FALLBACK;
        }
        for (DocTabsProperties.TemplateRule rule : rules) {
            String aba = upperTrim(rule.getAba());
            if (aba.isBlank() || rule.getKeywords() == null) {
                continue;
            }
            for (String keyword : rule.getKeywords()) {
                String key = normalize(keyword);
                if (!key.isBlank() && keywordMatchesTemplate(base, key)) {
                    return aba;
                }
            }
        }
        return ABA_FALLBACK;
    }

    private static String upperTrim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    /**
     * Correspondência por palavra inteira para termos curtos (ex.: {@code cide} não casa em {@code incidencia}).
     * Frases com espaço continuam com {@link String#contains}.
     */
    private static boolean keywordMatchesTemplate(String baseNormalized, String keyNormalized) {
        if (keyNormalized.contains(" ")) {
            return baseNormalized.contains(keyNormalized);
        }
        return Pattern.compile("\\b" + Pattern.quote(keyNormalized) + "\\b").matcher(baseNormalized).find();
    }
}
