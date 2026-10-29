package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Cliente da API padrão do Ollama (POST /api/generate).
 * JSON feito com Jackson (sem parsing manual), corpo em UTF-8, timeouts e retries.
 */
public class OllamaClient implements ResumoPipeline.Gerador {

    private static final Logger logger = Logger.getLogger(OllamaClient.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String baseUrl;
    private final String model;
    private final Duration readTimeout;
    private final int tentativas;
    private final long pausaMs;

    public OllamaClient(String baseUrl, String model, int timeoutSegundos) {
        this(baseUrl, model, timeoutSegundos, 3, 5000);
    }

    public OllamaClient(String baseUrl, String model, int timeoutSegundos, int tentativas, long pausaMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.readTimeout = Duration.ofSeconds(timeoutSegundos);
        this.tentativas = Math.max(1, tentativas);
        this.pausaMs = pausaMs;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String gerar(String prompt) throws IOException {
        String corpo = construirPedido(model, prompt);
        IOException ultimo = null;
        for (int i = 1; i <= tentativas; i++) {
            try {
                return enviar(corpo);
            } catch (ErroTemporario e) {
                ultimo = e;
                logger.warning("Ollama indisponível (tentativa " + i + "/" + tentativas + "): " + e.getMessage());
                if (i < tentativas) {
                    dormir(Backoff.pausaComJitter(pausaMs, i)); // exponencial, com jitter
                }
            }
        }
        throw new IOException("O serviço de IA não está disponível: "
                + (ultimo != null ? ultimo.getMessage() : "erro desconhecido"), ultimo);
    }

    private String enviar(String corpo) throws IOException {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
                .timeout(readTimeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resposta;
        try {
            resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Pedido ao Ollama interrompido", e);
        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            throw new ErroTemporario("sem ligação a " + baseUrl, e);
        }
        int estado = resposta.statusCode();
        if (estado == 404) {
            throw new IOException("Modelo '" + model + "' não encontrado no Ollama (executa: ollama pull " + model + ").");
        }
        if (estado == 503 || estado == 502) {
            throw new ErroTemporario("HTTP " + estado, null);
        }
        if (estado != 200) {
            throw new IOException("Ollama respondeu HTTP " + estado + ": " + abreviar(resposta.body()));
        }
        return extrairResposta(resposta.body());
    }

    static String construirPedido(String model, String prompt) throws IOException {
        ObjectNode raiz = MAPPER.createObjectNode();
        raiz.put("model", model);
        raiz.put("prompt", prompt);
        raiz.put("stream", false);
        ObjectNode opcoes = raiz.putObject("options");
        opcoes.put("temperature", 0.2);
        opcoes.put("num_ctx", 8192);
        return MAPPER.writeValueAsString(raiz);
    }

    /** Extrai o campo "response" da resposta do Ollama. */
    public static String extrairResposta(String json) throws IOException {
        JsonNode raiz = MAPPER.readTree(json);
        if (raiz == null || raiz.isMissingNode()) {
            throw new IOException("Resposta vazia do Ollama.");
        }
        if (raiz.hasNonNull("error")) {
            throw new IOException("Ollama: " + raiz.get("error").asText());
        }
        String texto = raiz.path("response").asText("");
        if (texto.isBlank()) {
            throw new IOException("O Ollama devolveu uma resposta sem texto.");
        }
        return texto;
    }

    private static String abreviar(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class ErroTemporario extends IOException {
        ErroTemporario(String msg, Throwable causa) {
            super(msg, causa);
        }
    }
}
