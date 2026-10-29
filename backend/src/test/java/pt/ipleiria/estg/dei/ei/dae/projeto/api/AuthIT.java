package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

class AuthIT extends ApiTestBase {

    @Test
    void loginValidoDevolveToken() {
        org.junit.jupiter.api.Assertions.assertTrue(tokenColaborador().split("\\.").length == 3);
    }

    @Test
    void passwordErradaDa401() {
        given().contentType(ContentType.JSON).body(Map.of("username", "colaborador", "password", "errada"))
                .post("/auth/login").then().statusCode(401);
    }

    @Test
    void contaSuspensaNaoFazLogin() {
        given().contentType(ContentType.JSON).body(Map.of("username", "suspenso", "password", "suspenso1"))
                .post("/auth/login").then().statusCode(401);
    }

    @Test
    void semTokenOuComTokenInvalidoDa401() {
        given().get("/publicacoes/").then().statusCode(401);
        como("isto.nao.eumtoken").get("/publicacoes/").then().statusCode(401);
    }

    @Test
    void bloqueiaLoginAposVariasFalhas() {
        String username = "naoexiste_" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "x"))
                    .post("/auth/login").then().statusCode(401);
        }
        given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "x"))
                .post("/auth/login").then().statusCode(429);
    }

    @Test
    void recuperarPasswordNaoRevelaSeOEmailExiste() {
        given().contentType(ContentType.JSON).body(Map.of("email", "ninguem@nao-existe.pt"))
                .post("/auth/forgot-password").then().statusCode(202);
        given().contentType(ContentType.JSON).body(Map.of("email", "colaborador@projeto.com"))
                .post("/auth/forgot-password").then().statusCode(202);
    }

    @Test
    void reporPasswordComTokenInvalidoDa400() {
        given().contentType(ContentType.JSON).body(Map.of("token", "inventado", "newPassword", "novapassword1"))
                .post("/auth/reset-password").then().statusCode(400);
    }
}
