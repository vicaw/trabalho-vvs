package dev.vicaw.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserAuthRequest {
    @Email(message = "E-mail mal formatado")
    @NotNull(message = "Seu e-email não pode ficar em branco")
    private String email;

    @NotNull(message = "Sua senha não pode ficar em branco")
    @Size(min = 8, max = 128, message = "Sua senha deve ter entre 8 e 128 caracteres")
    private String password;
}
