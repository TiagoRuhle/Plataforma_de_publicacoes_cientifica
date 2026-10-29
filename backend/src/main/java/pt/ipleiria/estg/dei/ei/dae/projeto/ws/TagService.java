package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.TagDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.TagBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;

import java.util.List;

@Path("tags")
public class TagService {

    @EJB
    private TagBean tagBean;

    @Context
    private SecurityContext securityContext;

    // TAGS VISÍVEIS (todos os roles)
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({"COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR"})
    public List<TagDTO> getVisibleTags() {
        return TagDTO.from(tagBean.findVisible());
    }

    // TODAS AS TAGS, incluindo ocultas (responsável / admin)
    @GET
    @Path("/view")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({"RESPONSAVEL", "ADMINISTRADOR"})
    public List<TagDTO> getTags() {
        return TagDTO.from(tagBean.findAll());
    }

    // CREATE TAG
    @POST
    @Path("/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Authenticated
    @RolesAllowed({"RESPONSAVEL", "ADMINISTRADOR"})
    public Response createTag(TagDTO tagDTO) throws MyEntityExistsException {
        boolean visivel = tagDTO.isVisivel() == null || tagDTO.isVisivel();
        tagBean.create(tagDTO.getTag(), visivel);

        Tag tag = tagBean.findByTag(tagDTO.getTag().trim());
        return Response.status(Response.Status.CREATED).entity(TagDTO.from(tag)).build();
    }

    // DELETE TAG
    @DELETE
    @Path("/{tag}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({"RESPONSAVEL", "ADMINISTRADOR"})
    public Response deleteTag(@PathParam("tag") String tag) throws MyEntityNotFoundException {
        tagBean.delete(tag);
        return Response.noContent().build(); // 204
    }
}
