package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.CreateUserDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.SubscricaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.UpdatePasswordDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.UserDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.SubscricaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;

import java.util.List;

@Path("users")
public class UserService {

    @EJB
    private UserBean userBean;

    @EJB
    private SubscricaoBean subscricaoBean;

    @Context
    private SecurityContext securityContext;

    // LISTAR TODOS (admin)
    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public List<UserDTO> getAllUsers() {
        return UserDTO.from(userBean.findAll());
    }

    // UPDATE EMAIL (o próprio ou admin)
    @PATCH
    @Path("{username}/updateEmail")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response updateUserEmail(@PathParam("username") String username, UserDTO userDTO)
            throws MyEntityNotFoundException, MyEntityExistsException {
        requireSelfOrAdmin(username);
        userBean.updateEmail(username, userDTO.getEmail());

        return Response.ok(UserDTO.from(userBean.findByUsername(username))).build();
    }

    // UPDATE PASSWORD (só o próprio)
    @PATCH
    @Path("{username}/updatePassword")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response updateUserPassword(@PathParam("username") String username, UpdatePasswordDTO updatePasswordDTO)
            throws MyEntityNotFoundException {
        requireSelf(username);
        userBean.updatePassword(username, updatePasswordDTO.getOldPassword(), updatePasswordDTO.getNewPassword());

        return Response.ok().build();
    }

    // GET ME
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response getAuthenticatedUser() {
        String username = securityContext.getUserPrincipal().getName();

        User user = userBean.findByUsername(username);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }

    // CREATE USER (admin)
    @POST
    @Path("/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response createUser(CreateUserDTO createUserDTO) throws MyEntityExistsException {
        userBean.create(createUserDTO.getUsername(), createUserDTO.getPassword(), createUserDTO.getName(),
                createUserDTO.getEmail(), createUserDTO.getRole());

        User user = userBean.findByUsername(createUserDTO.getUsername());

        return Response.status(Response.Status.CREATED).entity(UserDTO.from(user)).build();
    }

    // CHANGE ROLE (admin; não pode alterar o seu próprio role)
    @PATCH
    @Path("{username}/updateRole")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response updateRole(@PathParam("username") String username, UserDTO userDTO)
            throws MyEntityNotFoundException {
        forbidSelf(username, "Não pode alterar o seu próprio role.");
        userBean.updateRole(username, userDTO.getRole());

        return Response.ok(UserDTO.from(userBean.findByUsername(username))).build();
    }

    // UPDATE STATUS (admin; não pode suspender a própria conta)
    @PATCH
    @Path("{username}/updateStatus")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response updateStatus(@PathParam("username") String username, UserDTO userDTO)
            throws MyEntityNotFoundException {
        forbidSelf(username, "Não pode suspender a sua própria conta.");
        userBean.updateStatus(username, userDTO.isActive());

        return Response.ok(UserDTO.from(userBean.findByUsername(username))).build();
    }

    // DELETE USER (admin; não pode apagar a própria conta)
    @DELETE
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response deleteUser(@PathParam("username") String username) throws MyEntityNotFoundException {
        forbidSelf(username, "Não pode apagar a sua própria conta.");
        userBean.delete(username);

        return Response.noContent().build(); // 204
    }

    // GET A USER (admin)
    @GET
    @Path("/view/{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response getUserByUsername(@PathParam("username") String username) {
        User user = userBean.findByUsername(username);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }

    // EDIT USER (o próprio ou admin) - só nome e email; nunca role nem active
    @PUT
    @Path("{username}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response updateUser(@PathParam("username") String username, UserDTO userDTO)
            throws MyEntityNotFoundException, MyEntityExistsException {
        requireSelfOrAdmin(username);
        userBean.updateUser(username, userDTO);

        return Response.ok(UserDTO.from(userBean.findByUsername(username))).build();
    }

    // AS MINHAS SUBSCRIÇÕES
    @GET
    @Path("/view/subscricoes")
    @Produces({ MediaType.APPLICATION_JSON })
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getSubscricoes() {
        String username = securityContext.getUserPrincipal().getName();

        List<SubscricaoDTO> subscricoes = subscricaoBean.findAllByUser(username);

        return Response.ok(subscricoes).build();
    }

    // ---------- helpers de autorização ----------

    private void requireSelfOrAdmin(String username) {
        String me = securityContext.getUserPrincipal().getName();
        if (!me.equals(username) && !securityContext.isUserInRole("ADMINISTRADOR")) {
            throw new ForbiddenException("Só pode alterar os seus próprios dados.");
        }
    }

    private void requireSelf(String username) {
        if (!securityContext.getUserPrincipal().getName().equals(username)) {
            throw new ForbiddenException("Só pode alterar a sua própria password.");
        }
    }

    private void forbidSelf(String username, String message) {
        if (securityContext.getUserPrincipal().getName().equals(username)) {
            throw new ForbiddenException(message);
        }
    }
}