package com.contabilidade.pj.api;

import com.contabilidade.pj.config.Contab360FeaturesProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FeaturesController {

    private final Contab360FeaturesProperties featuresProperties;

    public FeaturesController(Contab360FeaturesProperties featuresProperties) {
        this.featuresProperties = featuresProperties;
    }

    /**
     * Público: usado pelo front para exibir/ocultar módulos sem autenticação.
     */
    @GetMapping("/features")
    public Map<String, Boolean> features() {
        return Map.of("certificadoDigital", featuresProperties.isCertificadoDigital());
    }
}
