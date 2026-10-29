package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Comentario;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.events.NotificacaoEvent;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;

@Stateless
public class ComentarioBean {

    private static final int MAX_TEXTO = 2000;

    @EJB
    private HistoricoBean historicoBean;

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private Event<NotificacaoEvent> notificacaoEvent;

    public Comentario create(long publicacaoId, User autor, String texto) throws MyEntityNotFoundException {
        return create(publicacaoId, autor, texto, true);
    }

    public Comentario create(long publicacaoId, User autor, String texto, boolean notificar)
            throws MyEntityNotFoundException {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.isEmpty()) {
            throw new BusinessRuleException("O texto do comentário não pode estar vazio.");
        }
        if (limpo.length() > MAX_TEXTO) {
            throw new BusinessRuleException("O comentário não pode ter mais de " + MAX_TEXTO + " caracteres.");
        }
        Publicacao publicacao = entityManager.find(Publicacao.class, publicacaoId);
        if (publicacao == null) {
            throw new MyEntityNotFoundException("Publicação não encontrada: " + publicacaoId);
        }
        User autorGerido = entityManager.find(User.class, autor.getId());

        Comentario comentario = new Comentario(limpo, autorGerido, publicacao);
        entityManager.persist(comentario);

        historicoBean.registar(TipoAcao.COMENTARIO_ADICIONADO, autorGerido, publicacao,
                "Comentou: " + abreviar(limpo, 150));

        if (notificar && publicacao.isVisivel()) {
            notificacaoEvent.fire(new NotificacaoEvent(publicacao.getId(),
                    autorGerido.getUsername() + " comentou: " + abreviar(limpo, 300),
                    autorGerido.getUsername()));
        }
        return comentario;
    }

    public Comentario find(long id) {
        return entityManager.find(Comentario.class, id);
    }

    public void delete(long comentarioId, User actor) throws MyEntityNotFoundException {
        Comentario comentario = encontrar(comentarioId);
        Publicacao publicacao = comentario.getPublicacao();
        String texto = comentario.getTexto();
        entityManager.remove(comentario);
        historicoBean.registar(TipoAcao.COMENTARIO_REMOVIDO, actor, publicacao,
                "Apagou comentário: " + abreviar(texto, 150));
    }

    /** Ocultar comentário (Responsável/Admin). */
    public void ocultar(long comentarioId, User actor) throws MyEntityNotFoundException {
        Comentario comentario = encontrar(comentarioId);
        comentario.setVisivel(false);
        historicoBean.registar(TipoAcao.COMENTARIO_EDITADO, actor, comentario.getPublicacao(),
                "Ocultou comentário: " + abreviar(comentario.getTexto(), 150));
    }

    public void mostrar(long comentarioId, User actor) throws MyEntityNotFoundException {
        Comentario comentario = encontrar(comentarioId);
        comentario.setVisivel(true);
        historicoBean.registar(TipoAcao.COMENTARIO_EDITADO, actor, comentario.getPublicacao(),
                "Tornou visível o comentário: " + abreviar(comentario.getTexto(), 150));
    }

    private Comentario encontrar(long id) throws MyEntityNotFoundException {
        Comentario c = find(id);
        if (c == null) {
            throw new MyEntityNotFoundException("Comentário não encontrado: " + id);
        }
        return c;
    }

    private static String abreviar(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
