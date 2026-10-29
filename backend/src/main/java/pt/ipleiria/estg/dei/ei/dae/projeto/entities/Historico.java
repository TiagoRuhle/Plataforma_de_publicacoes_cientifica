package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico")
@NamedQueries({
        @NamedQuery(
                name = "getAllHistorico",
                query = "SELECT h FROM Historico h ORDER BY h.dataHora DESC"
        ),
        @NamedQuery(
                name = "getHistoricoByUser",
                query = "SELECT h FROM Historico h WHERE h.user = :user ORDER BY h.dataHora DESC"
        ),
        @NamedQuery(
                name = "getHistoricoByPublicacao",
                query = "SELECT h FROM Historico h WHERE h.publicacao = :publicacao ORDER BY h.dataHora DESC"
        ),
        @NamedQuery(
                name = "getHistoricoByTipoAcao",
                query = "SELECT h FROM Historico h WHERE h.tipoAcao = :tipoAcao ORDER BY h.dataHora DESC"
        )
})
public class Historico implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAcao tipoAcao;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "user_username")
    @NotNull
    private User user;

    @ManyToOne
    @JoinColumn(name = "publicacao_id")
    private Publicacao publicacao;  // pode ser null para ações não relacionadas a publicações

    @Column(length = 1000)
    private String descricao;  // descrição detalhada da ação

    @Column(length = 500)
    private String detalhes;  // informações extras (JSON ou texto)

    public Historico() {
        this.dataHora = LocalDateTime.now();
    }

    public Historico(TipoAcao tipoAcao, User user, Publicacao publicacao, String descricao) {
        this();
        this.tipoAcao = tipoAcao;
        this.user = user;
        this.publicacao = publicacao;
        this.descricao = descricao;
    }

    public Historico(TipoAcao tipoAcao, User user, Publicacao publicacao, String descricao, String detalhes) {
        this(tipoAcao, user, publicacao, descricao);
        this.detalhes = detalhes;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoAcao getTipoAcao() {
        return tipoAcao;
    }

    public void setTipoAcao(TipoAcao tipoAcao) {
        this.tipoAcao = tipoAcao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Publicacao getPublicacao() {
        return publicacao;
    }

    public void setPublicacao(Publicacao publicacao) {
        this.publicacao = publicacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}