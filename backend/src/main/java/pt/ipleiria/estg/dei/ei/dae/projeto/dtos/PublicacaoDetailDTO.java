package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Comentario;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;

import java.util.List;

/** Detalhe de uma publicação: inclui comentários e ratings. */
public class PublicacaoDetailDTO extends PublicacaoDTO {
    private List<ComentarioDTO> comentarios;
    private List<RatingDTO> ratings;

    public PublicacaoDetailDTO() {
    }

    /**
     * Comentários e tags ocultos só são incluídos se {@code verOcultos}
     * (Responsável/Admin). Para os restantes o conteúdo oculto nem sai do servidor.
     */
    public static PublicacaoDetailDTO detalhe(Publicacao p, boolean verOcultos) {
        PublicacaoDTO base = PublicacaoDTO.of(p, verOcultos);
        List<Comentario> comentarios = p.getComentarios().stream()
                .filter(c -> verOcultos || c.isVisivel())
                .toList();

        PublicacaoDetailDTO dto = new PublicacaoDetailDTO();
        dto.setId(base.getId());
        dto.setVersion(base.getVersion());
        dto.setTitulo(base.getTitulo());
        dto.setTipo(base.getTipo());
        dto.setAreaCientifica(base.getAreaCientifica());
        dto.setFicheiro(base.getFicheiro());
        dto.setResumoIa(base.getResumoIa());
        dto.setResumoEstado(base.getResumoEstado());
        dto.setVisivel(base.isVisivel());
        dto.setAno(base.getAno());
        dto.setDataSubmissao(base.getDataSubmissao());
        dto.setAutor(base.getAutor());
        dto.setSubmetidoPor(base.getSubmetidoPor());
        dto.setTags(base.getTags());
        dto.setTotalComentarios(comentarios.size());
        dto.setTotalRatings(base.getTotalRatings());
        dto.setRatingMedio(base.getRatingMedio());
        dto.comentarios = ComentarioDTO.from(comentarios);
        dto.ratings = RatingDTO.from(p.getRatings());
        return dto;
    }

    public List<ComentarioDTO> getComentarios() { return comentarios; }
    public void setComentarios(List<ComentarioDTO> comentarios) { this.comentarios = comentarios; }

    public List<RatingDTO> getRatings() { return ratings; }
    public void setRatings(List<RatingDTO> ratings) { this.ratings = ratings; }
}
