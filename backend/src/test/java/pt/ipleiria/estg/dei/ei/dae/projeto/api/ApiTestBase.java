package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Base dos testes de integração. Correm contra a aplicação JÁ em execução (docker compose up + make deploy)
 * e usam o seed do ConfigBean. URL configurável: -Dapi.baseUrl=http://localhost:8080/projeto/api
 */
public abstract class ApiTestBase {

    protected static final String BASE_URL = System.getProperty("api.baseUrl", "http://localhost:8080/projeto/api");

    @BeforeAll
    static void configurar() {
        RestAssured.baseURI = BASE_URL;
    }

    protected static String login(String username, String password) {
        String token = given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().asString();
        return token.replace("\"", "").trim();
    }

    protected static String tokenAdmin() { return login("administrador", "administrador"); }
    protected static String tokenResponsavel() { return login("responsavel", "responsavel"); }
    protected static String tokenColaborador() { return login("colaborador", "colaborador"); }
    protected static String tokenColaborador2() { return login("colaborador2", "colaborador2"); }

    protected static RequestSpecification como(String token) {
        return given().header("Authorization", "Bearer " + token);
    }

    /** Procura uma publicação pelo título (como o utilizador indicado) e devolve o id, ou -1 se não a vê. */
    @SuppressWarnings("unchecked")
    protected static long idPorTitulo(String token, String titulo) {
        List<Map<String, Object>> items = como(token).queryParam("titulo", titulo)
                .get("/publicacoes/search").then().statusCode(200)
                .extract().jsonPath().getList("items");
        return items.isEmpty() ? -1 : ((Number) items.get(0).get("id")).longValue();
    }
}
