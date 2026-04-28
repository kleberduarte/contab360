package com.contabilidade.pj.auth.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bloqueia IPs com excesso de tentativas de login falhas (brute-force).
 * Janela deslizante: máx. 5 falhas em 15 minutos por IP.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FALHAS = 5;
    private static final long JANELA_SEGUNDOS = 15 * 60;

    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    public boolean isoBloqueado(String ip) {
        Deque<Instant> janela = tentativas.get(ip);
        if (janela == null) return false;
        synchronized (janela) {
            limpar(janela);
            return janela.size() >= MAX_FALHAS;
        }
    }

    public void registrarFalha(String ip) {
        Deque<Instant> janela = tentativas.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (janela) {
            janela.addLast(Instant.now());
        }
    }

    public void registrarSucesso(String ip) {
        tentativas.remove(ip);
    }

    private void limpar(Deque<Instant> janela) {
        Instant limite = Instant.now().minusSeconds(JANELA_SEGUNDOS);
        while (!janela.isEmpty() && janela.peekFirst().isBefore(limite)) {
            janela.pollFirst();
        }
    }
}
