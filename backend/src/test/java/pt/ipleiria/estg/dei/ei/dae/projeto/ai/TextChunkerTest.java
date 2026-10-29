package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    @Test
    void textoCurtoFicaNumSoBloco() {
        assertEquals(List.of("curto"), TextChunker.dividir("curto", 500));
    }

    @Test
    void textoVazioNaoGeraBlocos() {
        assertTrue(TextChunker.dividir("   ", 500).isEmpty());
        assertTrue(TextChunker.dividir(null, 500).isEmpty());
    }

    @Test
    void respeitaOLimiteDeCadaBloco() {
        String longo = ("Frase de teste numero um. ".repeat(40) + "\n\n").repeat(30);
        List<String> blocos = TextChunker.dividir(longo, 1000);
        assertTrue(blocos.size() > 1);
        assertTrue(blocos.stream().allMatch(b -> b.length() <= 1000));
    }

    @Test
    void rejeitaLimiteDemasiadoPequeno() {
        assertThrows(IllegalArgumentException.class, () -> TextChunker.dividir("x", 10));
    }
}
