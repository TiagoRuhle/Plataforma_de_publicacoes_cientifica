package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackoffTest {
    @Test
    void duplicaACadaTentativaAteAoTeto() {
        assertEquals(5_000, Backoff.pausa(5_000, 1));
        assertEquals(10_000, Backoff.pausa(5_000, 2));
        assertEquals(20_000, Backoff.pausa(5_000, 3));
        assertEquals(40_000, Backoff.pausa(5_000, 4));
        assertEquals(Backoff.TETO_MS, Backoff.pausa(5_000, 5));
        assertEquals(Backoff.TETO_MS, Backoff.pausa(5_000, 500)); // sem overflow
    }

    @Test
    void jitterNuncaPassaDe20PorCento() {
        for (int i = 0; i < 200; i++) {
            long p = Backoff.pausaComJitter(1_000, 3);
            assertTrue(p >= 4_000 && p <= 4_800, "pausa fora do intervalo: " + p);
        }
    }
}
