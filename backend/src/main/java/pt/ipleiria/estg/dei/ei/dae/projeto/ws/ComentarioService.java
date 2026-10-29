package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.ComentarioDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.CreateComentarioDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.ComentarioBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.PublicacaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Comentario;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;

@Path("comentarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComentarioService {

    @EJB
    private ComentarioBean comentarioBean;

    @EJB
    private UserBean userBean;

    @EJB
    private PublicacaoBean publicacaoBean;

    @Context
    private SecurityContext securityContext;

    @POST
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response create(CreateComentarioDTO dto) throws MyEntityNotFoundException {
        String username = me();
        publicacaoBean.findAcessivel(dto.getPublicacaoId(), username, isGestor()); // 404 se oculta
        User autor = userBean.findByUsername(username);

        Comentario comentario = comentarioBean.create(dto.getPublicacaoId(), autor, dto.getTexto());
        return Response.status(Response.Status.CREATED).entity(ComentarioDTO.from(comentario)).build();
    }

    @PUT
    @Path("{id}/ocultar")
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public Response ocultar(@PathParam("id") long id) throws MyEntityNotFoundException {
        comentarioBean.ocultar(id, userBean.findByUsername(me()));
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}/visivel")
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public Response visivel(@PathParam("id") long id) throws MyEntityNotFoundException {
        comentarioBean.mostrar(id, userBean.findByUsername(me()));
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response delete(@PathParam("id") long id) throws MyEntityNotFoundException {
        Comentario c = comentarioBean.find(id);
        if (c == null) {
            throw new MyEntityNotFoundException("Comentário não encontrado: " + id);
        }
        if (!c.getAutor().getUsername().equals(me()) && !isGestor()) {
            throw new ForbiddenException("Só o autor ou um responsável pode apagar o comentário.");
        }
        comentarioBean.delete(id, userBean.findByUsername(me()));
        return Response.noContent().build();
    }

    private String me() {
        return securityContext.getUserPrincipal().getName();
    }

    private boolean isGestor() {
        return securityContext.isUserInRole("RESPONSAVEL") || securityContext.isUserInRole("ADMINISTRADOR");
    }
}
