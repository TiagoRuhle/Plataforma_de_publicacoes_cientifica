package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;

import java.util.List;
import java.util.regex.Pattern;

@Stateless
public class TagBean {

    /** Sem "/" nem "\" porque o nome vai no path (DELETE /tags/{tag}). */
    private static final Pattern NOME_VALIDO = Pattern.compile("^[^/\\\\]{1,100}$");

    @PersistenceContext
    private EntityManager entityManager;

    public Tag findByTag(String tag) {
        try {
            TypedQuery<Tag> query = entityManager.createNamedQuery("getTagByNome", Tag.class);
            query.setParameter("nome", tag);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Tag> findAll() {
        return entityManager.createNamedQuery("getAllTags", Tag.class).getResultList();
    }

    public List<Tag> findVisible() {
        return entityManager.createNamedQuery("getVisibleTags", Tag.class).getResultList();
    }

    public void create(String nome, boolean visivel) throws MyEntityExistsException {
        String limpo = nome == null ? "" : nome.trim();
        if (!NOME_VALIDO.matcher(limpo).matches()) {
            throw new BusinessRuleException("O nome da tag deve ter 1 a 100 caracteres e não pode conter / ou \\.");
        }
        if (findByTag(limpo) != null) {
            throw new MyEntityExistsException("A tag já existe: " + limpo);
        }
        entityManager.persist(new Tag(limpo, visivel));
    }

    public void delete(String nome) throws MyEntityNotFoundException {
        Tag tag = findByTag(nome);
        if (tag == null) {
            throw new MyEntityNotFoundException("Tag não encontrada: " + nome);
        }
        entityManager.createQuery("DELETE FROM Subscricao s WHERE s.tag = :t")
                .setParameter("t", tag).executeUpdate();
        for (Publicacao p : entityManager.createQuery(
                        "SELECT p FROM Publicacao p JOIN p.tags t WHERE t = :t", Publicacao.class)
                .setParameter("t", tag).getResultList()) {
            p.removeTag(tag);
        }
        entityManager.remove(tag);
    }
}
