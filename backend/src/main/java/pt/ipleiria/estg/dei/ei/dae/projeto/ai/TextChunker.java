package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import java.util.ArrayList;
import java.util.List;

/** Divide texto longo em blocos, preferindo cortar em fins de parágrafo/frase/palavra. */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> dividir(String texto, int maxChars) {
        if (maxChars < 200) {
            throw new IllegalArgumentException("maxChars demasiado pequeno");
        }
        List<String> blocos = new ArrayList<>();
        if (texto == null) {
            return blocos;
        }
        String t = texto.replace("\r\n", "\n").replaceAll("[ \\t]+", " ").trim();
        int inicio = 0;
        while (inicio < t.length()) {
            int fimMax = Math.min(inicio + maxChars, t.length());
            if (fimMax == t.length()) {
                blocos.add(t.substring(inicio).trim());
                break;
            }
            int corte = melhorCorte(t, inicio, fimMax);
            blocos.add(t.substring(inicio, corte).trim());
            inicio = corte;
        }
        blocos.removeIf(String::isBlank);
        return blocos;
    }

    private static int melhorCorte(String t, int inicio, int fimMax) {
        int minimo = inicio + (fimMax - inicio) / 2; // não cortar demasiado cedo
        int p = t.lastIndexOf("\n\n", fimMax);
        if (p >= minimo) {
            return p + 2;
        }
        p = t.lastIndexOf(". ", fimMax);
        if (p >= minimo) {
            return p + 2;
        }
        p = t.lastIndexOf(' ', fimMax);
        if (p >= minimo) {
            return p + 1;
        }
        return fimMax;
    }
}
