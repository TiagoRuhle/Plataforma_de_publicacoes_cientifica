package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

class TagsESubscricoesIT extends ApiTestBase {

    private static String novaTag() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void responsavelCriaTagEDuplicadaDa409() {
        String resp = tokenResponsavel();
        String nome = novaTag();
        como(resp).contentType(ContentType.JSON).body(Map.of("tag", nome, "visivel", true))
                .post("/tags").then().statusCode(201).body("tag", equalTo(nome));
        como(resp).contentType(ContentType.JSON).body(Map.of("tag", nome, "visivel", true))
                .post("/tags").then().statusCode(409);
        como(resp).delete("/tags/" + nome).then().statusCode(204);
    }

    @Test
    void colaboradorNaoCriaTags() {
        como(tokenColaborador()).contentType(ContentType.JSON).body(Map.of("tag", novaTag(), "visivel", true))
                .post("/tags").then().statusCode(403);
    }

    @Test
    void tagInvalidaDa400() {
        como(tokenResponsavel()).contentType(ContentType.JSON).body(Map.of("tag", "a/b", "visivel", true))
                .post("/tags").then().statusCode(400);
    }

    @Test
    void apagarTagEmUsoLimpaSubscricoesEAssociacoes() {
        String resp = tokenResponsavel();
        String colab = tokenColaborador();
        String nome = novaTag();
        long id = idPorTitulo(colab, "Deep Learning");

        como(resp).contentType(ContentType.JSON).body(Map.of("tag", nome, "visivel", true)).post("/tags").then().statusCode(201);
        como(colab).contentType(ContentType.JSON).body(nome).post("/publicacoes/" + id + "/tags").then().statusCode(201);
        como(colab).contentType(ContentType.JSON).body(Map.of("nome", nome)).post("/subscricao").then().statusCode(201);

        como(resp).delete("/tags/" + nome).then().statusCode(204);

        como(colab).get("/publicacoes/" + id).then().statusCode(200).body("tags.tag", not(hasItem(nome)));
        como(colab).get("/users/view/subscricoes").then().statusCode(200).body("nome", not(hasItem(nome)));
    }

    @Test
    void associarMesmaTagDuasVezesDa409() {
        String resp = tokenResponsavel();
        String colab = tokenColaborador();
        String nome = novaTag();
        long id = idPorTitulo(colab, "Deep Learning");
        como(resp).contentType(ContentType.JSON).body(Map.of("tag", nome, "visivel", true)).post("/tags").then().statusCode(201);
        como(colab).contentType(ContentType.JSON).body(nome).post("/publicacoes/" + id + "/tags").then().statusCode(201);
        como(colab).contentType(ContentType.JSON).body(nome).post("/publicacoes/" + id + "/tags").then().statusCode(409);
        como(resp).contentType(ContentType.JSON).body(nome).delete("/publicacoes/" + id + "/tags").then().statusCode(202);
        como(resp).delete("/tags/" + nome).then().statusCode(204);
    }

    @Test
    void colaboradorNaoUsaTagOculta() {
        String colab = tokenColaborador();
        long id = idPorTitulo(colab, "Deep Learning");
        como(colab).contentType(ContentType.JSON).body("tag-interna").post("/publicacoes/" + id + "/tags").then().statusCode(404);
        como(colab).contentType(ContentType.JSON).body(Map.of("nome", "tag-interna")).post("/subscricao").then().statusCode(404);
    }

    @Test
    void fluxoDeSubscricao() {
        String resp = tokenResponsavel();
        String colab = tokenColaborador();
        String nome = novaTag();
        como(resp).contentType(ContentType.JSON).body(Map.of("tag", nome, "visivel", true)).post("/tags").then().statusCode(201);

        como(colab).contentType(ContentType.JSON).body(Map.of("nome", nome)).post("/subscricao").then().statusCode(201);
        como(colab).contentType(ContentType.JSON).body(Map.of("nome", nome)).post("/subscricao").then().statusCode(409);
        como(colab).get("/users/view/subscricoes").then().statusCode(200).body("nome", hasItem(nome));
        como(colab).contentType(ContentType.JSON).body(Map.of("nome", nome)).delete("/subscricao").then().statusCode(204);
        como(colab).contentType(ContentType.JSON).body(Map.of("nome", nome)).delete("/subscricao").then().statusCode(404);

        como(resp).delete("/tags/" + nome).then().statusCode(204);
    }

    @Test
    void subscreverTagInexistenteDa404() {
        como(tokenColaborador()).contentType(ContentType.JSON).body(Map.of("nome", "nao-existe-" + UUID.randomUUID()))
                .post("/subscricao").then().statusCode(404);
    }
}
