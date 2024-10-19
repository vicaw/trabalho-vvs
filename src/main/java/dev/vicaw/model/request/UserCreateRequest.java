package dev.vicaw.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotNull
    @Size(min = 3, max = 30, message = "Seu nome deve ter entre 3 e 30 caracteres")
    private String name;

    private String photoUrl;

    @Email(message = "E-mail mal formatado")
    @NotNull(message = "Seu e-email não pode ficar em branco")
    @Size(min = 7, max = 128, message = "E-mail muito curto, verifique se ele está correto")
    private String email;

    @NotNull(message = "Sua senha não pode ficar em branco")
    @Size(min = 8, max = 128, message = "Sua senha deve ter entre 8 e 128 caracteres")
    private String password;
}
