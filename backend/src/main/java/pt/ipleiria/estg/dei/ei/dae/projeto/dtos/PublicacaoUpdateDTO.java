package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

public class PublicacaoUpdateDTO {
    private String titulo;
    private String tipo;
    private String areaCientifica;
    private String autor;
    private Integer ano;
    private String resumo;  // Campo para atualizar o resumo manualmente

    /**
     * Versão da publicação que o cliente tinha quando abriu a edição (campo "version" do GET).
     * Se estiver preenchida e já não for a atual, o servidor responde 409: alguém alterou entretanto.
     * Opcional por compatibilidade; sem ela a última gravação ganha.
     */
    private Integer version;

    public PublicacaoUpdateDTO() {}

    public PublicacaoUpdateDTO(PublicacaoUpdateDTO updateDTO) {
        this.titulo = updateDTO.getTitulo();
        this.tipo = updateDTO.getTipo();
        this.areaCientifica = updateDTO.getAreaCientifica();
        this.autor = updateDTO.getAutor();
        this.ano = updateDTO.getAno();
        this.resumo = updateDTO.getResumo();
        this.version = updateDTO.getVersion();
    }

    public static PublicacaoUpdateDTO from (PublicacaoUpdateDTO updateDTO) {
        return new PublicacaoUpdateDTO(updateDTO);
    }

    // Getters e Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAreaCientifica() {
        return areaCientifica;
    }

    public void setAreaCientifica(String areaCientifica) {
        this.areaCientifica = areaCientifica;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
