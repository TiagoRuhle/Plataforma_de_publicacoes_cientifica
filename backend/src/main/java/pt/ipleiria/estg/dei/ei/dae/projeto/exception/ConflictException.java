package pt.ipleiria.estg.dei.ei.dae.projeto.exception;

import jakarta.ejb.ApplicationException;

/**
 * Conflito de edição: o recurso foi alterado por outra pessoa depois de o cliente o ter lido (HTTP 409).
 * {@code @ApplicationException} evita que o contentor EJB a embrulhe em EJBException.
 */
@ApplicationException(rollback = true)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
