package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import java.util.ArrayList;
import java.util.List;

/** Dados de uma nova publicação (já lidos do multipart). Não é serializado em JSON. */
public class NovaPublicacaoDTO {
    private String titulo;
    private String tipo;
    private String areaCientifica;
    private String autor;
    private int ano;
    private String filename;
    private byte[] conteudo;
    private List<String> tags = new ArrayList<>();

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAreaCientifica() { return areaCientifica; }
    public void setAreaCientifica(String areaCientifica) { this.areaCientifica = areaCientifica; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public byte[] getConteudo() { return conteudo; }
    public void setConteudo(byte[] conteudo) { this.conteudo = conteudo; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
