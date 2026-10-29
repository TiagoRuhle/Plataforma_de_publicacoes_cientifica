package pt.ipleiria.estg.dei.ei.dae.projeto.api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicacoesIT extends ApiTestBase {

    private static final byte[] PDF_MINIMO = "%PDF-1.4\n%conteudo de teste\n%%EOF\n".getBytes(StandardCharsets.UTF_8);

    private static Response upload(String token, String titulo, String ficheiro, byte[] bytes, String... tags) {
        var req = como(token)
                .multiPart("titulo", titulo)
                .multiPart("tipo", "Artigo Científico")
                .multiPart("areaCientifica", "Ciência de Dados")
                .multiPart("autor", "Autor de Teste")
                .multiPart("ano", "2024")
                .multiPart("file", ficheiro, bytes, "application/octet-stream");
        for (String t : tags) {
            req = req.multiPart("tags", t);
        }
        return req.post("/publicacoes/create");
    }

    private static String titulo() {
        return "IT " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void uploadDePdfEDownloadDevolveOMesmoConteudo() {
        String colab = tokenColaborador();
        String titulo = titulo();
        long id = upload(colab, titulo, "teste.pdf", PDF_MINIMO, "Projeto X").then().statusCode(201)
                .body("titulo", equalTo(titulo))
                .body("submetidoPor", equalTo("colaborador"))
                .body("resumoEstado", anyOf(equalTo("PENDENTE"), equalTo("ERRO"), equalTo("CONCLUIDO")))
                .body("tags.tag", hasItem("Projeto X"))
                .extract().jsonPath().getLong("id");

        Response download = como(colab).get("/publicacoes/" + id + "/download");
        download.then().statusCode(200).header("Content-Disposition", containsString("teste.pdf"))
                .contentType(containsString("application/pdf"));
        assertArrayEquals(PDF_MINIMO, download.asByteArray());

        como(tokenColaborador2()).get("/publicacoes/" + id + "/download").then().statusCode(200);
    }

    @Test
    void uploadDeZipNaoTemResumoAutomatico() {
        byte[] zip = {'P', 'K', 3, 4, 0, 0, 0, 0};
        upload(tokenColaborador(), titulo(), "dados.zip", zip).then().statusCode(201)
                .body("resumoEstado", equalTo("NAO_APLICAVEL"));
    }

    @Test
    void rejeitaFicheirosInvalidos() {
        String colab = tokenColaborador();
        upload(colab, titulo(), "virus.exe", PDF_MINIMO).then().statusCode(400);
        upload(colab, titulo(), "falso.pdf", "isto nao e um pdf".getBytes(StandardCharsets.UTF_8)).then().statusCode(400);
        upload(colab, titulo(), "vazio.pdf", new byte[0]).then().statusCode(400);
    }

    @Test
    void uploadComTagInexistenteDa404ENaoCriaNada() {
        String colab = tokenColaborador();
        String titulo = titulo();
        upload(colab, titulo, "t.pdf", PDF_MINIMO, "tag-que-nao-existe").then().statusCode(404);
        assertEquals(-1, idPorTitulo(colab, titulo), "a publicação não pode ter ficado criada a meio");
    }

    @Test
    void camposObrigatoriosEmFaltaDao400() {
        como(tokenColaborador()).multiPart("titulo", "só título")
                .multiPart("file", "t.pdf", PDF_MINIMO, "application/pdf")
                .post("/publicacoes/create").then().statusCode(400);
    }

    @Test
    void donoCorrigeOResumoEOutroColaboradorNaoPode() {
        String dono = tokenColaborador();
        long id = upload(dono, titulo(), "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");

        como(tokenColaborador2()).contentType(ContentType.JSON).body(Map.of("resumo", "tentativa"))
                .put("/publicacoes/" + id).then().statusCode(403);

        como(dono).contentType(ContentType.JSON).body(Map.of("resumo", "Resumo corrigido pelo dono"))
                .put("/publicacoes/" + id).then().statusCode(200)
                .body("resumoIa", equalTo("Resumo corrigido pelo dono"))
                .body("resumoEstado", equalTo("EDITADO"));

        como(tokenResponsavel()).contentType(ContentType.JSON).body(Map.of("titulo", "Novo título do gestor"))
                .put("/publicacoes/" + id).then().statusCode(200).body("titulo", equalTo("Novo título do gestor"));

        como(dono).get("/historico/publicacao/" + id).then().statusCode(200)
                .body("tipoAcao", hasItems("PUBLICACAO_CRIADA", "RESUMO_EDITADO_MANUALMENTE"));
    }

    @Test
    void edicaoComVersaoDesatualizadaDa409() {
        String dono = tokenColaborador();
        long id = upload(dono, titulo(), "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");
        int versaoInicial = como(dono).get("/publicacoes/" + id).then().statusCode(200).extract().jsonPath().getInt("version");

        // 1.ª edição, com a versão certa: aceite, e a versão avança
        como(dono).contentType(ContentType.JSON).body(Map.of("titulo", "Primeira edição", "version", versaoInicial))
                .put("/publicacoes/" + id).then().statusCode(200).body("titulo", equalTo("Primeira edição"));
        int versaoNova = como(dono).get("/publicacoes/" + id).then().extract().jsonPath().getInt("version");
        assertTrue(versaoNova > versaoInicial, "a versão tem de avançar a cada alteração");

        // 2.ª edição com a versão antiga (como quem abriu o formulário antes): conflito
        como(tokenResponsavel()).contentType(ContentType.JSON).body(Map.of("titulo", "Edição de outra pessoa", "version", versaoInicial))
                .put("/publicacoes/" + id).then().statusCode(409).body(containsString("alterada"));
        como(dono).get("/publicacoes/" + id).then().body("titulo", equalTo("Primeira edição"));

        // com a versão atual, o responsável já consegue
        como(tokenResponsavel()).contentType(ContentType.JSON).body(Map.of("titulo", "Edição de outra pessoa", "version", versaoNova))
                .put("/publicacoes/" + id).then().statusCode(200);
    }

    @Test
    void alterarTagsTambemAvancaAVersao() {
        String resp = tokenResponsavel();
        String dono = tokenColaborador();
        long id = upload(dono, titulo(), "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");
        int antes = como(dono).get("/publicacoes/" + id).then().extract().jsonPath().getInt("version");
        como(resp).put("/publicacoes/" + id + "/ocultar").then().statusCode(200);
        int depois = como(resp).get("/publicacoes/" + id).then().extract().jsonPath().getInt("version");
        assertTrue(depois > antes);
    }

    @Test
    void regenerarResumoSoParaDonoOuGestor() {
        String dono = tokenColaborador();
        long id = upload(dono, titulo(), "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");
        como(tokenColaborador2()).post("/publicacoes/" + id + "/resumo/regenerar").then().statusCode(403);
        como(dono).post("/publicacoes/" + id + "/resumo/regenerar").then().statusCode(202);
    }

    @Test
    void ratingsValidamIntervaloEAtualizam() {
        String colab = tokenColaborador2();
        long id = upload(colab, titulo(), "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");

        como(colab).contentType(ContentType.JSON).body(Map.of("rating", 6)).post("/publicacoes/" + id + "/rating")
                .then().statusCode(400);
        como(colab).contentType(ContentType.JSON).body(Map.of("rating", 3)).put("/publicacoes/" + id + "/rating")
                .then().statusCode(400); // ainda não tem rating para atualizar

        como(colab).contentType(ContentType.JSON).body(Map.of("rating", 4)).post("/publicacoes/" + id + "/rating")
                .then().statusCode(201).body("totalRatings", equalTo(1)).body("ratingMedio", equalTo(4.0f));
        como(colab).contentType(ContentType.JSON).body(Map.of("rating", 2)).put("/publicacoes/" + id + "/rating")
                .then().statusCode(200).body("totalRatings", equalTo(1)).body("ratingMedio", equalTo(2.0f));
        // POST repetido atualiza em vez de duplicar
        como(colab).contentType(ContentType.JSON).body(Map.of("rating", 5)).post("/publicacoes/" + id + "/rating")
                .then().statusCode(201).body("totalRatings", equalTo(1)).body("ratingMedio", equalTo(5.0f));
    }

    @Test
    void comentariosValidamTextoEPermissoesDeApagar() {
        String autor = tokenColaborador();
        long id = idPorTitulo(autor, "Deep Learning");

        como(autor).contentType(ContentType.JSON).body(Map.of("publicacaoId", id, "texto", "   "))
                .post("/comentarios").then().statusCode(400);

        long comentarioId = como(autor).contentType(ContentType.JSON)
                .body(Map.of("publicacaoId", id, "texto", "Comentário de teste " + UUID.randomUUID()))
                .post("/comentarios").then().statusCode(201).extract().jsonPath().getLong("id");

        como(tokenColaborador2()).delete("/comentarios/" + comentarioId).then().statusCode(403);
        como(autor).delete("/comentarios/" + comentarioId).then().statusCode(204);
    }

    @Test
    void responsavelOcultaEMostraPublicacao() {
        String dono = tokenColaborador();
        String resp = tokenResponsavel();
        String titulo = titulo();
        long id = upload(dono, titulo, "t.pdf", PDF_MINIMO).then().statusCode(201).extract().jsonPath().getLong("id");

        como(resp).put("/publicacoes/" + id + "/ocultar").then().statusCode(200).body("visivel", equalTo(false));
        assertEquals(-1, idPorTitulo(tokenColaborador2(), titulo));
        assertTrue(idPorTitulo(dono, titulo) > 0, "o dono continua a ver a sua publicação oculta");

        como(resp).put("/publicacoes/" + id + "/visivel").then().statusCode(200).body("visivel", equalTo(true));
        assertTrue(idPorTitulo(tokenColaborador2(), titulo) > 0);
    }
}
