package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptTrackerTest {

    @Test
    void bloqueiaAposDemasiadasFalhasEDesbloqueiaComOTempo() {
        AtomicLong agora = new AtomicLong(1_000_000);
        LoginAttemptTracker t = new LoginAttemptTracker(3, Duration.ofMinutes(5), agora::get);

        t.registarFalha("Ana");
        t.registarFalha("ana");
        assertFalse(t.bloqueado("ana"));
        t.registarFalha("ANA");
        assertTrue(t.bloqueado("ana"));
        assertEquals(300, t.segundosRestantes("ana"));
        assertFalse(t.bloqueado("outro"));

        agora.addAndGet(301_000);
        assertFalse(t.bloqueado("ana"));
    }

    @Test
    void loginComSucessoLimpaOContador() {
        LoginAttemptTracker t = new LoginAttemptTracker(3, Duration.ofMinutes(5));
        t.registarFalha("bia");
        t.registarFalha("bia");
        t.limpar("bia");
        t.registarFalha("bia");
        assertFalse(t.bloqueado("bia"));
    }
}
