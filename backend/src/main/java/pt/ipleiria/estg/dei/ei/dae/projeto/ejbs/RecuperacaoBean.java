package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.PasswordResetToken;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Hasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Recuperação de palavra-passe por e-mail (token de uso único, validade curta, guardado em hash). */
@Stateless
public class RecuperacaoBean {

    private static final Logger logger = Logger.getLogger(RecuperacaoBean.class.getName());
    private static final int VALIDADE_MINUTOS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private UserBean userBean;

    @EJB
    private EmailBean emailBean;

    @EJB
    private HistoricoBean historicoBean;

    /** Nunca revela se o e-mail existe: devolve sempre normalmente. */
    public void pedir(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        User encontrado = userBean.findByEmail(email.trim());
        if (encontrado == null || !encontrado.isActive()) {
            return;
        }
        User user = entityManager.find(User.class, encontrado.getId());

        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.user = :u")
                .setParameter("u", user).executeUpdate();

        String token = gerarToken();
        entityManager.persist(new PasswordResetToken(user, sha256(token),
                LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS)));

        String link = AppConfig.appBaseUrl() + "/repor-password?token=" + token;
        String corpo = "Olá " + user.getName() + ",\n\n"
                + "Recebemos um pedido para repor a tua palavra-passe.\n"
                + "Usa o link abaixo nos próximos " + VALIDADE_MINUTOS + " minutos:\n\n" + link + "\n\n"
                + "Se não foste tu, ignora este e-mail.";
        try {
            emailBean.enviar(user.getEmail(), "Recuperação de palavra-passe", corpo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Falha ao enviar e-mail de recuperação", e);
        }
    }

    public void repor(String token, String novaPassword) {
        if (novaPassword == null || novaPassword.length() < 8) {
            throw new BusinessRuleException("A nova password tem de ter pelo menos 8 caracteres.");
        }
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Token inválido ou expirado.");
        }
        PasswordResetToken registo;
        try {
            registo = entityManager.createQuery(
                            "SELECT t FROM PasswordResetToken t JOIN FETCH t.user "
                                    + "WHERE t.tokenHash = :h AND t.expiraEm > :agora", PasswordResetToken.class)
                    .setParameter("h", sha256(token.trim()))
                    .setParameter("agora", LocalDateTime.now())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessRuleException("Token inválido ou expirado.");
        }
        User user = registo.getUser();
        user.setPassword(Hasher.hash(novaPassword));
        entityManager.remove(registo); // uso único
        historicoBean.registar(TipoAcao.SENHA_ALTERADA, user, "Password reposta por e-mail");
    }

    static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
