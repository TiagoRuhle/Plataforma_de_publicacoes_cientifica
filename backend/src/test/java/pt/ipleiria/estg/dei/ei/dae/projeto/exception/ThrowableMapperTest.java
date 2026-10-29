package pt.ipleiria.estg.dei.ei.dae.projeto.exception.mappers;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThrowableMapperTest {

    private final ThrowableMapper mapper = new ThrowableMapper();

    @Test
    void conflitoDeVersaoEscondidoEmEmbrulhosDevolve409() {
        // é assim que chega: EJBException -> RollbackException -> OptimisticLockException
        Throwable cadeia = new RuntimeException("EJB",
                new RollbackException("commit falhou", new OptimisticLockException("versão antiga")));
        assertEquals(409, mapper.toResponse(cadeia).getStatus());
        assertEquals(409, mapper.toResponse(new RuntimeException(new StaleObjectStateException("Publicacao", 1L))).getStatus());
    }

    @Test
    void errosNormaisContinuamAser500() {
        assertEquals(500, mapper.toResponse(new IllegalStateException("bug")).getStatus());
        assertFalse(ThrowableMapper.eConflitoDeVersao(new RuntimeException(new IllegalStateException())));
    }

    @Test
    void respostasHttpDeliberadasPassamSemAlteracao() {
        Response r = mapper.toResponse(new ForbiddenException("não"));
        assertEquals(403, r.getStatus());
    }

    @Test
    void naoEntraEmCicloComCausasCirculares() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);
        assertFalse(ThrowableMapper.eConflitoDeVersao(a)); // termina graças ao limite de 12 níveis
    }
}
