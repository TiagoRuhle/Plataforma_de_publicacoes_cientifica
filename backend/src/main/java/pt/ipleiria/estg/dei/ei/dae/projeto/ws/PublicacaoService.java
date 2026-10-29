package pt.ipleiria.estg.dei.ei.dae.projeto.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.NovaPublicacaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.PublicacaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.PublicacaoDetailDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.PublicacaoUpdateDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.RatingDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.PublicacaoBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.PublicacaoSearchBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.search.PaginaDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.search.PublicacaoFiltro;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Authenticated;
import pt.ipleiria.estg.dei.ei.dae.projeto.utils.FicheiroValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("publicacoes")
public class PublicacaoService {

    /** Máximo de resultados nas listagens "completas" (sem paginação) mantidas por compatibilidade. */
    private static final int LISTA_MAX = 1000;
    /** Máximo por página na pesquisa paginada. */
    private static final int PAGINA_MAX = 100;

    @EJB
    private PublicacaoBean publicacaoBean;

    @EJB
    private PublicacaoSearchBean searchBean;

    @EJB
    private UserBean userBean;

    @Context
    private SecurityContext securityContext;

    // ------------------------------------------------------------ listagens

    /** Todas as publicações visíveis para todos. */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public List<PublicacaoDTO> getVisivelPublicacoes() {
        PublicacaoFiltro f = filtroLista();
        f.somenteVisiveis = true;
        return searchBean.pesquisar(f, me(), isGestor(), LISTA_MAX).getItems();
    }

    @GET
    @Path("/view/invisivel")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public List<PublicacaoDTO> getInvisivelPublicacoes() {
        PublicacaoFiltro f = filtroLista();
        f.somenteOcultas = true;
        return searchBean.pesquisar(f, me(), true, LISTA_MAX).getItems();
    }

    @GET
    @Path("/view")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public List<PublicacaoDTO> getAllPublicacoes() {
        return searchBean.pesquisar(filtroLista(), me(), true, LISTA_MAX).getItems();
    }

    /** As minhas publicações (incluindo as ocultas). */
    @GET
    @Path("/view/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public List<PublicacaoDTO> getMinhasPublicacoes() {
        PublicacaoFiltro f = filtroLista();
        f.minhas = true;
        return searchBean.pesquisar(f, me(), isGestor(), LISTA_MAX).getItems();
    }

