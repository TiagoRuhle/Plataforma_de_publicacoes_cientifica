package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateComentarioDTO {

    @NotNull
    private Long publicacaoId;

    @NotBlank
    private String texto;

    public CreateComentarioDTO() { }

    public CreateComentarioDTO(Long publicacaoId, String texto) {
        this.publicacaoId = publicacaoId;
        this.texto = texto;
    }

    public Long getPublicacaoId() {
        return publicacaoId;
    }

    public void setPublicacaoId(Long publicacaoId) {
        this.publicacaoId = publicacaoId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
