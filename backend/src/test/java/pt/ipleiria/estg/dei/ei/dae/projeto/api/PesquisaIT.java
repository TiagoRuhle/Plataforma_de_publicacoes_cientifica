package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PesquisaIT extends ApiTestBase {

    @Test
    void pesquisaPorTextoLivreEncontraPorTituloAutorEArea() {
        String colab = tokenColaborador();
        como(colab).queryParam("q", "convolucionais").get("/publicacoes/search").then().statusCode(200)
                .body("total", greaterThanOrEqualTo(1)).body("items.titulo", hasItem(containsString("Convolucionais")));
        como(colab).queryParam("autor", "angelo").get("/publicacoes/search").then().statusCode(200)
                .body("items.autor", everyItem(containsStringIgnoringCase("angelo")));
        como(colab).queryParam("area", "machine").get("/publicacoes/search").then().statusCode(200)
                .body("total", greaterThanOrEqualTo(1));
    }

    @Test
    void pesquisaNaoTrataPercentagemComoCuringa() {
        como(tokenColaborador()).queryParam("q", "%").get("/publicacoes/search").then().statusCode(200)
                .body("total", equalTo(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtraPorTag() {
        List<Map<String, Object>> items = como(tokenColaborador()).queryParam("tag", "Projeto X")
                .get("/publicacoes/search").then().statusCode(200).body("total", greaterThanOrEqualTo(2))
                .extract().jsonPath().getList("items");
        for (Map<String, Object> item : items) {
            List<Map<String, String>> tags = (List<Map<String, String>>) item.get("tags");
            assertTrue(tags.stream().anyMatch(t -> "Projeto X".equals(t.get("tag"))), "todas têm a tag Projeto X");
        }
    }

    @Test
    void colaboradorNaoPodeFiltrarPorTagOculta() {
        como(tokenColaborador()).queryParam("tag", "tag-interna").get("/publicacoes/search").then().statusCode(200)
                .body("total", equalTo(0));
        como(tokenResponsavel()).queryParam("tag", "tag-interna").get("/publicacoes/search").then().statusCode(200)
                .body("total", greaterThanOrEqualTo(1));
    }

    @Test
    void filtraPorAno() {
        como(tokenColaborador()).queryParam("anoDe", 2025).queryParam("anoAte", 2025)
                .get("/publicacoes/search").then().statusCode(200)
                .body("items.ano", everyItem(equalTo(2025))).body("total", greaterThanOrEqualTo(1));
    }

    @Test
    void ordenaPorRatingMedioDescendente() {
        List<Number> medias = como(tokenColaborador()).queryParam("sort", "rating").queryParam("dir", "desc")
                .get("/publicacoes/search").then().statusCode(200).extract().jsonPath().getList("items.ratingMedio");
        assertTrue(medias.size() >= 3);
        for (int i = 1; i < medias.size(); i++) {
            assertTrue(medias.get(i - 1).doubleValue() >= medias.get(i).doubleValue(), "ordem decrescente de rating: " + medias);
        }
    }

    @Test
    void ordenaPorNumeroDeComentarios() {
        List<Number> totais = como(tokenColaborador()).queryParam("sort", "comentarios").queryParam("dir", "desc")
                .get("/publicacoes/search").then().statusCode(200).extract().jsonPath().getList("items.totalComentarios");
        for (int i = 1; i < totais.size(); i++) {
            assertTrue(totais.get(i - 1).doubleValue() >= totais.get(i).doubleValue(), "ordem decrescente de comentários: " + totais);
        }
    }

    @Test
    void ordenaPorNumeroDeRatings() {
        List<Number> totais = como(tokenColaborador()).queryParam("sort", "ratings").queryParam("dir", "asc")
                .get("/publicacoes/search").then().statusCode(200).extract().jsonPath().getList("items.totalRatings");
        for (int i = 1; i < totais.size(); i++) {
            assertTrue(totais.get(i - 1).doubleValue() <= totais.get(i).doubleValue(), "ordem crescente de ratings: " + totais);
        }
    }

    @Test
    void paginaOsResultados() {
        como(tokenResponsavel()).queryParam("size", 2).queryParam("page", 0).queryParam("sort", "titulo").queryParam("dir", "asc")
                .get("/publicacoes/search").then().statusCode(200)
                .body("items.size()", equalTo(2)).body("size", equalTo(2)).body("page", equalTo(0))
                .body("total", greaterThanOrEqualTo(5)).body("totalPages", greaterThanOrEqualTo(3));
    }

    @Test
    void tamanhoDaPaginaEstaLimitado() {
        como(tokenColaborador()).queryParam("size", 100000).get("/publicacoes/search").then().statusCode(200)
                .body("size", equalTo(100));
    }

    @Test
    void parametrosInvalidosDao400OuSaoIgnorados() {
        como(tokenColaborador()).queryParam("desde", "ontem").get("/publicacoes/search").then().statusCode(400);
        como(tokenColaborador()).queryParam("sort", "p.id; DROP TABLE users").get("/publicacoes/search")
                .then().statusCode(200);
    }

    @Test
    void asMinhasPublicacoesIncluemAsOcultas() {
        como(tokenColaborador2()).get("/publicacoes/view/mine").then().statusCode(200)
                .body("titulo", hasItem("Relatório Técnico Confidencial"));
    }
}
