package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HasherTest {
    @Test
    void verificaPasswordCorreta() {
        String hash = Hasher.hash("segredo123");
        assertTrue(Hasher.verify("segredo123", hash));
        assertFalse(Hasher.verify("outra", hash));
    }

    @Test
    void hashesDaMesmaPasswordSaoDiferentesPorCausaDoSalt() {
        assertNotEquals(Hasher.hash("igual"), Hasher.hash("igual"));
    }

    @Test
    void naoVerificaUmHashContraOutroHash() {
        // regressão: o updatePassword antigo comparava hash com hash e nunca falhava
        String hash = Hasher.hash("segredo123");
        assertFalse(Hasher.verify(Hasher.hash("segredo123"), hash));
    }
}
