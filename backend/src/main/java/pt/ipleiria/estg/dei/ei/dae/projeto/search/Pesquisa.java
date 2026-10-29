package pt.ipleiria.estg.dei.ei.dae.projeto.search;

import java.util.Locale;
import java.util.Map;

/**
 * Peças puras (sem JPA) da pesquisa: escape de LIKE e whitelist de ordenação.
 * Separadas para poderem ser testadas sem base de dados.
 */
public final class Pesquisa {

    public static final char ESCAPE = '!';

    private static final String SUB_COMENTARIOS =
            "(SELECT COUNT(c) FROM Comentario c WHERE c.publicacao = p AND c.visivel = true)";
    private static final String SUB_NRATINGS =
            "(SELECT COUNT(r) FROM Rating r WHERE r.publicacao = p)";
    private static final String SUB_MEDIA =
            "(SELECT AVG(r2.valor) FROM Rating r2 WHERE r2.publicacao = p)";

    /** Só estas chaves são aceites: o valor nunca é concatenado diretamente na query. */
    private static final Map<String, String> ORDENACOES = Map.of(
            "data", "p.dataSubmissao",
            "titulo", "LOWER(p.titulo)",
            "ano", "p.ano",
            "comentarios", SUB_COMENTARIOS,
            "ratings", SUB_NRATINGS,
            "rating", "COALESCE(" + SUB_MEDIA + ", 0.0)"
    );

    private Pesquisa() {
    }

    public static String expressaoSelectComentarios() { return SUB_COMENTARIOS; }
    public static String expressaoSelectRatings() { return SUB_NRATINGS; }
    public static String expressaoSelectMedia() { return SUB_MEDIA; }

    /** "%texto%" em minúsculas, com %, _ e ! escapados. */
    public static String padraoLike(String texto) {
        String t = texto.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder("%");
        for (char c : t.toCharArray()) {
            if (c == '%' || c == '_' || c == ESCAPE) {
                sb.append(ESCAPE);
            }
            sb.append(c);
        }
        return sb.append('%').toString();
    }

    /** @return "ORDER BY ..." seguro. Chaves desconhecidas caem na ordenação por data. */
    public static String orderBy(String sort, String dir) {
        String chave = sort == null ? "data" : sort.toLowerCase(Locale.ROOT);
        String expressao = ORDENACOES.getOrDefault(chave, ORDENACOES.get("data"));
        String direcao = "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";
        String nulos = expressao.equals("p.dataSubmissao") ? " NULLS LAST" : "";
        return " ORDER BY " + expressao + " " + direcao + nulos + ", p.id " + direcao;
    }

    public static int limitarTamanho(int size, int max) {
        return Math.min(Math.max(size, 1), max);
    }

    public static int paginaValida(int page) {
        return Math.max(page, 0);
    }
}
