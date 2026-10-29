package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "getAllUsers",
                query = "SELECT u FROM User u ORDER BY u.name"
        ),
        @NamedQuery(
                name = "getUserByUsername",
                query = "SELECT u FROM User u WHERE u.username = :username"
        )
})

@Table(name = "users")
public class User extends Versionable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String role;

    private boolean active;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastActivity;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonbTransient
    private List<Historico> historicos;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonbTransient
    private List<Subscricao> subscricoes;

    public User() {

    }

    public User(String username, String password, String name, String email, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.historicos = new ArrayList<>();
        this.subscricoes = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Historico> getHistoricos() {
        return historicos;
    }

    public List<Subscricao> getSubscricoes() {
        return subscricoes;
    }

    public void addSubscricao(Tag tag) {
        Subscricao s = new Subscricao(this, tag);
        subscricoes.add(s);
    }

    public void removeSubscricao(Tag tag) {
        subscricoes.removeIf(s -> s.getTag().equals(tag));
    }
}

