package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void abreDepoisDeFalhasSeguidasEFalhaLogo() {
        AtomicLong agora = new AtomicLong(0);
        CircuitBreaker cb = new CircuitBreaker(3, 60_000, agora::get);
        for (int i = 0; i < 3; i++) {
            assertTrue(cb.permitir());
            cb.falha();
        }
        assertEquals(CircuitBreaker.Estado.ABERTO, cb.estado());
        assertFalse(cb.permitir(), "aberto: nem tenta o serviço");
    }

    @Test
    void umSucessoZeraAContagemDeFalhas() {
        CircuitBreaker cb = new CircuitBreaker(3, 60_000, () -> 0L);
        cb.permitir(); cb.falha();
        cb.permitir(); cb.falha();
        cb.permitir(); cb.sucesso();
        cb.permitir(); cb.falha();
        cb.permitir(); cb.falha();
        assertEquals(CircuitBreaker.Estado.FECHADO, cb.estado(), "só 2 falhas seguidas desde o sucesso");
    }

    @Test
    void depoisDoTempoDeixaPassarUmTesteQueFechaOuReabre() {
        AtomicLong agora = new AtomicLong(0);
        CircuitBreaker cb = new CircuitBreaker(1, 60_000, agora::get);
        cb.permitir(); cb.falha();
        assertFalse(cb.permitir());

        agora.set(60_000);
        assertTrue(cb.permitir(), "meio aberto: deixa passar um teste");
        assertFalse(cb.permitir(), "mas só um de cada vez");
        cb.falha(); // o teste falhou: volta a abrir
        assertEquals(CircuitBreaker.Estado.ABERTO, cb.estado());

        agora.set(120_000);
        assertTrue(cb.permitir());
        cb.sucesso(); // o teste correu bem: fecha
        assertEquals(CircuitBreaker.Estado.FECHADO, cb.estado());
        assertTrue(cb.permitir());
    }
}
