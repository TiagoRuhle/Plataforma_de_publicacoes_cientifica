package pt.ipleiria.estg.dei.ei.dae.projeto.exception.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.NotAuthorizedException;

import java.util.logging.Logger;

@Provider
public class NotAuthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {
    private static final Logger logger = Logger.getLogger(MyEntityNotFoundException.class.getCanonicalName());

    @Override
    public Response toResponse(NotAuthorizedException e) {
        String errorMsg = e.getMessage();
        logger.warning("ERROR: " + errorMsg);
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}
