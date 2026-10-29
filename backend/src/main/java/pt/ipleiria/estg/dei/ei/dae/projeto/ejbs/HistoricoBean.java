package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class HistoricoBean {

    private static final Logger logger = Logger.getLogger(HistoricoBean.class.getName());

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Regista uma ação no histórico
     */
    public Historico registar(TipoAcao tipoAcao, User user, Publicacao publicacao, String descricao) {
        Historico historico = new Historico(tipoAcao, user, publicacao, descricao);
        entityManager.persist(historico);
        logger.info(String.format("Histórico registado: %s - User: %s", tipoAcao, user.getUsername()));
        return historico;
    }

    /**
     * Regista uma ação no histórico com detalhes adicionais
     */
    public Historico registar(TipoAcao tipoAcao, User user, Publicacao publicacao, String descricao, String detalhes) {
        Historico historico = new Historico(tipoAcao, user, publicacao, descricao, detalhes);
        entityManager.persist(historico);
        logger.info(String.format("Histórico registado: %s - User: %s", tipoAcao, user.getUsername()));
        return historico;
    }

    /**
     * Regista ação sem publicação associada
     */
    public Historico registar(TipoAcao tipoAcao, User user, String descricao) {
        return registar(tipoAcao, user, null, descricao);
    }

    /**
     * Busca todo o histórico
     */
    public List<Historico> findAll() {
        var list = entityManager
                .createNamedQuery("getAllHistorico", Historico.class)
                .getResultList();

        for (var h : list) {
            Hibernate.initialize(h.getUser());
            if (h.getPublicacao() != null) {
                Hibernate.initialize(h.getPublicacao());
            }
        }

        return list;
    }

    /**
     * Busca histórico de um utilizador específico
     */
    public List<Historico> findByUser(User user) {
        var list = entityManager
                .createNamedQuery("getHistoricoByUser", Historico.class)
                .setParameter("user", user)
                .getResultList();

        for (var h : list) {
            Hibernate.initialize(h.getUser());
            if (h.getPublicacao() != null) {
                Hibernate.initialize(h.getPublicacao());
            }
        }

        return list;
    }

    /**
     * Busca histórico de uma publicação específica
     */
    public List<Historico> findByPublicacao(Publicacao publicacao) {
        var list = entityManager
                .createNamedQuery("getHistoricoByPublicacao", Historico.class)
                .setParameter("publicacao", publicacao)
                .getResultList();

        for (var h : list) {
            Hibernate.initialize(h.getUser());
            Hibernate.initialize(h.getPublicacao());
        }

        return list;
    }

    /**
     * Busca histórico por tipo de ação
     */
    public List<Historico> findByTipoAcao(TipoAcao tipoAcao) {
        var list = entityManager
                .createNamedQuery("getHistoricoByTipoAcao", Historico.class)
                .setParameter("tipoAcao", tipoAcao)
                .getResultList();

        for (var h : list) {
            Hibernate.initialize(h.getUser());
            if (h.getPublicacao() != null) {
                Hibernate.initialize(h.getPublicacao());
            }
        }

        return list;
    }

    /**
     * Busca histórico por utilizador e tipo de ação
     */
    public List<Historico> findByUserAndTipoAcao(User user, TipoAcao tipoAcao) {
        return entityManager
                .createQuery("SELECT h FROM Historico h WHERE h.user = :user AND h.tipoAcao = :tipoAcao ORDER BY h.dataHora DESC", Historico.class)
                .setParameter("user", user)
                .setParameter("tipoAcao", tipoAcao)
                .getResultList();
    }

    /**
     * Busca histórico entre datas
     */
    public List<Historico> findByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return entityManager
                .createQuery("SELECT h FROM Historico h WHERE h.dataHora BETWEEN :inicio AND :fim ORDER BY h.dataHora DESC", Historico.class)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();
    }

    /**
     * Busca histórico de um utilizador entre datas
     */
    public List<Historico> findByUserAndPeriodo(User user, LocalDateTime inicio, LocalDateTime fim) {
        return entityManager
                .createQuery("SELECT h FROM Historico h WHERE h.user = :user AND h.dataHora BETWEEN :inicio AND :fim ORDER BY h.dataHora DESC", Historico.class)
                .setParameter("user", user)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();
    }

    /**
     * Limpa histórico antigo (opcional - para manutenção)
     */
    public int limparHistoricoAntigo(int diasAntigos) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasAntigos);

        int deleted = entityManager
                .createQuery("DELETE FROM Historico h WHERE h.dataHora < :dataLimite")
                .setParameter("dataLimite", dataLimite)
                .executeUpdate();

        logger.info(String.format("Removidos %d registos de histórico com mais de %d dias", deleted, diasAntigos));
        return deleted;
    }

    /**
     * Conta ações de um utilizador
     */
    public long contarAcoesUser(User user) {
        return entityManager
                .createQuery("SELECT COUNT(h) FROM Historico h WHERE h.user = :user", Long.class)
                .setParameter("user", user)
                .getSingleResult();
    }

    /**
     * Conta ações numa publicação
     */
    public long contarAcoesPublicacao(Publicacao publicacao) {
        return entityManager
                .createQuery("SELECT COUNT(h) FROM Historico h WHERE h.publicacao = :publicacao", Long.class)
                .setParameter("publicacao", publicacao)
                .getSingleResult();
    }
}