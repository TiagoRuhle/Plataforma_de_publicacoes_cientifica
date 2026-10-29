package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Subscricao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Envia e-mails aos subscritores das tags de uma publicação. Nunca propaga erros. */
@Stateless
public class NotificacaoAsyncBean {

    private static final Logger logger = Logger.getLogger(NotificacaoAsyncBean.class.getName());

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private EmailBean emailBean;

    @Asynchronous
    public void notificar(long publicacaoId, String descricao, String actor) {
        try {
            Publicacao publicacao = entityManager.find(Publicacao.class, publicacaoId);
            if (publicacao == null || !publicacao.isVisivel()) {
                return; // publicações ocultas não geram alertas
            }

            List<Subscricao> subscricoes = entityManager.createQuery(
                            "SELECT s FROM Subscricao s JOIN FETCH s.user u JOIN FETCH s.tag tg "
                                    + "WHERE u.active = true AND u.username <> :actor "
                                    + "AND tg IN (SELECT t FROM Publicacao p JOIN p.tags t WHERE p.id = :id)",
                            Subscricao.class)
                    .setParameter("actor", actor == null ? "" : actor)
                    .setParameter("id", publicacaoId)
                    .getResultList();

            // um e-mail por utilizador, mesmo que subscreva várias tags da publicação
            Map<String, List<String>> tagsPorUser = new LinkedHashMap<>();
            Map<String, User> users = new LinkedHashMap<>();
            for (Subscricao s : subscricoes) {
                String username = s.getUser().getUsername();
                users.putIfAbsent(username, s.getUser());
                tagsPorUser.computeIfAbsent(username, k -> new ArrayList<>()).add(s.getTag().getTag());
            }

            for (Map.Entry<String, List<String>> e : tagsPorUser.entrySet()) {
                User destinatario = users.get(e.getKey());
                enviar(destinatario, e.getValue(), publicacao, descricao);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Falha ao notificar subscritores da publicação " + publicacaoId, e);
        }
    }

    private void enviar(User destinatario, List<String> tags, Publicacao publicacao, String descricao) {
        String assunto = tags.size() == 1
                ? "Há novidades na tag " + tags.get(0)
                : "Há novidades nas tags " + String.join(", ", tags);
        String corpo = "Olá " + destinatario.getName() + ",\n\n"
                + descricao + "\n\n"
                + "Publicação: " + publicacao.getTitulo() + "\n"
                + "Ver: " + AppConfig.appBaseUrl() + "/publicacoes/" + publicacao.getId() + "\n\n"
                + "Recebes este e-mail porque subscreveste: " + String.join(", ", tags) + ".\n"
                + "Podes cancelar a subscrição na plataforma.";
        try {
            emailBean.enviar(destinatario.getEmail(), assunto, corpo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Não foi possível enviar e-mail a " + destinatario.getEmail(), e);
        }
    }
}
