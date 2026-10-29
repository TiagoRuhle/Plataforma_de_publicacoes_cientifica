package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Comentario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ComentarioDTO {

    private Long id;
    private String texto;
    private String autor;
    private boolean visivel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss dd-MM-yyyy")
    private LocalDateTime data;

    public ComentarioDTO() {}

    public ComentarioDTO(Long id, String texto, String autor, LocalDateTime data, boolean visivel) {
        this.id = id;
        this.texto = texto;
        this.autor = autor;
        this.data = data;
        this.visivel = visivel;
    }

    public static ComentarioDTO from(Comentario comentario) {
        return new ComentarioDTO(
                comentario.getId(),
                comentario.getTexto(),
                comentario.getAutor().getUsername(),
                comentario.getData(),
                comentario.isVisivel()
        );
    }

    public static List<ComentarioDTO> from(List<Comentario> comentarios) {
        return comentarios.stream()
                .map(ComentarioDTO::from)
                .collect(Collectors.toList());
    }

    public static List<ComentarioDTO> fromVisiveis(List<Comentario> comentarios) {
        return comentarios.stream()
                .filter(Comentario::isVisivel)
                .map(ComentarioDTO::from)
                .collect(Collectors.toList());
    }


    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public String getAutor() { return autor; }
    public LocalDateTime getData() { return data; }
    public boolean isVisivel() {
        return visivel;
    }
    public void setVisivel(boolean visivel) {
        this.visivel = visivel;
    }
}
