package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.AuthDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.ForgotPasswordDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.ResetPasswordDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.RecuperacaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.LoginAttemptTracker;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.TokenIssuer;

import java.time.Duration;

@Path("auth")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class AuthService {

    /** 5 falhas seguidas => 5 minutos de bloqueio para esse username. */
    private static final LoginAttemptTracker TENTATIVAS = new LoginAttemptTracker(5, Duration.ofMinutes(5));

    @EJB
    private UserBean userBean;

    @EJB
    private RecuperacaoBean recuperacaoBean;

    @POST
    @Path("/login")
    public Response authenticate(@Valid AuthDTO auth) {
        String username = auth.getUsername();

        if (TENTATIVAS.bloqueado(username)) {
            return Response.status(429)
                    .header("Retry-After", TENTATIVAS.segundosRestantes(username))
                    .entity("Demasiadas tentativas falhadas. Tente novamente mais tarde.")
                    .build();
        }

        if (userBean.canLogin(username, auth.getPassword())) {
            TENTATIVAS.limpar(username);
            User user = userBean.findByUsername(username);
            String token = TokenIssuer.issue(username, user.getRole());
            return Response.ok(token).build();
        }

        TENTATIVAS.registarFalha(username);
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    /** Pede um e-mail de recuperação. Responde sempre 202, exista ou não o e-mail (sem enumeração). */
    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid ForgotPasswordDTO dto) {
        recuperacaoBean.pedir(dto.getEmail());
        return Response.accepted().build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordDTO dto) {
        recuperacaoBean.repor(dto.getToken(), dto.getNewPassword());
        return Response.noContent().build();
    }
}
