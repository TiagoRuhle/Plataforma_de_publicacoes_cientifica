package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.NovaPublicacaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.PublicacaoUpdateDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Rating;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.ResumoEstado;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.events.NotificacaoEvent;
import pt.ipleiria.estg.dei.ei.dae.projeto.events.ResumoPedidoEvent;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.utils.FicheiroValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PublicacaoBean {

    private static final Logger logger = Logger.getLogger(PublicacaoBean.class.getName());

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private HistoricoBean historicoBean;

    @Inject
    private Event<ResumoPedidoEvent> resumoEvent;

    @Inject
    private Event<NotificacaoEvent> notificacaoEvent;

    // ------------------------------------------------------------------ leitura

    /** Carrega a publicação com tags, ratings e comentários (para o detalhe). */
    public Publicacao find(long id) {
        Publicacao publicacao = entityManager.find(Publicacao.class, id);
        if (publicacao == null) {
            return null;
        }
        Hibernate.initialize(publicacao.getTags());
        Hibernate.initialize(publicacao.getRatings());
        Hibernate.initialize(publicacao.getComentarios());
        return publicacao;
    }

    /**
     * Ponto único da regra de visibilidade: publicações ocultas só são acessíveis ao dono e a
     * gestores. Para os outros comportam-se como inexistentes (404) e não revelam que existem.
     */
    public Publicacao findAcessivel(long id, String username, boolean gestor) throws MyEntityNotFoundException {
        Publicacao p = find(id);
        boolean dono = p != null && p.getSubmetidoPor() != null
                && p.getSubmetidoPor().getUsername().equals(username);
        if (p == null || (!p.isVisivel() && !gestor && !dono)) {
            throw new MyEntityNotFoundException("Publicação não encontrada: " + id);
        }
        return p;
    }

    public boolean temRating(long publicacaoId, String username) {
        Long total = entityManager.createQuery(
                        "SELECT COUNT(r) FROM Rating r WHERE r.publicacao.id = :id AND r.autor.username = :u",
                        Long.class)
                .setParameter("id", publicacaoId)
                .setParameter("u", username)
                .getSingleResult();
        return total > 0;
    }

    /** Caminho do ficheiro no disco, garantindo que está dentro da pasta de uploads. */
    public Path caminhoFicheiro(Publicacao p) throws MyEntityNotFoundException {
        if (p.getFilepath() == null) {
            throw new MyEntityNotFoundException("Publicação sem ficheiro: " + p.getId());
        }
        Path base = Paths.get(AppConfig.uploadDir()).toAbsolutePath().normalize();
        Path ficheiro = Paths.get(p.getFilepath()).toAbsolutePath().normalize();
        if (!ficheiro.startsWith(base) || !Files.isRegularFile(ficheiro)) {
            throw new MyEntityNotFoundException("Ficheiro não encontrado para a publicação " + p.getId());
        }
        return ficheiro;
    }

    // ------------------------------------------------------------------ criação

    public Publicacao criar(NovaPublicacaoDTO dados, User user, boolean gestor)
            throws MyEntityNotFoundException, IOException {
        return criar(dados, user, gestor, null, true);
    }

    /**
     * Cria a publicação, guarda o ficheiro e associa as tags numa única transação.
     * Se algo falhar depois de o ficheiro estar no disco, o ficheiro é apagado.
     *
     * @param resumoFixo se não for null, usa-se este resumo e não se chama a IA (seed/testes)
     */
    public Publicacao criar(NovaPublicacaoDTO dados, User user, boolean gestor, String resumoFixo, boolean notificar)
            throws MyEntityNotFoundException, IOException {

        String titulo = exigir(dados.getTitulo(), "titulo", 255);
        String tipo = exigir(dados.getTipo(), "tipo", 100);
        String area = exigir(dados.getAreaCientifica(), "areaCientifica", 100);
        String autor = exigir(dados.getAutor(), "autor", 255);
        validarAno(dados.getAno());

        FicheiroValidator.Tipo tipoFicheiro;
        try {
            tipoFicheiro = FicheiroValidator.validar(dados.getFilename(), dados.getConteudo());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        }

        // valida as tags ANTES de escrever no disco
        LinkedHashSet<Tag> tags = new LinkedHashSet<>();
        for (String nome : dados.getTags()) {
            String limpo = nome == null ? "" : nome.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            Tag tag = entityManager.find(Tag.class, limpo);
            if (tag == null || (!tag.isVisivel() && !gestor)) {
                throw new MyEntityNotFoundException("Tag não encontrada: " + limpo);
            }
            tags.add(tag);
        }

        Path pasta = Paths.get(AppConfig.uploadDir(), "u" + user.getId());
        Files.createDirectories(pasta);
        String extensao = tipoFicheiro == FicheiroValidator.Tipo.PDF ? ".pdf" : ".zip";
        Path destino = pasta.resolve("file_" + UUID.randomUUID() + extensao);
        Files.write(destino, dados.getConteudo(), StandardOpenOption.CREATE_NEW);

        try {
            Publicacao p = new Publicacao(titulo, tipo, area, destino.toString(),
                    FicheiroValidator.sanitizarNome(dados.getFilename()), tipoFicheiro.mime(),
                    autor, dados.getAno(), entityManager.find(User.class, user.getId()));
            tags.forEach(p::addTag);

            boolean ia = resumoFixo == null && tipoFicheiro == FicheiroValidator.Tipo.PDF;
            if (resumoFixo != null) {
                p.setResumo(resumoFixo);
                p.setResumoEstado(ResumoEstado.EDITADO); // texto escrito por pessoas, não pela IA
            } else if (ia) {
                p.setResumo("Resumo em processamento...");
                p.setResumoEstado(ResumoEstado.PENDENTE);
            } else {
                p.setResumo("O resumo automático não está disponível para ficheiros ZIP.");
                p.setResumoEstado(ResumoEstado.NAO_APLICAVEL);
            }

            entityManager.persist(p);
            entityManager.flush();

            historicoBean.registar(TipoAcao.PUBLICACAO_CRIADA, user, p, "Publicação criada: " + p.getTitulo());
            for (Tag t : tags) {
                historicoBean.registar(TipoAcao.TAG_ADICIONADA, user, p, "Tag " + t.getTag());
            }

            if (ia) {
                resumoEvent.fire(new ResumoPedidoEvent(p.getId())); // só corre após o commit
            }
            if (notificar && !tags.isEmpty()) {
                notificacaoEvent.fire(new NotificacaoEvent(p.getId(),
                        "Nova publicação nas tags que segues: " + p.getTitulo(), user.getUsername()));
            }
            return p;
        } catch (RuntimeException e) {
            apagarSilenciosamente(destino);
            throw e;
        }
    }

    // ------------------------------------------------------------------ edição

    public void update(long id, PublicacaoUpdateDTO dto, User editor) throws MyEntityNotFoundException {
        Publicacao publicacao = find(id);
        if (publicacao == null) {
            throw new MyEntityNotFoundException("Publicação não encontrada: " + id);
        }

        // Controlo de concorrência: se o cliente leu a versão N e entretanto outra pessoa gravou (N+1), recusa.
        // O @Version do Hibernate cobre gravações simultâneas; esta verificação cobre o tempo em que a pessoa
        // esteve com o formulário aberto (muito mais longo do que uma transação).
        if (dto.getVersion() != null && dto.getVersion() != publicacao.getVersion()) {
            throw new ConflictException("A publicação foi alterada por outra pessoa enquanto a editava. "
                    + "Reveja a versão atual e volte a guardar.");
        }

        if (preenchido(dto.getTitulo())) {
            publicacao.setTitulo(exigir(dto.getTitulo(), "titulo", 255));
        }
        if (preenchido(dto.getTipo())) {
            publicacao.setTipo(exigir(dto.getTipo(), "tipo", 100));
        }
        if (preenchido(dto.getAreaCientifica())) {
            publicacao.setAreaCientifica(exigir(dto.getAreaCientifica(), "areaCientifica", 100));
        }
        if (preenchido(dto.getAutor())) {
            publicacao.setAutor(exigir(dto.getAutor(), "autor", 255));
        }
        if (dto.getAno() != null) {
            validarAno(dto.getAno());
            publicacao.setAno(dto.getAno());
        }

        boolean resumoAlterado = preenchido(dto.getResumo());
        if (resumoAlterado) {
            String resumo = dto.getResumo().trim();
            if (resumo.length() > 19000) {
                throw new BusinessRuleException("O resumo não pode ter mais de 19000 caracteres.");
            }
            publicacao.setResumo(resumo);
            publicacao.setResumoEstado(ResumoEstado.EDITADO);
        }

        historicoBean.registar(
                resumoAlterado ? TipoAcao.RESUMO_EDITADO_MANUALMENTE : TipoAcao.PUBLICACAO_ATUALIZADA,
                editor, publicacao, resumoAlterado ? "Resumo corrigido manualmente" : "Publicação editada");

        if (publicacao.isVisivel()) {
            notificacaoEvent.fire(new NotificacaoEvent(publicacao.getId(),
                    resumoAlterado ? "O resumo da publicação foi corrigido por " + editor.getUsername()
                            : "A publicação foi atualizada por " + editor.getUsername(),
                    editor.getUsername()));
        }
    }

    /** Volta a pedir o resumo à IA (útil quando falhou ou quando o utilizador quer outro). */
    public void pedirNovoResumo(long id, User actor) throws MyEntityNotFoundException {
        Publicacao p = find(id);
        if (p == null) {
            throw new MyEntityNotFoundException("Publicação não encontrada: " + id);
        }
        if (!"application/pdf".equals(p.getMimeType())) {
            throw new BusinessRuleException("O resumo automático só está disponível para ficheiros PDF.");
        }
        p.setResumoEstado(ResumoEstado.PENDENTE);
        p.setResumo("Resumo em processamento...");
        historicoBean.registar(TipoAcao.PUBLICACAO_ATUALIZADA, actor, p, "Pediu novo resumo à IA");
        resumoEvent.fire(new ResumoPedidoEvent(p.getId()));
    }

    // ------------------------------------------------------------------ visibilidade

    public void ocultar(long id, User actor) throws MyEntityNotFoundException {
        Publicacao publicacao = encontrar(id);
        publicacao.setVisivel(false);
        historicoBean.registar(TipoAcao.PUBLICACAO_OCULTADA, actor, publicacao,
                "Ocultou a publicação " + publicacao.getTitulo());
    }

    public void mostrar(long id, User actor) throws MyEntityNotFoundException {
        Publicacao publicacao = encontrar(id);
        publicacao.setVisivel(true);
        historicoBean.registar(TipoAcao.PUBLICACAO_VISIVEL, actor, publicacao,
                "Tornou visível a publicação " + publicacao.getTitulo());
    }

    // ------------------------------------------------------------------ tags

    public void addTag(long publicacaoId, String nomeTag, User user, boolean gestor)
            throws MyEntityNotFoundException, MyEntityExistsException {
        addTag(publicacaoId, nomeTag, user, gestor, true);
    }

    public void addTag(long publicacaoId, String nomeTag, User user, boolean gestor, boolean notificar)
            throws MyEntityNotFoundException, MyEntityExistsException {
        Publicacao publicacao = encontrar(publicacaoId);
        Tag tag = entityManager.find(Tag.class, nomeTag);
        // tag oculta comporta-se como inexistente para quem não é gestor
        if (tag == null || (!tag.isVisivel() && !gestor)) {
            throw new MyEntityNotFoundException("Tag não encontrada: " + nomeTag);
        }
        if (publicacao.getTags().contains(tag)) {
            throw new MyEntityExistsException("A publicação já tem a tag " + nomeTag);
        }
        publicacao.addTag(tag);
        historicoBean.registar(TipoAcao.TAG_ADICIONADA, user, publicacao, "Tag " + nomeTag);

        if (notificar && publicacao.isVisivel()) {
            notificacaoEvent.fire(new NotificacaoEvent(publicacao.getId(),
                    "A tag \"" + nomeTag + "\" foi associada a uma publicação por " + user.getUsername(),
                    user.getUsername()));
        }
    }

    public void removeTag(long publicacaoId, String nomeTag, User user) throws MyEntityNotFoundException {
        Publicacao publicacao = encontrar(publicacaoId);
        Tag tag = entityManager.find(Tag.class, nomeTag);
        if (tag == null || !publicacao.getTags().contains(tag)) {
            throw new MyEntityNotFoundException("A publicação não tem a tag " + nomeTag);
        }
        publicacao.removeTag(tag);
        historicoBean.registar(TipoAcao.TAG_REMOVIDA, user, publicacao, "Tag " + nomeTag);
    }

    // ------------------------------------------------------------------ ratings

    public void addOrUpdateRating(long publicacaoId, User autor, int valor) throws MyEntityNotFoundException {
        if (valor < 1 || valor > 5) {
            throw new BusinessRuleException("O rating tem de estar entre 1 e 5.");
        }
        Publicacao publicacao = encontrar(publicacaoId);

        // compara por username: o User recebido vem de outro contexto de persistência
        Rating existente = publicacao.getRatings().stream()
                .filter(r -> r.getAutor().getUsername().equals(autor.getUsername()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setValor(valor);
            historicoBean.registar(TipoAcao.RATING_ATUALIZADO, autor, publicacao,
                    "Rating atualizado para " + valor);
        } else {
            User autorGerido = entityManager.find(User.class, autor.getId());
            Rating novo = new Rating(valor, autorGerido, publicacao);
            entityManager.persist(novo);
            publicacao.getRatings().add(novo);
            historicoBean.registar(TipoAcao.RATING_ADICIONADO, autor, publicacao, "Rating " + valor);
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private Publicacao encontrar(long id) throws MyEntityNotFoundException {
        Publicacao p = find(id);
        if (p == null) {
            throw new MyEntityNotFoundException("Publicação não encontrada: " + id);
        }
        return p;
    }

    private static boolean preenchido(String s) {
        return s != null && !s.isBlank();
    }

    private static String exigir(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessRuleException("Campo obrigatório em falta: " + campo);
        }
        String limpo = valor.trim();
        if (limpo.length() > max) {
            throw new BusinessRuleException("O campo " + campo + " não pode ter mais de " + max + " caracteres.");
        }
        return limpo;
    }

    private static void validarAno(int ano) {
        int maximo = Year.now().getValue() + 1;
        if (ano < 1000 || ano > maximo) {
            throw new BusinessRuleException("O ano tem de estar entre 1000 e " + maximo + ".");
        }
    }

    private static void apagarSilenciosamente(Path ficheiro) {
        try {
            Files.deleteIfExists(ficheiro);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Não foi possível apagar o ficheiro órfão " + ficheiro, e);
        }
    }
}
