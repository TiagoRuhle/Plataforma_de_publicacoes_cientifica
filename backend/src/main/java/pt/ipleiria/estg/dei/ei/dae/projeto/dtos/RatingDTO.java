package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Rating;
import java.util.List;
import java.util.stream.Collectors;

public class RatingDTO {
    private long id;
    private String autor;  // username de quem deu o rating
    private int rating;    // 1-5

    public RatingDTO() {}

    public RatingDTO(long id, String autor, int rating) {
        this.id = id;
        this.autor = autor;
        this.rating = rating;
    }

    // Converter de Rating para RatingDTO
    public static RatingDTO from(Rating rating) {
        return new RatingDTO(
                rating.getId(),
                rating.getAutor().getUsername(),
                rating.getValor()
        );
    }

    // Converter lista
    public static List<RatingDTO> from(List<Rating> ratings) {
        return ratings.stream()
                .map(RatingDTO::from)
                .collect(Collectors.toList());
    }

    // Getters e Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}