package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OllamaClientTest {

    @Test
    void extraiRespostaComAspasEAcentos() throws IOException {
        // o parser antigo cortava o texto na primeira aspa
        String json = "{\"model\":\"llama3.2\",\"response\":\"Ele disse \\\"olá\\\" e \\u00e7a\\nfim\",\"done\":true}";
        assertEquals("Ele disse \"olá\" e ça\nfim", OllamaClient.extrairResposta(json));
    }

    @Test
    void erroDoOllamaViraIOException() {
        IOException e = assertThrows(IOException.class,
                () -> OllamaClient.extrairResposta("{\"error\":\"model not found\"}"));
        assertTrue(e.getMessage().contains("model not found"));
    }

    @Test
    void respostaSemTextoViraIOException() {
        assertThrows(IOException.class, () -> OllamaClient.extrairResposta("{\"response\":\"  \"}"));
    }

    @Test
    void pedidoEJsonValidoComPromptEspecial() throws IOException {
        String prompt = "linha1\n\"citação\" \\ ç";
        JsonNode corpo = new ObjectMapper().readTree(OllamaClient.construirPedido("llama3.2", prompt));
        assertEquals("llama3.2", corpo.get("model").asText());
        assertEquals(prompt, corpo.get("prompt").asText());
        assertFalse(corpo.get("stream").asBoolean());
    }
}
