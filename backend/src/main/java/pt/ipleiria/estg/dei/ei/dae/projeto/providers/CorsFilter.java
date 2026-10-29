package pt.ipleiria.estg.dei.ei.dae.projeto.providers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * CORS restrito à origem do frontend (CORS_ORIGIN), sem credenciais (a autenticação é por cabeçalho
 * Authorization, não por cookies). Expõe Content-Disposition e Retry-After para o frontend os poder ler.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {
    private static final String ORIGIN =
            System.getenv().getOrDefault("CORS_ORIGIN", "http://localhost:5173");

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        var h = res.getHeaders();
        h.putSingle("Access-Control-Allow-Origin", ORIGIN);
        h.putSingle("Vary", "Origin");
        h.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        h.putSingle("Access-Control-Allow-Headers", "Authorization, Content-Type");
        h.putSingle("Access-Control-Expose-Headers", "Content-Disposition, Content-Length, Retry-After");
        if (req.getMethod().equals("OPTIONS")) {
            res.setStatus(Response.Status.NO_CONTENT.getStatusCode());
        }
    }
}
