package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Tag;


import java.util.List;
import java.util.stream.Collectors;

public class TagDTO {

    private String nome;
    private Boolean visivel;

    public TagDTO() {}

    public TagDTO(String tag, boolean visivel) {
        this.nome = tag;
        this.visivel = visivel;
    }

    public static TagDTO from(Tag tag) {
        return new TagDTO(
                tag.getTag(),
                tag.isVisivel()
        );
    }

    public static List<TagDTO> from(List<Tag> tags) {
        return tags.stream().map(TagDTO::from).collect(Collectors.toList());
    }

    public String getTag() {
        return nome;
    }

    public void setTag(String tag) {
        this.nome = tag;
    }

    public Boolean isVisivel() {
        return visivel;
    }

    public void setVisivel(Boolean visivel) {
        this.visivel = visivel;
    }
}
