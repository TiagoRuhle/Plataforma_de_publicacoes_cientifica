package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordDTO {
    @NotBlank
    private String token;
    @NotBlank
    private String newPassword;

    public ResetPasswordDTO() {
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
