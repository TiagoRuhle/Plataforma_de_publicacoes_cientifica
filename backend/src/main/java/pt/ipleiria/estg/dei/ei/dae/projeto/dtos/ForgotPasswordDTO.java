package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordDTO {
    @NotBlank
    private String email;

    public ForgotPasswordDTO() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
