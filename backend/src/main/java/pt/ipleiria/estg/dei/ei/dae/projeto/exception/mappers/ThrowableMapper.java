package pt.ipleiria.estg.dei.ei.dae.projeto.exception.mappers;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.StaleStateException;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {
    private static final Logger logger = Logger.getLogger(ThrowableMapper.class.getName());

    static final String MENSAGEM_CONFLITO =
            "Os dados foram alterados por outra pessoa entretanto. Recarregue e tente novamente.";

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        if (eConflitoDeVersao(e)) {
            // duas pessoas gravaram ao mesmo tempo: não é um erro do servidor
            logger.log(Level.INFO, "Conflito de versão (controlo de concorrência otimista)", e);
            return Response.status(Response.Status.CONFLICT).entity(MENSAGEM_CONFLITO).type(MediaType.TEXT_PLAIN).build();
        }
        logger.log(Level.SEVERE, "Erro não tratado", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Um conflito de versão chega embrulhado: o EJB (EJBException / EJBTransactionRolledbackException) e o commit
     * (RollbackException) escondem a OptimisticLockException na cadeia de causas. Percorre-a.
     */
    static boolean eConflitoDeVersao(Throwable erro) {
        Throwable atual = erro;
        for (int i = 0; atual != null && i < 12; i++, atual = atual.getCause()) {
            if (atual instanceof OptimisticLockException || atual instanceof StaleStateException) {
                return true;
            }
        }
        return false;
    }
}
