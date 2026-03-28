package com.contabilidade.pj.ia;

import com.contabilidade.pj.auth.PerfilUsuario;
import com.contabilidade.pj.auth.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IaObservadoraService {

    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public IaObservadoraService(AuditoriaEventoRepository auditoriaEventoRepository) {
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    @Transactional(readOnly = true)
    public IaObservadoraResponse analisar(Usuario usuarioAtual) {
        if (usuarioAtual.getPerfil() != PerfilUsuario.CONTADOR) {
            throw new IllegalArgumentException("Apenas contador acessa a IA observadora.");
        }
        Instant desde = Instant.now().minus(7, ChronoUnit.DAYS);
        List<AuditoriaEvento> eventos = auditoriaEventoRepository.findTop200ByCriadoEmAfterOrderByCriadoEmDesc(desde);
        long total = auditoriaEventoRepository.countByCriadoEmAfter(desde);

        Map<String, Long> porPerfil = eventos.stream()
                .filter(e -> e.getPerfil() != null)
                .collect(Collectors.groupingBy(AuditoriaEvento::getPerfil, Collectors.counting()));

        Map<String, Long> porCategoria = eventos.stream()
                .filter(e -> e.getCategoria() != null)
                .collect(Collectors.groupingBy(AuditoriaEvento::getCategoria, Collectors.counting()));

        Map<String, Long> contagensPath = new HashMap<>();
        for (AuditoriaEvento e : eventos) {
            String chave = e.getMetodoHttp() + " " + simplificarPath(e.getPath());
            contagensPath.merge(chave, 1L, Long::sum);
        }
        List<Map.Entry<String, Long>> topPaths = contagensPath.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toList());

        List<String> sugestoes = gerarSugestoesAutonomia(eventos, porPerfil, porCategoria, total);

        List<EventoResumo> ultimos = eventos.stream()
                .limit(40)
                .map(e -> new EventoResumo(
                        e.getCriadoEm().toString(),
                        e.getUsuarioEmail(),
                        e.getPerfil(),
                        e.getMetodoHttp(),
                        e.getPath(),
                        e.getCategoria(),
                        e.getStatusHttp()
                ))
                .collect(Collectors.toList());

        return new IaObservadoraResponse(
                total,
                porPerfil,
                porCategoria,
                topPaths.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)),
                sugestoes,
                ultimos
        );
    }

    private static String simplificarPath(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("/\\d+", "/{id}");
    }

    private static List<String> gerarSugestoesAutonomia(
            List<AuditoriaEvento> eventos,
            Map<String, Long> porPerfil,
            Map<String, Long> porCategoria,
            long total
    ) {
        List<String> s = new ArrayList<>();
        if (total == 0) {
            s.add("Ainda nao ha dados suficientes. Use o sistema normalmente: a IA observadora aprende com cada acao.");
            return s;
        }
        s.add(String.format(Locale.ROOT,
                "Volume observado (7 dias): %d eventos registrados. Isso alimenta o modelo de padroes de uso.", total));

        long cli = porPerfil.getOrDefault("CLIENTE", 0L);
        long cont = porPerfil.getOrDefault("CONTADOR", 0L);
        if (cli > cont * 2 && cli > 5) {
            s.add("Padrao: o portal do cliente e muito utilizado. Automatizar lembretes de pendencias pode reduzir carga manual.");
        }
        if (cont > cli * 2 && cont > 5) {
            s.add("Padrao: o contador concentra a maior parte das acoes. Fluxos em lote (gerar pendencias, revisao IA) sao candidatos a automacao.");
        }
        long fiscal = porCategoria.getOrDefault("FISCAL", 0L);
        if (fiscal > 10) {
            s.add("Modulo fiscal e recorrente. Em uma evolucao futura, a IA podera sugerir emissoes e cobrancas com base nesse historico.");
        }
        long pend = porCategoria.getOrDefault("PENDENCIAS", 0L);
        if (pend > 15) {
            s.add("Fluxo de pendencias intenso. Padroes de upload por competencia podem ser aprendidos para prever atrasos.");
        }

        List<String> sequencias = detectarSequenciasFrequentes(eventos);
        s.addAll(sequencias);

        s.add("Proximo passo tecnico: conectar um modelo de linguagem (API) para sugestoes em linguagem natural e automacoes com confirmacao.");
        return s;
    }

    /**
     * Detecta pares metodo+path A -> B frequentes no mesmo usuario em janela curta (simula "aprendizado" de rotina).
     */
    private static List<String> detectarSequenciasFrequentes(List<AuditoriaEvento> eventos) {
        List<AuditoriaEvento> ordenados = new ArrayList<>(eventos);
        ordenados.sort(Comparator.comparing(AuditoriaEvento::getCriadoEm));
        Map<String, Integer> pares = new HashMap<>();
        Map<String, String> ultimoPorUsuario = new HashMap<>();
        for (AuditoriaEvento e : ordenados) {
            String email = e.getUsuarioEmail();
            if (email == null) {
                continue;
            }
            String atual = e.getMetodoHttp() + " " + simplificarPath(e.getPath());
            String anterior = ultimoPorUsuario.get(email);
            if (anterior != null) {
                String par = anterior + " => " + atual;
                pares.merge(par, 1, Integer::sum);
            }
            ultimoPorUsuario.put(email, atual);
        }
        return pares.entrySet().stream()
                .filter(en -> en.getValue() >= 3)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(en -> "Sequencia repetida (" + en.getValue() + "x): " + en.getKey())
                .collect(Collectors.toList());
    }

    public record IaObservadoraResponse(
            long totalEventosPeriodo,
            Map<String, Long> acoesPorPerfil,
            Map<String, Long> acoesPorCategoria,
            Map<String, Long> topCaminhos,
            List<String> sugestoesAutonomia,
            List<EventoResumo> ultimosEventos
    ) {
    }

    public record EventoResumo(
            String criadoEm,
            String usuarioEmail,
            String perfil,
            String metodoHttp,
            String path,
            String categoria,
            Integer statusHttp
    ) {
    }
}
