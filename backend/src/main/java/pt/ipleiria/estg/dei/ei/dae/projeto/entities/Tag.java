package pt.ipleiria.estg.dei.ei.dae.projeto.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "getAllTags",
                query = "SELECT t FROM Tag t ORDER BY t.nome"
        ),
        @NamedQuery(
                name = "getTagByNome",
                query = "SELECT t FROM Tag t WHERE t.nome = :nome"
        ),
        @NamedQuery(
                name = "getVisibleTags",
                query = "SELECT t FROM Tag t WHERE t.visivel = true"
        )
})
@Table(name = "tags")
public class Tag {

    @Id
    @NotBlank
    @Column(unique = true)
    private String nome;

    private boolean visivel;

    public Tag() {}

    public Tag(String tag, boolean visivel) {
        this.nome = tag;
        this.visivel = visivel;
    }

    public String getTag() {
        return nome;
    }

    public void setTag(String tag) {
        this.nome = tag;
    }

    public boolean isVisivel() {
        return visivel;
    }

    public void setVisivel(boolean visivel) {
        this.visivel = visivel;
    }
}
