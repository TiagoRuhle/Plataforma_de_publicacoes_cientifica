package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.projeto.dtos.UserDTO;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.TipoAcao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.BusinessRuleException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityExistsException;
import pt.ipleiria.estg.dei.ei.dae.projeto.exception.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Hasher;
import pt.ipleiria.estg.dei.ei.dae.projeto.security.Roles;

import java.util.List;
import java.util.regex.Pattern;

@Stateless
public class UserBean {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private HistoricoBean historicoBean;

    public User find(Long id) {
        return entityManager.find(User.class, id);
    }

    public User findByUsername(String username) {
        try {
            TypedQuery<User> query = entityManager.createNamedQuery("getUserByUsername", User.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public User findByEmail(String email) {
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                    .setParameter("email", email.trim().toLowerCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<User> findAll() {
        return entityManager.createNamedQuery("getAllUsers", User.class).getResultList();
    }

    public boolean canLogin(String username, String password) {
        User user = findByUsername(username);
        return user != null && user.isActive() && Hasher.verify(password, user.getPassword());
    }

    public void create(String username, String password, String name, String email, String role)
            throws MyEntityExistsException {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new BusinessRuleException("O username deve ter 3 a 50 caracteres (letras, números, . _ -).");
        }
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("A password tem de ter pelo menos 8 caracteres.");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome é obrigatório.");
        }
        String emailNormalizado = validarEmail(email);
        if (!Roles.isValid(role)) {
            throw new BusinessRuleException("Role inválido. Valores aceites: COLABORADOR, RESPONSAVEL, ADMINISTRADOR.");
        }
        if (findByUsername(username) != null) {
            throw new MyEntityExistsException("O utilizador já existe: " + username);
        }
        if (findByEmail(emailNormalizado) != null) {
            throw new MyEntityExistsException("O e-mail já está em uso: " + emailNormalizado);
        }

        User user = new User(username, Hasher.hash(password), name.trim(), emailNormalizado, Roles.normalizar(role));
        entityManager.persist(user);
        historicoBean.registar(TipoAcao.USER_CRIADO, user, "User criado");
    }

    public void updateEmail(String username, String email)
            throws MyEntityNotFoundException, MyEntityExistsException {
        User user = findOrThrow(username);
        String emailNormalizado = validarEmail(email);
        garantirEmailLivre(emailNormalizado, user);

        entityManager.lock(user, LockModeType.OPTIMISTIC);
        user.setEmail(emailNormalizado);
        historicoBean.registar(TipoAcao.PERFIL_ATUALIZADO, user, "E-mail atualizado");
    }

    public void updatePassword(String username, String oldPassword, String newPassword)
            throws MyEntityNotFoundException {
        User user = findOrThrow(username);
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessRuleException("A nova password tem de ter pelo menos 8 caracteres.");
        }
        if (oldPassword == null || !Hasher.verify(oldPassword, user.getPassword())) {
            throw new BusinessRuleException("A password atual está incorreta.");
        }
        if (Hasher.verify(newPassword, user.getPassword())) {
            throw new BusinessRuleException("A nova password tem de ser diferente da atual.");
        }
        entityManager.lock(user, LockModeType.OPTIMISTIC);
        user.setPassword(Hasher.hash(newPassword));
        historicoBean.registar(TipoAcao.SENHA_ALTERADA, user, "Password alterada");
    }

    public void updateRole(String username, String role) throws MyEntityNotFoundException {
        User user = findOrThrow(username);
        if (!Roles.isValid(role)) {
            throw new BusinessRuleException("Role inválido. Valores aceites: COLABORADOR, RESPONSAVEL, ADMINISTRADOR.");
        }
        String novo = Roles.normalizar(role);
        if (!Roles.ADMINISTRADOR.equals(novo)) {
            garantirAdminRestante(user);
        }
        entityManager.lock(user, LockModeType.OPTIMISTIC);
        user.setRole(novo);
        historicoBean.registar(TipoAcao.PERFIL_ATUALIZADO, user, "Role alterado para " + novo);
    }

    public void updateStatus(String username, Boolean status) throws MyEntityNotFoundException {
        User user = findOrThrow(username);
        if (status == null) {
            throw new BusinessRuleException("O estado (active) é obrigatório.");
        }
        if (!status) {
            garantirAdminRestante(user);
        }
        entityManager.lock(user, LockModeType.OPTIMISTIC);
        user.setActive(status);
        historicoBean.registar(TipoAcao.PERFIL_ATUALIZADO, user, status ? "Conta ativada" : "Conta suspensa");
    }

    public void delete(String username) throws MyEntityNotFoundException {
        User user = findOrThrow(username);
        garantirAdminRestante(user);

        long conteudo = contar("SELECT COUNT(p) FROM Publicacao p WHERE p.submetidoPor = :u", user)
                + contar("SELECT COUNT(c) FROM Comentario c WHERE c.autor = :u", user)
                + contar("SELECT COUNT(r) FROM Rating r WHERE r.autor = :u", user);
        if (conteudo > 0) {
            throw new BusinessRuleException("O utilizador tem publicações, comentários ou ratings associados. "
                    + "Suspenda a conta em vez de a apagar.");
        }
        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.user = :u")
                .setParameter("u", user).executeUpdate();
        entityManager.remove(user);
    }

    // update nome, email
    public void updateUser(String username, UserDTO userDTO)
            throws MyEntityNotFoundException, MyEntityExistsException {
        User user = findOrThrow(username);
        if (userDTO.getName() == null || userDTO.getName().isBlank()) {
            throw new BusinessRuleException("O nome é obrigatório.");
        }
        String emailNormalizado = validarEmail(userDTO.getEmail());
        garantirEmailLivre(emailNormalizado, user);

        entityManager.lock(user, LockModeType.OPTIMISTIC);
        user.setName(userDTO.getName().trim());
        user.setEmail(emailNormalizado);
        historicoBean.registar(TipoAcao.PERFIL_ATUALIZADO, user, "Perfil atualizado");
    }

    // ---------- auxiliares ----------

    private User findOrThrow(String username) throws MyEntityNotFoundException {
        User user = findByUsername(username);
        if (user == null) {
            throw new MyEntityNotFoundException("Utilizador não encontrado: " + username);
        }
        return user;
    }

    private String validarEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessRuleException("O e-mail é inválido.");
        }
        return email.trim().toLowerCase();
    }

    private void garantirEmailLivre(String email, User atual) throws MyEntityExistsException {
        User outro = findByEmail(email);
        if (outro != null && !outro.getUsername().equals(atual.getUsername())) {
            throw new MyEntityExistsException("O e-mail já está em uso: " + email);
        }
    }

    /** Impede que o sistema fique sem nenhum administrador ativo. */
    private void garantirAdminRestante(User alvo) {
        if (Roles.ADMINISTRADOR.equals(alvo.getRole()) && alvo.isActive()) {
            long ativos = entityManager.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.role = :r AND u.active = true", Long.class)
                    .setParameter("r", Roles.ADMINISTRADOR)
                    .getSingleResult();
            if (ativos <= 1) {
                throw new BusinessRuleException("Tem de existir pelo menos um administrador ativo.");
            }
        }
    }

    private long contar(String jpql, User user) {
        return entityManager.createQuery(jpql, Long.class).setParameter("u", user).getSingleResult();
    }
}
