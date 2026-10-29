package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import java.util.function.LongSupplier;

/**
 * Circuit breaker simples. Depois de {@code falhasParaAbrir} falhas seguidas abre e os pedidos falham logo,
 * sem tentar o serviço, durante {@code aberturaMs}. Depois deixa passar UM pedido de teste (meio aberto):
 * se correr bem fecha, se falhar volta a abrir.
 */
public class CircuitBreaker {

    public enum Estado { FECHADO, ABERTO, MEIO_ABERTO }

    private final int falhasParaAbrir;
    private final long aberturaMs;
    private final LongSupplier relogio;

    private Estado estado = Estado.FECHADO;
    private int falhasSeguidas;
    private long abertoAte;
    private boolean testeEmCurso;

    public CircuitBreaker(int falhasParaAbrir, long aberturaMs) {
        this(falhasParaAbrir, aberturaMs, System::currentTimeMillis);
    }

    public CircuitBreaker(int falhasParaAbrir, long aberturaMs, LongSupplier relogioMs) {
        this.falhasParaAbrir = Math.max(1, falhasParaAbrir);
        this.aberturaMs = aberturaMs;
        this.relogio = relogioMs;
    }

    /** @return true se o pedido pode seguir; o chamador TEM de chamar sucesso() ou falha() a seguir. */
    public synchronized boolean permitir() {
        switch (estado) {
            case FECHADO:
                return true;
            case ABERTO:
                if (relogio.getAsLong() < abertoAte) {
                    return false;
                }
                estado = Estado.MEIO_ABERTO;
                testeEmCurso = true;
                return true;
            default: // MEIO_ABERTO: só um teste de cada vez
                if (testeEmCurso) {
                    return false;
                }
                testeEmCurso = true;
                return true;
        }
    }

    public synchronized void sucesso() {
        estado = Estado.FECHADO;
        falhasSeguidas = 0;
        testeEmCurso = false;
    }

    public synchronized void falha() {
        if (estado == Estado.MEIO_ABERTO || ++falhasSeguidas >= falhasParaAbrir) {
            abrir();
        }
    }

    private void abrir() {
        estado = Estado.ABERTO;
        abertoAte = relogio.getAsLong() + aberturaMs;
        falhasSeguidas = 0;
        testeEmCurso = false;
    }

    public synchronized Estado estado() {
        return estado;
    }
}
