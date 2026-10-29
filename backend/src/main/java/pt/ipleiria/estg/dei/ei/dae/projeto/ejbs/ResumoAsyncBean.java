package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import pt.ipleiria.estg.dei.ei.dae.projeto.ai.CircuitBreaker;
import pt.ipleiria.estg.dei.ei.dae.projeto.ai.OllamaClient;
import pt.ipleiria.estg.dei.ei.dae.projeto.ai.ResumoLimiter;
import pt.ipleiria.estg.dei.ei.dae.projeto.ai.ResumoPipeline;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;
import pt.ipleiria.estg.dei.ei.dae.projeto.utils.PdfTextExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gera resumos com a IA em background, com proteções para que a IA nunca derrube a aplicação:
 * <ul>
 *   <li><b>concorrência limitada</b> (AI_MAX_CONCURRENT, por omissão 1) e <b>fila limitada</b> (AI_MAX_QUEUE):
 *       o Ollama em CPU trata um pedido de cada vez, e a extração de PDF também fica serializada;</li>
 *   <li><b>circuit breaker</b>: depois de falhas seguidas do Ollama, os resumos falham logo durante um tempo
 *       em vez de cada um gastar tentativas e minutos;</li>
 *   <li><b>sem duplicados</b>: se o mesmo resumo já está em fila ou a correr, o novo pedido é ignorado;</li>
 *   <li><b>conflitos de versão</b>: se um utilizador editar a publicação ao mesmo tempo, a gravação repete-se
 *       sobre os dados atuais (e nunca sobrepõe uma correção manual).</li>
 * </ul>
 */
@Stateless
public class ResumoAsyncBean {

    private static final Logger logger = Logger.getLogger(ResumoAsyncBean.class.getName());

    private static final ResumoLimiter LIMITER =
            new ResumoLimiter(AppConfig.aiMaxConcurrent(), AppConfig.aiMaxQueue());
    private static final CircuitBreaker BREAKER =
            new CircuitBreaker(AppConfig.aiBreakerFailures(), AppConfig.aiBreakerOpenSeconds() * 1000L);
    private static final Set<Long> EM_CURSO = ConcurrentHashMap.newKeySet();

    private static final int TENTATIVAS_GRAVACAO = 3;

    @EJB
    private ResumoStoreBean store;

    /**
     * NOT_SUPPORTED: a chamada ao LLM pode demorar minutos e não pode ocupar uma ligação à BD nem estourar
     * o timeout de transação do WildFly.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void gerarResumo(long publicacaoId) {
        if (!EM_CURSO.add(publicacaoId)) {
            logger.info("Resumo da publicação " + publicacaoId + " já está em fila ou a correr; pedido ignorado.");
            return;
        }
        try {
            ResumoStoreBean.DadosResumo dados = store.carregar(publicacaoId);
            if (dados == null) {
                logger.warning("Resumo pedido para publicação inexistente: " + publicacaoId);
                return;
            }
            try {
                String resumo = LIMITER.executar(() -> gerar(publicacaoId, dados),
                        AppConfig.aiQueueWaitSeconds() * 1000L);
                gravar(() -> store.guardarSucesso(publicacaoId, resumo), publicacaoId);
                logger.info("Resumo da publicação " + publicacaoId + " concluído");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Falha ao gerar resumo da publicação " + publicacaoId, e);
                gravar(() -> store.guardarErro(publicacaoId, e.getMessage()), publicacaoId);
            }
        } finally {
            EM_CURSO.remove(publicacaoId);
        }
    }

    private String gerar(long publicacaoId, ResumoStoreBean.DadosResumo dados) throws IOException {
        logger.info("A gerar resumo da publicação " + publicacaoId);
        String texto = PdfTextExtractor.extrairTexto(Path.of(dados.filepath()));

        OllamaClient cliente = new OllamaClient(
                AppConfig.ollamaUrl(), AppConfig.ollamaModel(), AppConfig.aiTimeoutSeconds());
        ResumoPipeline pipeline = new ResumoPipeline(cliente, AppConfig.aiChunkChars(), AppConfig.aiMaxChunks());
        return pipeline.resumir(texto, protegido(cliente));
    }

    /** Cada chamada ao Ollama passa pelo circuit breaker. Erros do documento (sem texto...) não contam. */
    private static ResumoPipeline.Gerador protegido(OllamaClient cliente) {
        return prompt -> {
            if (!BREAKER.permitir()) {
                throw new IOException("O serviço de IA está temporariamente indisponível (várias falhas seguidas). "
                        + "Tente novamente dentro de alguns minutos.");
            }
            try {
                String resposta = cliente.gerar(prompt);
                BREAKER.sucesso();
                return resposta;
            } catch (IOException | RuntimeException e) {
                BREAKER.falha();
                throw e;
            }
        };
    }

    /** Se alguém alterou a publicação entretanto (conflito de versão), repete sobre os dados atuais. */
    private void gravar(Runnable gravacao, long publicacaoId) {
        for (int tentativa = 1; tentativa <= TENTATIVAS_GRAVACAO; tentativa++) {
            try {
                gravacao.run();
                return;
            } catch (RuntimeException e) {
                logger.log(Level.INFO, "Conflito ao gravar o resumo da publicação " + publicacaoId
                        + " (tentativa " + tentativa + "/" + TENTATIVAS_GRAVACAO + ")", e);
            }
        }
        logger.warning("Não foi possível gravar o resumo da publicação " + publicacaoId + ".");
    }
}