    /**
     * Pesquisa paginada. Filtros: q, titulo, autor, area, tipo, tag, anoDe, anoAte, desde, ate (AAAA-MM-DD), minhas, ocultas.
     * Ordenação: sort = data|titulo|ano|comentarios|rating|ratings, dir = asc|desc.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public PaginaDTO<PublicacaoDTO> pesquisar(
            @QueryParam("q") String q,
            @QueryParam("titulo") String titulo,
            @QueryParam("autor") String autor,
            @QueryParam("area") String area,
            @QueryParam("tipo") String tipo,
            @QueryParam("tag") String tag,
            @QueryParam("anoDe") Integer anoDe,
            @QueryParam("anoAte") Integer anoAte,
            @QueryParam("desde") String desde,
            @QueryParam("ate") String ate,
            @QueryParam("minhas") @DefaultValue("false") boolean minhas,
            @QueryParam("ocultas") @DefaultValue("false") boolean ocultas,
            @QueryParam("sort") @DefaultValue("data") String sort,
            @QueryParam("dir") @DefaultValue("desc") String dir,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        PublicacaoFiltro f = new PublicacaoFiltro();
        f.q = q;
        f.titulo = titulo;
        f.autor = autor;
        f.area = area;
        f.tipo = tipo;
        f.tag = tag;
        f.anoDe = anoDe;
        f.anoAte = anoAte;
        f.desde = data(desde, "desde");
        f.ate = data(ate, "ate");
        f.minhas = minhas;
        f.somenteOcultas = ocultas; // responsável/admin: todas as ocultas; colaborador: só as suas
        f.sort = sort;
        f.dir = dir;
        f.page = page;
        f.size = size;
        return searchBean.pesquisar(f, me(), isGestor(), PAGINA_MAX);
    }

    // ------------------------------------------------------------ criação / edição

    @POST
    @Path("/create")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response create(MultipartFormDataInput input)
            throws IOException, MyEntityNotFoundException {

        Map<String, List<InputPart>> map = input.getFormDataMap();

        NovaPublicacaoDTO dados = new NovaPublicacaoDTO();
        dados.setTitulo(campo(map, "titulo"));
        dados.setTipo(campo(map, "tipo"));
        dados.setAreaCientifica(campo(map, "areaCientifica"));
        dados.setAutor(campo(map, "autor"));
        try {
            dados.setAno(Integer.parseInt(campo(map, "ano").trim()));
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("O campo ano tem de ser um número.");
        }

        List<InputPart> ficheiros = map.get("file");
        if (ficheiros == null || ficheiros.isEmpty()) {
            throw new BusinessRuleException("Campo obrigatório em falta: file");
        }
        InputPart filePart = ficheiros.get(0);
        dados.setFilename(filePart.getFileName());
        try (InputStream stream = filePart.getBody(InputStream.class, null)) {
            dados.setConteudo(lerComLimite(stream, AppConfig.maxUploadMb() * 1024L * 1024L));
        }

        List<String> tags = new ArrayList<>();
        List<InputPart> tagParts = map.get("tags");
        if (tagParts != null) {
            for (InputPart part : tagParts) {
                tags.add(part.getBodyAsString());
            }
        }
        dados.setTags(tags);

        User user = userBean.findByUsername(me());
        Publicacao publicacao = publicacaoBean.criar(dados, user, isGestor());

        return Response.status(Response.Status.CREATED)
                .entity(PublicacaoDTO.of(publicacaoBean.find(publicacao.getId()), isGestor()))
                .build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response updatePublicacao(@PathParam("id") long id, PublicacaoUpdateDTO dto)
            throws MyEntityNotFoundException {
        Publicacao p = publicacaoBean.findAcessivel(id, me(), isGestor());
        exigirDonoOuGestor(p, "Só quem submeteu (ou um responsável) pode editar esta publicação.");

        publicacaoBean.update(id, dto, userBean.findByUsername(me()));
        return Response.ok(PublicacaoDetailDTO.detalhe(publicacaoBean.find(id), isGestor())).build();
    }

    /** Pede nova geração do resumo à IA (dono ou gestor). */
    @POST
    @Path("{id}/resumo/regenerar")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response regenerarResumo(@PathParam("id") long id) throws MyEntityNotFoundException {
        Publicacao p = publicacaoBean.findAcessivel(id, me(), isGestor());
        exigirDonoOuGestor(p, "Só quem submeteu (ou um responsável) pode pedir um novo resumo.");

        publicacaoBean.pedirNovoResumo(id, userBean.findByUsername(me()));
        return Response.accepted(PublicacaoDTO.of(publicacaoBean.find(id), isGestor())).build();
    }

    // ------------------------------------------------------------ detalhe / download

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response getPublicacao(@PathParam("id") long id) throws MyEntityNotFoundException {
        Publicacao p = publicacaoBean.findAcessivel(id, me(), isGestor());
        return Response.ok(PublicacaoDetailDTO.detalhe(p, isGestor())).build();
    }

