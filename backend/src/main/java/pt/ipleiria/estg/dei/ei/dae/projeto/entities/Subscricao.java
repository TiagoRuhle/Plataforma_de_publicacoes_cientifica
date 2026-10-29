package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.persistence.*;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "getAllSubscricoes",
                query = "SELECT s FROM Subscricao s ORDER BY s.user.username"
        ),
        @NamedQuery(
                name = "getAllSubscricoesByUser",
                query = "SELECT s FROM Subscricao s WHERE s.user.username = :username"
        ),
        @NamedQuery(
                name = "getSubscricaoByUserAndTag",
                query = "SELECT s FROM Subscricao s WHERE s.user.username = :username AND s.tag.nome = :tag"
        )
})
@Table(name = "subscricoes")
public class Subscricao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_username", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "nome", nullable = false)
    private Tag tag;

    public Subscricao() {
    }

    public Subscricao(User user, Tag tag) {
        this.user = user;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
