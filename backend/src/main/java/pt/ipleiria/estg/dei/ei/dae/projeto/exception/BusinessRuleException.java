package pt.ipleiria.estg.dei.ei.dae.projeto.exception;

import jakarta.ejb.ApplicationException;

/**
 * Violação de uma regra de negócio (dados inválidos, estado incompatível...).
 * {@code @ApplicationException} evita que o contentor EJB a embrulhe em EJBException,
 * para que o mapper devolva 400 em vez de 500.
 */
@ApplicationException(rollback = true)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
