package pt.ipleiria.estg.dei.ei.dae.projeto.exception.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;

import jakarta.ws.rs.core.MediaType;

@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {
    @Override
    public Response toResponse(BusinessRuleException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
}
