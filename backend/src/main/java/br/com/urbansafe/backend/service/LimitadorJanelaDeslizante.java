package br.com.urbansafe.backend.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import br.com.urbansafe.backend.exception.ApiException;

/**
 * Limite de requisições por chave (normalmente o IP) em janela deslizante, em memória. Usado nas rotas públicas
 * que não exigem login (ex.: cálculo de rotas) para impedir abuso. Com várias instâncias da API o limite é por
 * instância. Atrás de proxy reverso configure {@code server.forward-headers-strategy} para enxergar o IP real.
 */
public class LimitadorJanelaDeslizante {

    private static final int TAMANHO_MAXIMO_MAPA = 10_000;

    private final int maximo;
    private final long janelaNanos;
    private final String mensagem;
    private final LongSupplier relogioNanos;
    private final Map<String, Deque<Long>> tentativas = new ConcurrentHashMap<>();

    public LimitadorJanelaDeslizante(int maximo, Duration janela, String mensagem) {
        this(maximo, janela, mensagem, System::nanoTime);
    }

    LimitadorJanelaDeslizante(int maximo, Duration janela, String mensagem, LongSupplier relogioNanos) {
        if (maximo < 1 || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("Limite e janela devem ser positivos.");
        }
        this.maximo = maximo;
        this.janelaNanos = janela.toNanos();
        this.mensagem = mensagem;
        this.relogioNanos = relogioNanos;
    }

    /** Registra uma requisição; lança 429 se a chave já atingiu o máximo dentro da janela. */
    public void registrar(String chave) {
        long agora = relogioNanos.getAsLong();

        if (tentativas.size() > TAMANHO_MAXIMO_MAPA) {
            tentativas.values().removeIf(fila -> vazia(fila, agora));
        }

        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());

        synchronized (fila) {
            descartarExpiradas(fila, agora);
            if (fila.size() >= maximo) {
                throw ApiException.tooManyRequests(mensagem);
            }
            fila.addLast(agora);
        }
    }

    private boolean vazia(Deque<Long> fila, long agora) {
        synchronized (fila) {
            descartarExpiradas(fila, agora);
            return fila.isEmpty();
        }
    }

    private void descartarExpiradas(Deque<Long> fila, long agora) {
        while (!fila.isEmpty() && agora - fila.peekFirst() >= janelaNanos) {
            fila.pollFirst();
        }
    }
}
