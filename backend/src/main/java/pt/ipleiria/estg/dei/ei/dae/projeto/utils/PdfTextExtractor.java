package pt.ipleiria.estg.dei.ei.dae.projeto.utils;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Extrai texto de um PDF com limites estritos, porque o ficheiro vem de utilizadores e pode ser
 * malicioso (PDF "bomb": streams comprimidos gigantes, milhares de páginas, estruturas profundas):
 * <ul>
 *   <li>memória: o PDFBox passa para ficheiros temporários acima de {@code memoriaMaxBytes};</li>
 *   <li>páginas: só se lêem as primeiras {@code maxPaginas};</li>
 *   <li>texto: o texto é escrito para um buffer limitado, por isso nunca existe "tudo" em memória;</li>
 *   <li>tempo: a extração é interrompida passado {@code tempoMaximo}.</li>
 * </ul>
 * Se o limite for atingido depois de já haver texto, usa-se o que foi extraído (para um resumo chega).
 */
public final class PdfTextExtractor {

    public record Limites(int maxPaginas, int maxChars, Duration tempoMaximo, long memoriaMaxBytes) {
        public static Limites daConfiguracao() {
            return new Limites(
                    AppConfig.pdfMaxPages(),
                    AppConfig.pdfMaxChars(),
                    Duration.ofSeconds(AppConfig.pdfTimeoutSeconds()),
                    AppConfig.pdfMaxMemoryMb() * 1024L * 1024L);
        }
    }

    private PdfTextExtractor() {
    }

    public static String extrairTexto(Path pdf) throws IOException {
        return extrairTexto(pdf, Limites.daConfiguracao());
    }

    public static String extrairTexto(Path pdf, Limites limites) throws IOException {
        long fim = System.nanoTime() + limites.tempoMaximo().toNanos();
        MemoryUsageSetting memoria = MemoryUsageSetting.setupMixed(limites.memoriaMaxBytes());

        try (PDDocument documento = PDDocument.load(pdf.toFile(), memoria)) {
            if (!documento.getCurrentAccessPermission().canExtractContent()) {
                throw new IOException("O PDF não permite a extração de texto.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.max(1, Math.min(limites.maxPaginas(), documento.getNumberOfPages())));

            LimitedWriter escritor = new LimitedWriter(limites.maxChars(), fim);
            try {
                stripper.writeText(documento, escritor);
            } catch (LimiteAtingido e) {
                if (escritor.length() == 0) {
                    throw new IOException("Não foi possível extrair texto do PDF dentro dos limites ("
                            + e.getMessage() + ").", e);
                }
                // já há texto suficiente: continua com o que foi extraído
            }
            return escritor.toString();
        }
    }

    /** Sinal interno: um dos limites foi atingido. */
    static final class LimiteAtingido extends IOException {
        LimiteAtingido(String motivo) {
            super(motivo);
        }
    }

    /** Writer que guarda no máximo {@code maxChars} caracteres e respeita um prazo. */
    static final class LimitedWriter extends Writer {
        private final StringBuilder texto = new StringBuilder();
        private final int maxChars;
        private final long fimNanos;

        LimitedWriter(int maxChars, long fimNanos) {
            this.maxChars = maxChars;
            this.fimNanos = fimNanos;
        }

        @Override
        public void write(char[] buffer, int inicio, int tamanho) throws IOException {
            if (System.nanoTime() - fimNanos > 0) {
                throw new LimiteAtingido("tempo máximo excedido");
            }
            int cabem = maxChars - texto.length();
            if (tamanho > cabem) {
                texto.append(buffer, inicio, Math.max(0, cabem));
                throw new LimiteAtingido("texto demasiado longo");
            }
            texto.append(buffer, inicio, tamanho);
        }

        int length() {
            return texto.length();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return texto.toString();
        }
    }
}
