package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.SubscricaoDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Subscricao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;

import java.util.List;

@Stateless
public class SubscricaoBean {

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private HistoricoBean historicoBean;

    @EJB
    private UserBean userBean;

    /** Tags invisíveis comportam-se como inexistentes para quem não é gestor (404). */
    public void subscrever(String username, String nomeTag, boolean gestor)
            throws MyEntityNotFoundException, MyEntityExistsException {
        User user = userBean.findByUsername(username);
        if (user == null) {
            throw new MyEntityNotFoundException("Utilizador não encontrado: " + username);
        }
        Tag tag = entityManager.find(Tag.class, nomeTag);
        if (tag == null || (!tag.isVisivel() && !gestor)) {
            throw new MyEntityNotFoundException("Tag não encontrada: " + nomeTag);
        }
        if (findByUsernameAndTag(username, tag.getTag()) != null) {
            throw new MyEntityExistsException("Já está subscrito à tag " + tag.getTag());
        }
        entityManager.persist(new Subscricao(user, tag));
        historicoBean.registar(TipoAcao.TAG_SUBSCRITA, user, "Tag " + tag.getTag() + " subscrita");
    }

    public void dessubscrever(String username, String nomeTag) throws MyEntityNotFoundException {
        User user = userBean.findByUsername(username);
        Subscricao subscricao = findByUsernameAndTag(username, nomeTag);
        if (user == null || subscricao == null) {
            throw new MyEntityNotFoundException("Não existe subscrição à tag " + nomeTag);
        }
        entityManager.remove(subscricao);
        historicoBean.registar(TipoAcao.TAG_DESSUBSCRITA, user, "Tag " + nomeTag + " dessubscrita");
    }

    public Subscricao findByUsernameAndTag(String username, String tag) {
        try {
            TypedQuery<Subscricao> query = entityManager.createNamedQuery("getSubscricaoByUserAndTag", Subscricao.class);
            query.setParameter("username", username);
            query.setParameter("tag", tag);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<SubscricaoDTO> findAllByUser(String username) {
        TypedQuery<Subscricao> query = entityManager.createNamedQuery("getAllSubscricoesByUser", Subscricao.class);
        query.setParameter("username", username);
        return SubscricaoDTO.from(query.getResultList());
    }
}
