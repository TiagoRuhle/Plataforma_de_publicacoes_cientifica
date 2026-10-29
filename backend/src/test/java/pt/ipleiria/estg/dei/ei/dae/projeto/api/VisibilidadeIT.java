package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** Conteúdo oculto (publicações, comentários, tags) só é visível a quem de direito. */
class VisibilidadeIT extends ApiTestBase {

    private static final String TITULO_OCULTA = "Relatório Técnico Confidencial";

    @Test
    void publicacaoOcultaSoApareceAoDonoEAGestores() {
        assertEquals(-1, idPorTitulo(tokenColaborador(), TITULO_OCULTA), "outro colaborador não a deve ver");
        assertTrue(idPorTitulo(tokenColaborador2(), TITULO_OCULTA) > 0, "o dono vê-a");
        assertTrue(idPorTitulo(tokenResponsavel(), TITULO_OCULTA) > 0, "o responsável vê-a");
        assertTrue(idPorTitulo(tokenAdmin(), TITULO_OCULTA) > 0, "o admin vê-a");
    }

    @Test
    void acessoDiretoAPublicacaoOcultaDa404ParaOutros() {
        long id = idPorTitulo(tokenResponsavel(), TITULO_OCULTA);
        como(tokenColaborador()).get("/publicacoes/" + id).then().statusCode(404);
        como(tokenColaborador()).get("/publicacoes/" + id + "/download").then().statusCode(404);
        como(tokenColaborador()).get("/historico/publicacao/" + id).then().statusCode(404);
        como(tokenColaborador()).get("/historico/publicacao/" + id + "/estatisticas").then().statusCode(404);
        como(tokenColaborador2()).get("/publicacoes/" + id).then().statusCode(200);
    }

    @Test
    void naoSePodeComentarNemAvaliarPublicacaoOculta() {
        long id = idPorTitulo(tokenResponsavel(), TITULO_OCULTA);
        como(tokenColaborador()).contentType("application/json")
                .body(Map.of("publicacaoId", id, "texto", "olá")).post("/comentarios").then().statusCode(404);
        como(tokenColaborador()).contentType("application/json")
                .body(Map.of("rating", 5)).post("/publicacoes/" + id + "/rating").then().statusCode(404);
    }

    @Test
    void listagensGeraisNaoIncluemPublicacoesOcultasDeOutros() {
        como(tokenColaborador()).get("/publicacoes/").then().statusCode(200)
                .body("titulo", not(hasItem(TITULO_OCULTA)));
        como(tokenResponsavel()).get("/publicacoes/view/invisivel").then().statusCode(200)
                .body("titulo", hasItem(TITULO_OCULTA));
        como(tokenColaborador()).get("/publicacoes/view/invisivel").then().statusCode(403);
    }

    @Test
    void comentarioOcultoSoSaiDoServidorParaGestores() {
        long id = idPorTitulo(tokenColaborador(), "Deep Learning");
        como(tokenColaborador()).get("/publicacoes/" + id).then().statusCode(200)
                .body("comentarios.texto", not(hasItem(containsString("impróprio"))))
                .body("totalComentarios", greaterThanOrEqualTo(3));
        como(tokenResponsavel()).get("/publicacoes/" + id).then().statusCode(200)
                .body("comentarios.texto", hasItem(containsString("impróprio")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void tagOcultaSoApareceAGestores() {
        como(tokenColaborador()).get("/tags").then().statusCode(200).body("tag", not(hasItem("tag-interna")));
        como(tokenResponsavel()).get("/tags/view").then().statusCode(200).body("tag", hasItem("tag-interna"));

        long id = idPorTitulo(tokenColaborador(), "Algoritmos de Machine Learning");
        List<Map<String, String>> tagsColab = como(tokenColaborador()).get("/publicacoes/" + id)
                .then().statusCode(200).extract().jsonPath().getList("tags");
        assertTrue(tagsColab.stream().noneMatch(t -> "tag-interna".equals(t.get("tag"))));

        List<Map<String, String>> tagsResp = como(tokenResponsavel()).get("/publicacoes/" + id)
                .then().statusCode(200).extract().jsonPath().getList("tags");
        assertTrue(tagsResp.stream().anyMatch(t -> "tag-interna".equals(t.get("tag"))));
    }

    @Test
    void historicoDaPublicacaoSoParaDonoEGestores() {
        long id = idPorTitulo(tokenColaborador(), "Deep Learning"); // submetida pelo admin
        como(tokenColaborador()).get("/historico/publicacao/" + id).then().statusCode(403);
        como(tokenAdmin()).get("/historico/publicacao/" + id).then().statusCode(200).body("size()", greaterThan(0));
    }
}
