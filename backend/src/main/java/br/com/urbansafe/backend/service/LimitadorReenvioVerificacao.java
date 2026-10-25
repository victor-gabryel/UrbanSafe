package br.com.urbansafe.backend.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.urbansafe.backend.exception.ApiException;

/**
 * Limite de pedidos de reenvio por chave (IP) em janela deslizante, em memória.
 * É aplicado antes de qualquer consulta ao banco e igual para e-mails existentes ou não,
 * então a resposta 429 não revela nada sobre contas. Com várias instâncias da API o limite é
 * por instância; o intervalo mínimo por conta (banco) continua valendo para todas.
 */
@Component
public class LimitadorReenvioVerificacao {

    static final String MSG_LIMITE = "Muitas solicitações. Aguarde alguns minutos e tente novamente.";

    private final int maximo;
    private final long janelaNanos;
    private final LongSupplier relogioNanos;
    private final Map<String, Deque<Long>> tentativas = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorReenvioVerificacao(
            @Value("${app.email-verification.resend-max-per-window:5}") int maximo,
            @Value("${app.email-verification.resend-window-minutes:15}") long janelaMinutos) {
        this(maximo, Duration.ofMinutes(janelaMinutos), System::nanoTime);
    }

    LimitadorReenvioVerificacao(int maximo, Duration janela, LongSupplier relogioNanos) {
        this.maximo = maximo;
        this.janelaNanos = janela.toNanos();
        this.relogioNanos = relogioNanos;
    }

    /** Registra uma tentativa; lança 429 se a chave já atingiu o máximo dentro da janela. */
    public void registrar(String chave) {
        long agora = relogioNanos.getAsLong();

        if (tentativas.size() > 10_000) {
            tentativas.values().removeIf(d -> synchronizedVazia(d, agora));
        }

        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());

        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() >= janelaNanos) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                throw ApiException.tooManyRequests(MSG_LIMITE);
            }
            fila.addLast(agora);
        }
    }

    private boolean synchronizedVazia(Deque<Long> fila, long agora) {
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() >= janelaNanos) {
                fila.pollFirst();
            }
            return fila.isEmpty();
        }
    }
}
