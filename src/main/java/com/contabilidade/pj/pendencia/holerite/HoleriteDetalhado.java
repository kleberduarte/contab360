package com.contabilidade.pj.pendencia.holerite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estrutura alinhada ao que escritórios costumam usar no holerite/folha: identificação, período,
 * rubricas, totais e bases — base para o diferencial de captura completa.
 */
public record HoleriteDetalhado(
        HoleriteEmpresa empresa,
        HoleriteFuncionario funcionario,
        HoleritePeriodo periodo,
        List<HoleriteRubrica> proventos,
        List<HoleriteRubrica> descontos,
        HoleriteTotais totais,
        HoleriteBases bases,
        Map<String, String> camposAdicionais
) {
    public boolean preenchidoMinimo() {
        if (totais != null && totais.valorLiquidoBr() != null && !totais.valorLiquidoBr().isBlank()) {
            return true;
        }
        return (empresa != null && empresa.cnpjDigitos() != null && !empresa.cnpjDigitos().isBlank())
                || (funcionario != null && funcionario.nome() != null && !funcionario.nome().isBlank())
                || (proventos != null && !proventos.isEmpty());
    }

    /**
     * Campos "planos" para tabelas na UI e persistência linha a rubrica.
     */
    public Map<String, String> toCamposPlanos() {
        Map<String, String> m = new LinkedHashMap<>();
        if (empresa != null) {
            put(m, "holerite.empresa.razaoSocial", empresa.razaoSocial());
            put(m, "holerite.empresa.cnpjFormatado", empresa.cnpjFormatado());
            put(m, "holerite.empresa.cnpjDigitos", empresa.cnpjDigitos());
            put(m, "holerite.empresa.endereco", empresa.endereco());
        }
        if (funcionario != null) {
            put(m, "holerite.funcionario.nome", funcionario.nome());
            put(m, "holerite.funcionario.cpf", funcionario.cpf());
            put(m, "holerite.funcionario.pisPasep", funcionario.pisPasep());
            put(m, "holerite.funcionario.cargo", funcionario.cargo());
            put(m, "holerite.funcionario.departamento", funcionario.departamento());
            put(m, "holerite.funcionario.dataAdmissao", funcionario.dataAdmissao());
        }
        if (periodo != null) {
            put(m, "holerite.periodo.competencia", periodo.competencia()); // mm/aaaa ou aaaa-mm
            put(m, "holerite.periodo.tipoFolha", periodo.tipoFolha());
            put(m, "holerite.periodo.diasTrabalhados", periodo.diasTrabalhados());
        }
        if (proventos != null) {
            for (int i = 0; i < proventos.size(); i++) {
                HoleriteRubrica r = proventos.get(i);
                String p = "holerite.proventos[" + i + "]";
                put(m, p + ".descricao", r.descricao());
                put(m, p + ".referencia", r.referencia());
                put(m, p + ".valorOriginalBr", r.valorOriginalBr());
                put(m, p + ".valorNumerico", r.valorNumerico());
            }
        }
        if (descontos != null) {
            for (int i = 0; i < descontos.size(); i++) {
                HoleriteRubrica r = descontos.get(i);
                String p = "holerite.descontos[" + i + "]";
                put(m, p + ".descricao", r.descricao());
                put(m, p + ".referencia", r.referencia());
                put(m, p + ".valorOriginalBr", r.valorOriginalBr());
                put(m, p + ".valorNumerico", r.valorNumerico());
            }
        }
        if (totais != null) {
            put(m, "holerite.totais.totalProventosBr", totais.totalProventosBr());
            put(m, "holerite.totais.totalProventosNumerico", totais.totalProventosNumerico());
            put(m, "holerite.totais.totalDescontosBr", totais.totalDescontosBr());
            put(m, "holerite.totais.totalDescontosNumerico", totais.totalDescontosNumerico());
            put(m, "holerite.totais.valorLiquidoBr", totais.valorLiquidoBr());
            put(m, "holerite.totais.valorLiquidoNumerico", totais.valorLiquidoNumerico());
        }
        if (bases != null) {
            put(m, "holerite.bases.salarioBaseESocialBr", bases.salarioBaseESocialBr());
            put(m, "holerite.bases.salarioBaseESocialNumerico", bases.salarioBaseESocialNumerico());
            put(m, "holerite.bases.baseCalculoFgtsBr", bases.baseCalculoFgtsBr());
            put(m, "holerite.bases.baseCalculoFgtsNumerico", bases.baseCalculoFgtsNumerico());
            put(m, "holerite.bases.fgtsMesBr", bases.fgtsMesBr());
            put(m, "holerite.bases.fgtsMesNumerico", bases.fgtsMesNumerico());
            put(m, "holerite.bases.baseCalculoInssBr", bases.baseCalculoInssBr());
            put(m, "holerite.bases.baseCalculoInssNumerico", bases.baseCalculoInssNumerico());
        }
        if (camposAdicionais != null) {
            camposAdicionais.forEach((k, v) -> put(m, "holerite.extra." + k, v));
        }
        return m;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v);
        }
    }

    public record HoleriteEmpresa(
            String razaoSocial,
            String cnpjFormatado,
            String cnpjDigitos,
            String endereco
    ) {
    }

    public record HoleriteFuncionario(
            String nome,
            String cpf,
            String pisPasep,
            String cargo,
            String departamento,
            String dataAdmissao
    ) {
    }

    public record HoleritePeriodo(String competencia, String tipoFolha, String diasTrabalhados) {
    }

    public record HoleriteRubrica(String descricao, String referencia, String valorOriginalBr, String valorNumerico) {
    }

    public record HoleriteTotais(
            String totalProventosBr,
            String totalProventosNumerico,
            String totalDescontosBr,
            String totalDescontosNumerico,
            String valorLiquidoBr,
            String valorLiquidoNumerico
    ) {
    }

    public record HoleriteBases(
            String salarioBaseESocialBr,
            String salarioBaseESocialNumerico,
            String baseCalculoFgtsBr,
            String baseCalculoFgtsNumerico,
            String fgtsMesBr,
            String fgtsMesNumerico,
            String baseCalculoInssBr,
            String baseCalculoInssNumerico
    ) {
    }
}
