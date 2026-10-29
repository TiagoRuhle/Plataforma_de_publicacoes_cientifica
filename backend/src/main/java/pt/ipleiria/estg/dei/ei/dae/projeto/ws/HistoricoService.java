package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.HistoricoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.HistoricoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.PublicacaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("historico")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoricoService {

    @EJB
    private HistoricoBean historicoBean;

    @EJB
    private UserBean userBean;

    @EJB
    private PublicacaoBean publicacaoBean;

    @Context
    private SecurityContext securityContext;

    /** Histórico de toda a plataforma (admin). */
    @GET
    @Path("")
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response getAllHistorico() {
        List<HistoricoDTO> historico = HistoricoDTO.from(historicoBean.findAll());
        return Response.ok(historico).build();
    }

    @GET
    @Path("meu")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getMeuHistorico() throws MyEntityNotFoundException {
        User user = utilizador(me());
        return Response.ok(HistoricoDTO.from(historicoBean.findByUser(user))).build();
    }

    /** Histórico de qualquer utilizador (admin). */
    @GET
    @Path("user/{username}")
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response getHistoricoByUser(@PathParam("username") String username) throws MyEntityNotFoundException {
        User user = utilizador(username);
        return Response.ok(HistoricoDTO.from(historicoBean.findByUser(user))).build();
    }

    /** Histórico de edições de uma publicação: só o dono e gestores. */
    @GET
    @Path("publicacao/{id}")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getHistoricoByPublicacao(@PathParam("id") long publicacaoId)
            throws MyEntityNotFoundException {
        Publicacao publicacao = publicacaoBean.findAcessivel(publicacaoId, me(), isGestor());
        exigirDonoOuGestor(publicacao);
        return Response.ok(HistoricoDTO.from(historicoBean.findByPublicacao(publicacao))).build();
    }

    @GET
    @Path("tipo/{tipoAcao}")
    @Authenticated
    @RolesAllowed({ "ADMINISTRADOR" })
    public Response getHistoricoByTipo(@PathParam("tipoAcao") String tipoAcao) {
        TipoAcao tipo = tipo(tipoAcao);
        if (tipo == null) {
            return tipoInvalido(tipoAcao);
        }
        return Response.ok(HistoricoDTO.from(historicoBean.findByTipoAcao(tipo))).build();
    }

    @GET
    @Path("meu/tipo/{tipoAcao}")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getMeuHistoricoByTipo(@PathParam("tipoAcao") String tipoAcao) throws MyEntityNotFoundException {
        User user = utilizador(me());
        TipoAcao tipo = tipo(tipoAcao);
        if (tipo == null) {
            return tipoInvalido(tipoAcao);
        }
        return Response.ok(HistoricoDTO.from(historicoBean.findByUserAndTipoAcao(user, tipo))).build();
    }

    @GET
    @Path("meu/estatisticas")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getMinhasEstatisticas() throws MyEntityNotFoundException {
        User user = utilizador(me());
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("totalAcoes", historicoBean.contarAcoesUser(user));
        corpo.put("username", me());
        return Response.ok(corpo).build();
    }

    @GET
    @Path("publicacao/{id}/estatisticas")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getEstatisticasPublicacao(@PathParam("id") long publicacaoId) throws MyEntityNotFoundException {
        // findAcessivel: não revela a existência de publicações ocultas
        Publicacao publicacao = publicacaoBean.findAcessivel(publicacaoId, me(), isGestor());
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("totalAcoes", historicoBean.contarAcoesPublicacao(publicacao));
        corpo.put("publicacaoId", publicacaoId);
        return Response.ok(corpo).build();
    }

    // ---------------------------------------------------------------- auxiliares

    private String me() {
        return securityContext.getUserPrincipal().getName();
    }

    private boolean isGestor() {
        return securityContext.isUserInRole("RESPONSAVEL") || securityContext.isUserInRole("ADMINISTRADOR");
    }

    private void exigirDonoOuGestor(Publicacao p) {
        boolean dono = p.getSubmetidoPor() != null && p.getSubmetidoPor().getUsername().equals(me());
        if (!dono && !isGestor()) {
            throw new ForbiddenException("Sem acesso ao histórico desta publicação.");
        }
    }

    private User utilizador(String username) throws MyEntityNotFoundException {
        User user = userBean.findByUsername(username);
        if (user == null) {
            throw new MyEntityNotFoundException("Utilizador não encontrado: " + username);
        }
        return user;
    }

    private static TipoAcao tipo(String texto) {
        try {
            return TipoAcao.valueOf(texto.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Response tipoInvalido(String texto) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Tipo de ação inválido: " + texto).build();
    }
}
