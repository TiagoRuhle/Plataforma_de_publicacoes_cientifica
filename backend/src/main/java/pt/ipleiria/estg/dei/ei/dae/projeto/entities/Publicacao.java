package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "getAllPublicacoes",
                query = "SELECT p FROM Publicacao p ORDER BY p.id"
        ),
        @NamedQuery(
                name = "getVisivelPublicacoes",
                query = "SELECT p FROM Publicacao p WHERE p.visivel = true ORDER BY p.id"
        ),
        @NamedQuery(
                name = "getInvisivelPublicacoes",
                query = "SELECT p FROM Publicacao p WHERE p.visivel = false ORDER BY p.id"
        ),
        @NamedQuery(
                name = "findPublicacoesByAutor",
                query = "SELECT p FROM Publicacao p WHERE p.autor = :autor ORDER BY p.id"
        ),
        @NamedQuery(
                name = "findPublicacoesByTag",
                query = "SELECT p FROM Publicacao p JOIN p.tags t WHERE t.nome = :tag ORDER BY p.id"
        ),
        @NamedQuery(
                name = "findMinhasPublicacoes",
                query = "SELECT p FROM Publicacao p WHERE p.submetidoPor.username = :username ORDER BY p.id"
        )
})
@Table(name = "publicacoes", indexes = {
        @Index(name = "idx_publicacoes_visivel", columnList = "visivel"),
        @Index(name = "idx_publicacoes_ano", columnList = "ano")
})
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * Controlo de concorrência otimista: o Hibernate incrementa-o a cada alteração e recusa gravações feitas sobre
     * uma versão antiga. O default 0 permite acrescentar a coluna a uma tabela que já tem linhas.
     */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "integer default 0")
    private int version;

    @NotBlank
    private String titulo;

    private String tipo;

    private String areaCientifica;

    private String filename;

    private String filepath;

    /** Tipo MIME do ficheiro guardado (application/pdf ou application/zip). */
    private String mimeType;

    @Column(length = 20000)
    private String resumo;

    @Enumerated(EnumType.STRING)
    private ResumoEstado resumoEstado;

    private boolean visivel;

    // ano da publicação original
    private int ano;

    /** Quando foi submetida na plataforma. */
    private LocalDateTime dataSubmissao;

    @NotBlank
    private String autor;

    // QUEM SUBMETEU NA PLATAFORMA
    @ManyToOne
    @JoinColumn(name = "submetido_por_username")
    private User submetidoPor;

    @OneToMany(mappedBy = "publicacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios;

    @OneToMany(mappedBy = "publicacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "publicacao_tags",
            joinColumns = @JoinColumn(name = "publicacao_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_nome")
    )
    private List<Tag> tags;

    public Publicacao() {
        this.comentarios = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.visivel = true;
        this.dataSubmissao = LocalDateTime.now();
    }

    public Publicacao(String titulo, String tipo, String areaCientifica, String filepath, String filename,
                      String mimeType, String autor, int ano, User submetidoPor) {
        this();
        this.titulo = titulo;
        this.tipo = tipo;
        this.areaCientifica = areaCientifica;
        this.filepath = filepath;
        this.filename = filename;
        this.mimeType = mimeType;
        this.autor = autor;
        this.ano = ano;
        this.submetidoPor = submetidoPor;
    }

    public long getId() { return id; }

    public int getVersion() { return version; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAreaCientifica() { return areaCientifica; }
    public void setAreaCientifica(String areaCientifica) { this.areaCientifica = areaCientifica; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFilepath() { return filepath; }
    public void setFilepath(String filepath) { this.filepath = filepath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public ResumoEstado getResumoEstado() { return resumoEstado; }
    public void setResumoEstado(ResumoEstado resumoEstado) { this.resumoEstado = resumoEstado; }

    public boolean isVisivel() { return visivel; }
    public void setVisivel(boolean visivel) { this.visivel = visivel; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public LocalDateTime getDataSubmissao() { return dataSubmissao; }
    public void setDataSubmissao(LocalDateTime dataSubmissao) { this.dataSubmissao = dataSubmissao; }

    public User getSubmetidoPor() { return submetidoPor; }
    public void setSubmetidoPor(User submetidoPor) { this.submetidoPor = submetidoPor; }

    public List<Comentario> getComentarios() { return comentarios; }

    public List<Rating> getRatings() { return ratings; }

    public List<Tag> getTags() { return tags; }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }
}
