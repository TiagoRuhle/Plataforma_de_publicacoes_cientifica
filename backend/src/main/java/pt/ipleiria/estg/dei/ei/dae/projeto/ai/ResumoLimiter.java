package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita quantos resumos correm ao mesmo tempo (o Ollama em CPU trata um pedido de cada vez; sem limite,
 * vários uploads seguidos ficam todos em espera e esgotam o timeout) e quantos podem esperar na fila.
 */
public class ResumoLimiter {

    public static class FilaCheiaException extends Exception {
        public FilaCheiaException() {
            super("A fila de resumos está cheia. Tente novamente dentro de alguns minutos.");
        }
    }

    public static class EsperaExcedidaException extends Exception {
        public EsperaExcedidaException() {
            super("O resumo esteve demasiado tempo na fila. Tente novamente.");
        }
    }

    private final Semaphore permissoes;
    private final int maxFila;
    private final AtomicInteger aEsperar = new AtomicInteger();

    public ResumoLimiter(int maxConcorrentes, int maxFila) {
        this.permissoes = new Semaphore(Math.max(1, maxConcorrentes), true);
        this.maxFila = Math.max(0, maxFila);
    }

    /**
     * Espera pela sua vez e executa a tarefa.
     *
     * @throws FilaCheiaException        se já houver {@code maxFila} pedidos à espera
     * @throws EsperaExcedidaException   se a vez não chegar dentro de {@code esperaMaxMs}
     */
    public <T> T executar(Callable<T> tarefa, long esperaMaxMs) throws Exception {
        if (aEsperar.incrementAndGet() > maxFila + permissoes.availablePermits()) {
            aEsperar.decrementAndGet();
            throw new FilaCheiaException();
        }
        boolean obtida;
        try {
            obtida = permissoes.tryAcquire(esperaMaxMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            aEsperar.decrementAndGet();
        }
        if (!obtida) {
            throw new EsperaExcedidaException();
        }
        try {
            return tarefa.call();
        } finally {
            permissoes.release();
        }
    }

    public int aEsperar() {
        return aEsperar.get();
    }

    public int livres() {
        return permissoes.availablePermits();
    }
}
