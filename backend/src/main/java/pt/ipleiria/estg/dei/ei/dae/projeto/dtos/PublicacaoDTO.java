package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Publicacao;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.Rating;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Versão "de lista" de uma publicação: sem comentários nem ratings individuais. */
public class PublicacaoDTO {
    private Long id;
    private Integer version;
    private String titulo;
    private String tipo;
    private String areaCientifica;
    private String filename;
    private String resumoIa;
    private String resumoEstado;
    private boolean visivel;
    private int ano;
    private LocalDateTime dataSubmissao;
    private String autor; // autor do ficheiro
    private String submetidoPor; // username
    private int totalComentarios;
    private int totalRatings;
    private double ratingMedio;
    private List<TagDTO> tags;

    public PublicacaoDTO() {
    }

    public PublicacaoDTO(Long id, String titulo, String tipo, String areaCientifica, String filename,
                         String resumoIa, String resumoEstado, boolean visivel, int ano,
                         LocalDateTime dataSubmissao, String autor, String submetidoPor,
                         List<TagDTO> tags, int totalComentarios, int totalRatings, double ratingMedio) {
        this.id = id;
        this.titulo = titulo;
        this.tipo = tipo;
        this.areaCientifica = areaCientifica;
        this.filename = filename;
        this.resumoIa = resumoIa;
        this.resumoEstado = resumoEstado;
        this.visivel = visivel;
        this.ano = ano;
        this.dataSubmissao = dataSubmissao;
        this.autor = autor;
        this.submetidoPor = submetidoPor;
        this.tags = tags;
        this.totalComentarios = totalComentarios;
        this.totalRatings = totalRatings;
        this.ratingMedio = ratingMedio;
    }

    /**
     * A partir de uma entidade com coleções já inicializadas.
     * Só Responsável/Admin ({@code verOcultos}) veem tags e comentários ocultos.
     */
    public static PublicacaoDTO of(Publicacao p, boolean verOcultos) {
        int comentarios = (int) p.getComentarios().stream().filter(c -> verOcultos || c.isVisivel()).count();
        int ratings = p.getRatings().size();
        double media = ratings == 0 ? 0.0 : p.getRatings().stream().mapToInt(Rating::getValor).average().orElse(0.0);
        return ofRow(p, comentarios, ratings, media, verOcultos);
    }

    /** A partir de uma linha da pesquisa, em que os totais já vêm calculados pela BD. */
    public static PublicacaoDTO ofRow(Publicacao p, long totalComentarios, long totalRatings,
                                      double ratingMedio, boolean verOcultos) {
        PublicacaoDTO dto = new PublicacaoDTO(
                p.getId(), p.getTitulo(), p.getTipo(), p.getAreaCientifica(), p.getFilename(),
                p.getResumo(), p.getResumoEstado() != null ? p.getResumoEstado().name() : null,
                p.isVisivel(), p.getAno(), p.getDataSubmissao(), p.getAutor(),
                p.getSubmetidoPor() != null ? p.getSubmetidoPor().getUsername() : null,
                tagsVisiveis(p, verOcultos),
                (int) totalComentarios, (int) totalRatings, ratingMedio);
        dto.setVersion(p.getVersion());
        return dto;
    }

    protected static List<TagDTO> tagsVisiveis(Publicacao p, boolean verOcultos) {
        return p.getTags().stream()
                .filter(t -> verOcultos || t.isVisivel())
                .map(TagDTO::from)
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }

    /** Versão para controlo de concorrência: enviar de volta no PUT para detetar edições simultâneas. */
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAreaCientifica() { return areaCientifica; }
    public void setAreaCientifica(String areaCientifica) { this.areaCientifica = areaCientifica; }

    /** Mantém o nome JSON "ficheiro" que o frontend já usa. */
    public String getFicheiro() { return filename; }
    public void setFicheiro(String filename) { this.filename = filename; }

    public String getResumoIa() { return resumoIa; }
    public void setResumoIa(String resumoIa) { this.resumoIa = resumoIa; }

    public String getResumoEstado() { return resumoEstado; }
    public void setResumoEstado(String resumoEstado) { this.resumoEstado = resumoEstado; }

    public boolean isVisivel() { return visivel; }
    public void setVisivel(boolean visivel) { this.visivel = visivel; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public LocalDateTime getDataSubmissao() { return dataSubmissao; }
    public void setDataSubmissao(LocalDateTime dataSubmissao) { this.dataSubmissao = dataSubmissao; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getSubmetidoPor() { return submetidoPor; }
    public void setSubmetidoPor(String submetidoPor) { this.submetidoPor = submetidoPor; }

    public List<TagDTO> getTags() { return tags; }
    public void setTags(List<TagDTO> tags) { this.tags = tags; }

    public int getTotalComentarios() { return totalComentarios; }
    public void setTotalComentarios(int totalComentarios) { this.totalComentarios = totalComentarios; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public double getRatingMedio() { return ratingMedio; }
    public void setRatingMedio(double ratingMedio) { this.ratingMedio = ratingMedio; }
}
