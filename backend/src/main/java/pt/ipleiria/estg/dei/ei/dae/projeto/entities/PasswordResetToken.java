package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Token de recuperação de palavra-passe. Guarda-se apenas o hash SHA-256 do token. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    public PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, LocalDateTime expiraEm) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public User getUser() { return user; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
}
