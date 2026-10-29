package pt.ipleiria.estg.dei.ei.dae.projeto.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {

    @TempDir
    Path pasta;

    private static final PdfTextExtractor.Limites LIMITES =
            new PdfTextExtractor.Limites(150, 400_000, Duration.ofSeconds(30), 8L * 1024 * 1024);

    /** PDF com {@code paginas} páginas; cada uma tem "PAGINA n" e {@code linhasPorPagina} linhas de texto. */
    private Path criarPdf(int paginas, int linhasPorPagina) throws IOException {
        Path ficheiro = pasta.resolve("t" + System.nanoTime() + ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int p = 1; p <= paginas; p++) {
                PDPage pagina = new PDPage();
                doc.addPage(pagina);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(40, 780);
                    cs.showText("PAGINA " + p);
                    for (int l = 0; l < linhasPorPagina; l++) {
                        cs.newLineAtOffset(0, -12);
                        cs.showText("linha de texto numero " + l + " para encher a pagina");
                    }
                    cs.endText();
                }
            }
            doc.save(ficheiro.toFile());
        }
        return ficheiro;
    }

    @Test
    void extraiTextoNormal() throws IOException {
        String texto = PdfTextExtractor.extrairTexto(criarPdf(2, 5), LIMITES);
        assertTrue(texto.contains("PAGINA 1"));
        assertTrue(texto.contains("PAGINA 2"));
    }

    @Test
    void soLeAsPrimeirasPaginas() throws IOException {
        var limites = new PdfTextExtractor.Limites(3, 400_000, Duration.ofSeconds(30), 8L * 1024 * 1024);
        String texto = PdfTextExtractor.extrairTexto(criarPdf(10, 3), limites);
        assertTrue(texto.contains("PAGINA 3"));
        assertFalse(texto.contains("PAGINA 4"), "as páginas acima do limite nem são processadas");
    }

    @Test
    void cortaOTextoNoLimiteDeCaracteresSemFalhar() throws IOException {
        var limites = new PdfTextExtractor.Limites(150, 500, Duration.ofSeconds(30), 8L * 1024 * 1024);
        String texto = PdfTextExtractor.extrairTexto(criarPdf(3, 40), limites);
        assertEquals(500, texto.length());
        assertTrue(texto.startsWith("PAGINA 1"));
    }

    @Test
    void respeitaOTempoMaximo() throws IOException {
        var limites = new PdfTextExtractor.Limites(150, 400_000, Duration.ofMillis(-100), 8L * 1024 * 1024);
        IOException e = assertThrows(IOException.class, () -> PdfTextExtractor.extrairTexto(criarPdf(1, 5), limites));
        assertTrue(e.getMessage().contains("limites"));
    }

    @Test
    void paginaSemTextoDevolveVazioEFicheiroInvalidoFalha() throws IOException {
        Path vazio = pasta.resolve("vazio.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(vazio.toFile());
        }
        assertTrue(PdfTextExtractor.extrairTexto(vazio, LIMITES).isBlank());

        Path lixo = pasta.resolve("lixo.pdf");
        Files.writeString(lixo, "isto nao e um pdf");
        assertThrows(IOException.class, () -> PdfTextExtractor.extrairTexto(lixo, LIMITES));
    }
}
