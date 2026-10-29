package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicacaoServiceUtilTest {

    @Test
    void limparTagAceitaComEOuSemAspas() {
        assertEquals("Projeto X", PublicacaoService.limparTag("\"Projeto X\""));
        assertEquals("Projeto X", PublicacaoService.limparTag("  Projeto X "));
        assertEquals("", PublicacaoService.limparTag(null));
    }

    @Test
    void contentDispositionNaoPermiteInjecaoDeCabecalhos() {
        String h = PublicacaoService.contentDisposition("relatório\"\r\nX-Evil: 1.pdf");
        assertFalse(h.contains("\r") || h.contains("\n"));
        assertTrue(h.startsWith("attachment; filename=\""));
        assertTrue(h.contains("filename*=UTF-8''"));
    }
}
