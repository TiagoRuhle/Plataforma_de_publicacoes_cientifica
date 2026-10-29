package pt.ipleiria.estg.dei.ei.dae.projeto.search;

import java.time.LocalDate;

/** Critérios de pesquisa/ordenação/paginação de publicações. Campos nulos = sem filtro. */
public class PublicacaoFiltro {
    public String q;          // texto livre: título, autor, área ou tipo
    public String titulo;
    public String autor;
    public String area;
    public String tipo;
    public String tag;
    public Integer anoDe;
    public Integer anoAte;
    public LocalDate desde;   // data de submissão
    public LocalDate ate;

    public boolean somenteVisiveis;
    public boolean somenteOcultas;
    public boolean minhas;

    public String sort = "data";   // data | titulo | ano | comentarios | rating | ratings
    public String dir = "desc";    // asc | desc
    public int page = 0;
    public int size = 20;
}
