package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import io.jsonwebtoken.JwtException;
import jakarta.ws.rs.core.Response;

import javax.crypto.spec.SecretKeySpec;
import java.security.Principal;

@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    @EJB
    private UserBean userBean;
    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext ctx) {
        var header = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(ctx, "Authorization header must be provided");
            return;
        }
        Claims claims;
        try {
            claims = decodeToken(header.substring("Bearer ".length()).trim());
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(ctx, "Invalid or expired token");
            return;
        }

        var user = userBean.findByUsername(claims.getSubject());
        if (user == null || !user.isActive()) { // apagado ou suspenso
            unauthorized(ctx, "User not found or suspended");
            return;
        }
        final String role = user.getRole();

        ctx.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return user::getUsername;
            }

            @Override
            public boolean isUserInRole(String s) {
                return role.equalsIgnoreCase(s);
            }

            @Override
            public boolean isSecure() {
                return uriInfo.getAbsolutePath().toString().startsWith("https");
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });
    }

    private void unauthorized(ContainerRequestContext ctx, String msg) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(msg).build());
    }

    private Claims decodeToken(String token) {
        var key = new SecretKeySpec(TokenIssuer.SECRET_KEY, TokenIssuer.ALGORITHM);

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}