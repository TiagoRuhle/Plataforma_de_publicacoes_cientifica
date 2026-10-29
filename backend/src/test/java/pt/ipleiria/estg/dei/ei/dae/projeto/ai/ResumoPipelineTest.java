package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoPipelineTest {

    private static final String LONGO = ("Frase de teste numero um. ".repeat(40) + "\n\n").repeat(30);

    @Test
    void textoCurtoFazUmaSoChamada() throws IOException {
        List<String> prompts = new ArrayList<>();
        ResumoPipeline p = new ResumoPipeline(prompt -> { prompts.add(prompt); return "RESUMO"; }, 5000, 8);
        assertEquals("RESUMO", p.resumir("texto pequeno"));
        assertEquals(1, prompts.size());
    }

    @Test
    void textoLongoFazMapReduce() throws IOException {
        List<String> prompts = new ArrayList<>();
        ResumoPipeline p = new ResumoPipeline(prompt -> { prompts.add(prompt); return "parcial"; }, 1000, 4);
        p.resumir(LONGO);
        assertEquals(5, prompts.size(), "4 resumos parciais + 1 síntese");
        assertTrue(prompts.get(4).contains("RESUMOS PARCIAIS"));
    }

    @Test
    void textoSemConteudoLancaIOException() {
        ResumoPipeline p = new ResumoPipeline(prompt -> "x", 5000, 8);
        assertThrows(IOException.class, () -> p.resumir("   "));
    }

    @Test
    void resumoVazioDoModeloELancaErro() {
        ResumoPipeline p = new ResumoPipeline(prompt -> "   ", 5000, 8);
        assertThrows(IllegalStateException.class, () -> p.resumir("texto"));
    }

    @Test
    void resumoEnormeELimitadoAoTamanhoDaColuna() throws IOException {
        ResumoPipeline p = new ResumoPipeline(prompt -> "x".repeat(50_000), 5000, 8);
        assertEquals(ResumoPipeline.MAX_RESUMO_CHARS, p.resumir("texto").length());
    }

    @Test
    void amostragemIncluiPrimeiroEUltimoBloco() {
        List<String> todos = new ArrayList<>();
        for (int i = 0; i < 20; i++) todos.add("b" + i);
        List<String> escolhidos = ResumoPipeline.amostrar(todos, 4);
        assertEquals(4, escolhidos.size());
        assertEquals("b0", escolhidos.get(0));
        assertEquals("b19", escolhidos.get(3));
    }
}
