package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.PublicacaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.search.PaginaDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.search.Pesquisa;
import pt.ipleiria.estg.dei.ei.dae.projeto.search.PublicacaoFiltro;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pesquisa paginada de publicações. Em 3 queries (total, ids+agregados, entidades com tags)
 * em vez de carregar todas as coleções de todas as publicações (N+1).
 * Os totais de comentários/ratings e a média são calculados pela BD, que também ordena.
 */
@Stateless
public class PublicacaoSearchBean {

    @PersistenceContext
    private EntityManager entityManager;

    public PaginaDTO<PublicacaoDTO> pesquisar(PublicacaoFiltro f, String me, boolean gestor, int tamanhoMaximo) {
        int size = Pesquisa.limitarTamanho(f.size, tamanhoMaximo);
        int page = Pesquisa.paginaValida(f.page);

        Map<String, Object> params = new LinkedHashMap<>();
        String where = construirWhere(f, me, gestor, params);

        TypedQuery<Long> contagem = entityManager.createQuery(
                "SELECT COUNT(p) FROM Publicacao p" + where, Long.class);
        params.forEach(contagem::setParameter);
        long total = contagem.getSingleResult();

        if (total == 0) {
            return new PaginaDTO<>(List.of(), 0, page, size);
        }

        String hql = "SELECT p.id, " + Pesquisa.expressaoSelectComentarios() + ", "
                + Pesquisa.expressaoSelectRatings() + ", " + Pesquisa.expressaoSelectMedia()
                + " FROM Publicacao p" + where + Pesquisa.orderBy(f.sort, f.dir);
        TypedQuery<Object[]> linhasQuery = entityManager.createQuery(hql, Object[].class);
        params.forEach(linhasQuery::setParameter);
        linhasQuery.setFirstResult(page * size);
        linhasQuery.setMaxResults(size);
        List<Object[]> linhas = linhasQuery.getResultList();

        List<Long> ids = new ArrayList<>();
        for (Object[] l : linhas) {
            ids.add(((Number) l[0]).longValue());
        }
        if (ids.isEmpty()) {
            return new PaginaDTO<>(List.of(), total, page, size);
        }

        Map<Long, Publicacao> porId = new HashMap<>();
        for (Publicacao p : entityManager.createQuery(
                        "SELECT DISTINCT p FROM Publicacao p LEFT JOIN FETCH p.tags "
                                + "LEFT JOIN FETCH p.submetidoPor WHERE p.id IN :ids", Publicacao.class)
                .setParameter("ids", ids).getResultList()) {
            porId.put(p.getId(), p);
        }

        List<PublicacaoDTO> items = new ArrayList<>();
        for (Object[] l : linhas) {
            Publicacao p = porId.get(((Number) l[0]).longValue());
            if (p == null) {
                continue;
            }
            double media = l[3] == null ? 0.0 : ((Number) l[3]).doubleValue();
            items.add(PublicacaoDTO.ofRow(p, ((Number) l[1]).longValue(), ((Number) l[2]).longValue(),
                    media, gestor));
        }
        return new PaginaDTO<>(items, total, page, size);
    }

    private String construirWhere(PublicacaoFiltro f, String me, boolean gestor, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");

        // visibilidade: quem não é gestor só vê as visíveis e as suas próprias
        if (!gestor) {
            w.append(" AND (p.visivel = true OR p.submetidoPor.username = :me)");
            params.put("me", me);
        }
        if (f.somenteVisiveis) {
            w.append(" AND p.visivel = true");
        }
        if (f.somenteOcultas) {
            w.append(" AND p.visivel = false");
        }
        if (f.minhas) {
            w.append(" AND p.submetidoPor.username = :me");
            params.put("me", me);
        }

        if (preenchido(f.q)) {
            w.append(" AND (LOWER(p.titulo) LIKE :q ESCAPE '!' OR LOWER(p.autor) LIKE :q ESCAPE '!'")
                    .append(" OR LOWER(p.areaCientifica) LIKE :q ESCAPE '!' OR LOWER(p.tipo) LIKE :q ESCAPE '!')");
            params.put("q", Pesquisa.padraoLike(f.q));
        }
        adicionarLike(w, params, "p.titulo", "titulo", f.titulo);
        adicionarLike(w, params, "p.autor", "autor", f.autor);
        adicionarLike(w, params, "p.areaCientifica", "area", f.area);
        adicionarLike(w, params, "p.tipo", "tipo", f.tipo);

        if (preenchido(f.tag)) {
            // tags ocultas só podem ser usadas como filtro por gestores
            w.append(" AND p.id IN (SELECT p2.id FROM Publicacao p2 JOIN p2.tags t2 WHERE t2.nome = :tag")
                    .append(gestor ? "" : " AND t2.visivel = true").append(")");
            params.put("tag", f.tag.trim());
        }
        if (f.anoDe != null) {
            w.append(" AND p.ano >= :anoDe");
            params.put("anoDe", f.anoDe);
        }
        if (f.anoAte != null) {
            w.append(" AND p.ano <= :anoAte");
            params.put("anoAte", f.anoAte);
        }
        if (f.desde != null) {
            w.append(" AND p.dataSubmissao >= :desde");
            params.put("desde", f.desde.atStartOfDay());
        }
        if (f.ate != null) {
            w.append(" AND p.dataSubmissao < :ate");
            params.put("ate", f.ate.plusDays(1).atTime(LocalTime.MIN));
        }
        return w.toString();
    }

    private void adicionarLike(StringBuilder w, Map<String, Object> params, String campo, String nome, String valor) {
        if (preenchido(valor)) {
            w.append(" AND LOWER(").append(campo).append(") LIKE :").append(nome).append(" ESCAPE '!'");
            params.put(nome, Pesquisa.padraoLike(valor));
        }
    }

    private boolean preenchido(String s) {
        return s != null && !s.isBlank();
    }
}
