package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Estratégia de resumo para textos longos (map-reduce):
 *  - texto curto  -> 1 chamada ao LLM
 *  - texto longo  -> resume cada bloco e depois resume os resumos parciais
 * Não depende do cliente HTTP: recebe um {@link Gerador}, o que permite testar sem LLM.
 */
public class ResumoPipeline {

    @FunctionalInterface
    public interface Gerador {
        String gerar(String prompt) throws IOException;
    }

    /** Limite do resumo guardado (a coluna na BD tem 20000). */
    public static final int MAX_RESUMO_CHARS = 19000;

    private final Gerador gerador;
    private final int chunkChars;
    private final int maxChunks;

    public ResumoPipeline(Gerador gerador, int chunkChars, int maxChunks) {
        this.gerador = gerador;
        this.chunkChars = chunkChars;
        this.maxChunks = Math.max(1, maxChunks);
    }

    /** Usa outro gerador só nesta chamada (ex.: o cliente do Ollama embrulhado por um circuit breaker). */
    public String resumir(String texto, Gerador outroGerador) throws IOException {
        return new ResumoPipeline(outroGerador, chunkChars, maxChunks).resumir(texto);
    }

    public String resumir(String texto) throws IOException {
        if (texto == null || texto.isBlank()) {
            throw new IOException("O documento não contém texto extraível (pode ser um PDF digitalizado).");
        }
        List<String> blocos = amostrar(TextChunker.dividir(texto, chunkChars), maxChunks);
        String resultado;
        if (blocos.size() == 1) {
            resultado = gerador.gerar(promptFinal(blocos.get(0)));
        } else {
            List<String> parciais = new ArrayList<>();
            for (int i = 0; i < blocos.size(); i++) {
                parciais.add(gerador.gerar(promptParcial(blocos.get(i), i + 1, blocos.size())));
            }
            resultado = gerador.gerar(promptSintese(String.join("\n\n", parciais)));
        }
        return limitar(resultado);
    }

    /**
     * Se houver mais blocos do que o máximo, escolhe blocos espaçados de forma uniforme
     * (sempre incluindo o primeiro e o último), para cobrir início, meio e fim do documento.
     */
    static List<String> amostrar(List<String> todos, int max) {
        if (todos.size() <= max) {
            return todos;
        }
        if (max == 1) {
            return List.of(todos.get(0));
        }
        List<String> escolhidos = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            int indice = (int) Math.round(i * (todos.size() - 1) / (double) (max - 1));
            escolhidos.add(todos.get(indice));
        }
        return escolhidos;
    }

    static String limitar(String s) {
        String r = s == null ? "" : s.trim();
        if (r.isEmpty()) {
            throw new IllegalStateException("O modelo devolveu um resumo vazio.");
        }
        return r.length() > MAX_RESUMO_CHARS ? r.substring(0, MAX_RESUMO_CHARS) : r;
    }

    static String promptFinal(String texto) {
        return "Responde sempre em português de Portugal.\n"
                + "Escreve um resumo claro e profissional da publicação científica abaixo, "
                + "com as secções: Problema (que questão ou limitação aborda), Solução proposta (método ou abordagem), "
                + "Resultados e Conclusões. "
                + "Usa no máximo 300 palavras e não inventes informação que não esteja no texto.\n\n"
                + "TEXTO:\n" + texto;
    }

    static String promptParcial(String texto, int n, int total) {
        return "Responde sempre em português de Portugal.\n"
                + "Este é o bloco " + n + " de " + total + " de uma publicação científica. "
                + "Resume-o em tópicos curtos, mantendo o problema, a solução, os métodos e os resultados. "
                + "Não inventes informação.\n\nTEXTO:\n" + texto;
    }

    static String promptSintese(String parciais) {
        return "Responde sempre em português de Portugal.\n"
                + "Abaixo estão resumos parciais de uma publicação científica. "
                + "Junta-os num único resumo coerente e profissional, com as secções: "
                + "Problema, Solução proposta, Resultados e Conclusões, em no máximo 300 palavras. "
                + "Não inventes informação.\n\nRESUMOS PARCIAIS:\n" + parciais;
    }
}