    @GET
    @Path("{id}/download")
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response download(@PathParam("id") long id) throws MyEntityNotFoundException, IOException {
        Publicacao p = publicacaoBean.findAcessivel(id, me(), isGestor());
        java.nio.file.Path ficheiro = publicacaoBean.caminhoFicheiro(p);

        StreamingOutput corpo = saida -> Files.copy(ficheiro, saida);
        return Response.ok(corpo, p.getMimeType() != null ? p.getMimeType() : MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Length", Files.size(ficheiro))
                .header("Content-Disposition", contentDisposition(p.getFilename()))
                .build();
    }

    // ------------------------------------------------------------ visibilidade (responsável/admin)

    @PUT
    @Path("{id}/ocultar")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public Response ocultar(@PathParam("id") long id) throws MyEntityNotFoundException {
        publicacaoBean.ocultar(id, userBean.findByUsername(me()));
        return Response.ok(PublicacaoDTO.of(publicacaoBean.find(id), true)).build();
    }

    @PUT
    @Path("{id}/visivel")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public Response visivel(@PathParam("id") long id) throws MyEntityNotFoundException {
        publicacaoBean.mostrar(id, userBean.findByUsername(me()));
        return Response.ok(PublicacaoDTO.of(publicacaoBean.find(id), true)).build();
    }

    // ------------------------------------------------------------ tags

    @POST
    @Path("/{id}/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response addTag(@PathParam("id") long id, String nomeTag)
            throws MyEntityNotFoundException, MyEntityExistsException {
        publicacaoBean.findAcessivel(id, me(), isGestor());
        publicacaoBean.addTag(id, limparTag(nomeTag), userBean.findByUsername(me()), isGestor());
        return Response.status(Response.Status.CREATED)
                .entity(PublicacaoDTO.of(publicacaoBean.find(id), isGestor())).build();
    }

    /** Desassociar tag: só Responsável/Admin. */
    @DELETE
    @Path("/{id}/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "RESPONSAVEL", "ADMINISTRADOR" })
    public Response removeTag(@PathParam("id") long id, String nomeTag) throws MyEntityNotFoundException {
        publicacaoBean.removeTag(id, limparTag(nomeTag), userBean.findByUsername(me()));
        return Response.status(Response.Status.ACCEPTED)
                .entity(PublicacaoDTO.of(publicacaoBean.find(id), true)).build();
    }

    // ------------------------------------------------------------ ratings

    @POST
    @Path("{id}/rating")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response addRating(@PathParam("id") long id, RatingDTO ratingDTO) throws MyEntityNotFoundException {
        publicacaoBean.findAcessivel(id, me(), isGestor());
        publicacaoBean.addOrUpdateRating(id, userBean.findByUsername(me()), ratingDTO.getRating());
        return Response.status(Response.Status.CREATED)
                .entity(PublicacaoDTO.of(publicacaoBean.find(id), isGestor())).build();
    }

    @PUT
    @Path("{id}/rating")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @RolesAllowed({ "COLABORADOR", "RESPONSAVEL", "ADMINISTRADOR" })
    public Response updateRating(@PathParam("id") long id, RatingDTO ratingDTO) throws MyEntityNotFoundException {
        publicacaoBean.findAcessivel(id, me(), isGestor());
        if (!publicacaoBean.temRating(id, me())) {
            throw new BusinessRuleException("Não tem rating nesta publicação para atualizar.");
        }
        publicacaoBean.addOrUpdateRating(id, userBean.findByUsername(me()), ratingDTO.getRating());
        return Response.ok(PublicacaoDTO.of(publicacaoBean.find(id), isGestor())).build();
    }

    // ------------------------------------------------------------ auxiliares

    private String me() {
        return securityContext.getUserPrincipal().getName();
    }

    private boolean isGestor() {
        return securityContext.isUserInRole("RESPONSAVEL") || securityContext.isUserInRole("ADMINISTRADOR");
    }

    private void exigirDonoOuGestor(Publicacao p, String mensagem) {
        boolean dono = p.getSubmetidoPor() != null && p.getSubmetidoPor().getUsername().equals(me());
        if (!dono && !isGestor()) {
            throw new ForbiddenException(mensagem);
        }
    }

    private static PublicacaoFiltro filtroLista() {
        PublicacaoFiltro f = new PublicacaoFiltro();
        f.size = LISTA_MAX;
        return f;
    }

    private static String campo(Map<String, List<InputPart>> map, String nome) throws IOException {
        List<InputPart> partes = map.get(nome);
        if (partes == null || partes.isEmpty()) {
            throw new BusinessRuleException("Campo obrigatório em falta: " + nome);
        }
        return partes.get(0).getBodyAsString();
    }

    private static LocalDate data(String valor, String nome) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("O parâmetro " + nome + " deve estar no formato AAAA-MM-DD.");
        }
    }

    /** O frontend pode enviar o nome da tag com ou sem aspas JSON. */
    static String limparTag(String bruto) {
        if (bruto == null) {
            return "";
        }
        String t = bruto.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t.trim();
    }

    private static byte[] lerComLimite(InputStream in, long maximoBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int lidos;
        while ((lidos = in.read(buffer)) != -1) {
            total += lidos;
            if (total > maximoBytes) {
                throw new BusinessRuleException("O ficheiro excede o tamanho máximo de "
                        + (maximoBytes / (1024 * 1024)) + " MB.");
            }
            out.write(buffer, 0, lidos);
        }
        return out.toByteArray();
    }

    static String contentDisposition(String nome) {
        String seguro = FicheiroValidator.sanitizarNome(nome);
        String ascii = seguro.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8 = URLEncoder.encode(seguro, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }
}
