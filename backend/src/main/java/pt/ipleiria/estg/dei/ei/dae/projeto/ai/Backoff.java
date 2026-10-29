package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import java.util.concurrent.ThreadLocalRandom;

/** Pausas entre tentativas: backoff exponencial com teto e jitter (evita que vários clientes tentem ao mesmo tempo). */
public final class Backoff {

    public static final long TETO_MS = 60_000L;

    private Backoff() {
    }

    /** base, 2x base, 4x base... até ao teto de 60 segundos. {@code tentativa} começa em 1. */
    public static long pausa(long baseMs, int tentativa) {
        long fator = 1L << Math.min(Math.max(tentativa - 1, 0), 20);
        return Math.min(baseMs * fator, TETO_MS);
    }

    /** A pausa acrescida de até 20% aleatórios. */
    public static long pausaComJitter(long baseMs, int tentativa) {
        long p = pausa(baseMs, tentativa);
        return p + ThreadLocalRandom.current().nextLong(p / 5 + 1);
    }
}
