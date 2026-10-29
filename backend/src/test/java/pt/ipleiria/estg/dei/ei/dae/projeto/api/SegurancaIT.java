package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/** A matriz de permissões do enunciado (Colaborador < Responsável < Administrador). */
class SegurancaIT extends ApiTestBase {

    @Test
    void colaboradorNaoAlteraEmailDeOutroUtilizador() {
        como(tokenColaborador()).contentType(ContentType.JSON).body(Map.of("email", "hacker@mal.com"))
                .patch("/users/administrador/updateEmail").then().statusCode(403);
    }

    @Test
    void colaboradorNaoAlteraPasswordDeOutroUtilizador() {
        como(tokenColaborador()).contentType(ContentType.JSON)
                .body(Map.of("oldPassword", "administrador", "newPassword", "hackeado123"))
                .patch("/users/administrador/updatePassword").then().statusCode(403);
    }

    @Test
    void passwordAntigaErradaDa400() {
        como(tokenColaborador2()).contentType(ContentType.JSON)
                .body(Map.of("oldPassword", "errada", "newPassword", "novapassword1"))
                .patch("/users/colaborador2/updatePassword").then().statusCode(400);
    }

    @Test
    void passwordNovaCurtaDa400() {
        como(tokenColaborador2()).contentType(ContentType.JSON)
                .body(Map.of("oldPassword", "colaborador2", "newPassword", "curta"))
                .patch("/users/colaborador2/updatePassword").then().statusCode(400);
    }

    @Test
    void apenasAdminListaUtilizadores() {
        como(tokenColaborador()).get("/users").then().statusCode(403);
        como(tokenResponsavel()).get("/users").then().statusCode(403);
        como(tokenAdmin()).get("/users").then().statusCode(200).body("size()", greaterThanOrEqualTo(5));
    }

    @Test
    void apenasAdminVeHistoricoDeOutrosUtilizadores() {
        como(tokenResponsavel()).get("/historico/user/colaborador").then().statusCode(403);
        como(tokenAdmin()).get("/historico/user/colaborador").then().statusCode(200);
    }

    @Test
    void adminNaoSuspendeNemApagaASiProprio() {
        String admin = tokenAdmin();
        como(admin).contentType(ContentType.JSON).body(Map.of("active", false))
                .patch("/users/administrador/updateStatus").then().statusCode(403);
        como(admin).delete("/users/administrador").then().statusCode(403);
    }

    @Test
    void roleInvalidoDa400() {
        como(tokenAdmin()).contentType(ContentType.JSON).body(Map.of("role", "xyz"))
                .patch("/users/colaborador2/updateRole").then().statusCode(400);
    }

    @Test
    void criarUtilizadorDuplicadoDa409() {
        como(tokenAdmin()).contentType(ContentType.JSON)
                .body(Map.of("username", "colaborador", "password", "password123", "name", "Dup",
                        "email", "dup@projeto.com", "role", "COLABORADOR"))
                .post("/users").then().statusCode(409);
    }

    @Test
    void adminCriaEApagaUtilizadorSemConteudo() {
        String admin = tokenAdmin();
        String username = "tmp" + UUID.randomUUID().toString().substring(0, 8);
        como(admin).contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", "password123", "name", "Temporário",
                        "email", username + "@projeto.com", "role", "COLABORADOR"))
                .post("/users").then().statusCode(201).body("username", equalTo(username));
        como(admin).delete("/users/" + username).then().statusCode(204);
        como(admin).get("/users/view/" + username).then().statusCode(404);
    }

    @Test
    void apagarUtilizadorComConteudoDa400() {
        // o colaborador tem publicações/comentários no seed: deve sugerir suspender
        como(tokenAdmin()).delete("/users/colaborador").then().statusCode(400);
    }

    @Test
    void colaboradorNaoApagaTagsNemDesassociaTagsDePublicacoes() {
        String colab = tokenColaborador();
        como(colab).delete("/tags/Projeto X").then().statusCode(403);
        long id = idPorTitulo(colab, "Deep Learning");
        como(colab).contentType(ContentType.JSON).body("Projeto X")
                .delete("/publicacoes/" + id + "/tags").then().statusCode(403);
    }

    @Test
    void colaboradorNaoOcultaPublicacoesNemComentarios() {
        String colab = tokenColaborador();
        long id = idPorTitulo(colab, "Deep Learning");
        como(colab).put("/publicacoes/" + id + "/ocultar").then().statusCode(403);
        como(colab).put("/comentarios/1/ocultar").then().statusCode(403);
    }
}
