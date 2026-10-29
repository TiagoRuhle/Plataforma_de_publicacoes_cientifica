package pt.ipleiria.estg.dei.ei.dae.projeto.utils;

import java.util.Locale;

/**
 * Valida uploads: só PDF ou ZIP, confirmando a extensão E os "magic bytes".
 * Classe sem dependências de Jakarta, para ser facilmente testável.
 */
public final class FicheiroValidator {

    public enum Tipo {
        PDF("application/pdf"), ZIP("application/zip");

        private final String mime;

        Tipo(String mime) {
            this.mime = mime;
        }

        public String mime() {
            return mime;
        }
    }

    private FicheiroValidator() {
    }

    /** @throws IllegalArgumentException com mensagem pronta a mostrar ao utilizador */
    public static Tipo validar(String nomeFicheiro, byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("O ficheiro está vazio.");
        }
        String nome = sanitizarNome(nomeFicheiro).toLowerCase(Locale.ROOT);
        boolean pareceZip = nome.endsWith(".zip");
        boolean parecePdf = nome.endsWith(".pdf");
        if (!pareceZip && !parecePdf) {
            throw new IllegalArgumentException("Só são aceites ficheiros PDF ou ZIP.");
        }
        if (parecePdf && !comecaPorPdf(conteudo)) {
            throw new IllegalArgumentException("O ficheiro não é um PDF válido.");
        }
        if (pareceZip && !comecaPorZip(conteudo)) {
            throw new IllegalArgumentException("O ficheiro não é um ZIP válido.");
        }
        return parecePdf ? Tipo.PDF : Tipo.ZIP;
    }

    /** Remove caminhos e caracteres de controlo do nome enviado pelo cliente. */
    public static String sanitizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "ficheiro";
        }
        String limpo = nome.replace('\\', '/');
        limpo = limpo.substring(limpo.lastIndexOf('/') + 1);
        limpo = limpo.replaceAll("[\\p{Cntrl}\"]", "_").trim();
        if (limpo.length() > 200) {
            limpo = limpo.substring(limpo.length() - 200);
        }
        return limpo.isEmpty() ? "ficheiro" : limpo;
    }

    private static boolean comecaPorPdf(byte[] b) {
        // o cabeçalho "%PDF-" pode ter até 1024 bytes de lixo antes (especificação)
        int limite = Math.min(b.length - 5, 1024);
        for (int i = 0; i <= limite; i++) {
            if (b[i] == '%' && b[i + 1] == 'P' && b[i + 2] == 'D' && b[i + 3] == 'F' && b[i + 4] == '-') {
                return true;
            }
        }
        return false;
    }

    private static boolean comecaPorZip(byte[] b) {
        return b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
    }
}
