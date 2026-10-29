package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.SubscricaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.SubscricaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;

@Path("subscricao")
public class SubscricaoService {

    @EJB
    private SubscricaoBean subscricaoBean;

    @Context
    private SecurityContext securityContext;

    // SUBSCREVER A UMA TAG
    @POST
    @Path("/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response createSubscricao(SubscricaoDTO subscricaoDTO)
            throws MyEntityNotFoundException, MyEntityExistsException {
        exigirNome(subscricaoDTO);
        String username = securityContext.getUserPrincipal().getName();
        subscricaoBean.subscrever(username, subscricaoDTO.getNome(), isGestor());
        return Response.status(Response.Status.CREATED).build();
    }

    // DESSUBSCREVER A UMA TAG
    @DELETE
    @Path("/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response deleteSubscricao(SubscricaoDTO subscricaoDTO) throws MyEntityNotFoundException {
        exigirNome(subscricaoDTO);
        String username = securityContext.getUserPrincipal().getName();
        subscricaoBean.dessubscrever(username, subscricaoDTO.getNome());
        return Response.noContent().build();
    }

    private void exigirNome(SubscricaoDTO dto) {
        if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
            throw new BusinessRuleException("O nome da tag é obrigatório.");
        }
    }

    private boolean isGestor() {
        return securityContext.isUserInRole("RESPONSAVEL") || securityContext.isUserInRole("ADMINISTRADOR");
    }
}
