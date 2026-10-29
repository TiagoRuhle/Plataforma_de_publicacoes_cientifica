package pt.ipleiria.estg.dei.ei.dae.projeto.config;

/**
 * Configuração da aplicação lida de variáveis de ambiente (com valores por omissão
 * pensados para o docker-compose). Nada de segredos no código.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static String get(String chave, String omissao) {
        String valor = System.getenv(chave);
        return (valor == null || valor.isBlank()) ? omissao : valor.trim();
    }

    public static int getInt(String chave, int omissao) {
        try {
            return Integer.parseInt(get(chave, String.valueOf(omissao)));
        } catch (NumberFormatException e) {
            return omissao;
        }
    }

    // ---- IA (Ollama) ----
    public static String ollamaUrl() {
        return get("OLLAMA_URL", "http://ollama:11434");
    }

    public static String ollamaModel() {
        return get("OLLAMA_MODEL", "llama3.2");
    }

    /** Timeout de leitura do pedido ao LLM (em CPU pode demorar minutos). */
    public static int aiTimeoutSeconds() {
        return getInt("AI_TIMEOUT_SECONDS", 600);
    }

    public static int aiChunkChars() {
        return getInt("AI_CHUNK_CHARS", 12000);
    }

    public static int aiMaxChunks() {
        return getInt("AI_MAX_CHUNKS", 8);
    }

    /** Quantos resumos podem estar a ser gerados em simultâneo (o Ollama em CPU processa um de cada vez). */
    public static int aiMaxConcurrent() {
        return Math.max(1, getInt("AI_MAX_CONCURRENT", 1));
    }

    /** Quantos pedidos podem esperar na fila; acima disto o resumo falha logo com "fila cheia". */
    public static int aiMaxQueue() {
        return Math.max(0, getInt("AI_MAX_QUEUE", 20));
    }

    /** Tempo máximo que um pedido espera na fila antes de desistir. */
    public static int aiQueueWaitSeconds() {
        return getInt("AI_QUEUE_WAIT_SECONDS", 1800);
    }

    /** Falhas seguidas do serviço de IA que abrem o circuit breaker. */
    public static int aiBreakerFailures() {
        return Math.max(1, getInt("AI_BREAKER_FAILURES", 3));
    }

    /** Durante quanto tempo o circuit breaker fica aberto (falha logo, sem tentar o Ollama). */
    public static int aiBreakerOpenSeconds() {
        return Math.max(1, getInt("AI_BREAKER_OPEN_SECONDS", 60));
    }

    // ---- Extração de texto de PDF (limites contra PDFs maliciosos ou gigantes) ----
    public static int pdfMaxPages() {
        return Math.max(1, getInt("PDF_MAX_PAGES", 150));
    }

    public static int pdfMaxChars() {
        return Math.max(1000, getInt("PDF_MAX_CHARS", 400_000));
    }

    public static int pdfTimeoutSeconds() {
        return Math.max(1, getInt("PDF_TIMEOUT_SECONDS", 60));
    }

    /** Memória máxima do PDFBox antes de passar a ficheiros temporários. */
    public static int pdfMaxMemoryMb() {
        return Math.max(4, getInt("PDF_MAX_MEMORY_MB", 32));
    }

    // ---- Ficheiros ----
    public static String uploadDir() {
        return get("UPLOAD_DIR", "/data/uploads");
    }

    public static int maxUploadMb() {
        return getInt("MAX_UPLOAD_MB", 50);
    }

    // ---- E-mail ----
    public static String smtpHost() {
        return get("SMTP_HOST", "smtp");
    }

    public static String smtpPort() {
        return get("SMTP_PORT", "25");
    }

    public static String mailFrom() {
        return get("MAIL_FROM", "noreply@publicacoes.local");
    }

    /** URL base do frontend, usada nos links dos e-mails. */
    public static String appBaseUrl() {
        String url = get("APP_BASE_URL", "http://localhost:5173");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
