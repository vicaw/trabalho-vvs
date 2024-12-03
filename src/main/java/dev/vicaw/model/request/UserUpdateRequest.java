package dev.vicaw.model.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UserUpdateRequest {
    @Size(min = 3, max = 30, message = "Seu nome deve ter entre 3 e 30 caracteres")
    private String name;

    @Size(min = 8, max = 128, message = "Sua senha deve ter entre 8 e 128 caracteres")
    private String newPassword;

    @Size(min = 8, max = 128, message = "Sua senha deve ter entre 8 e 128 caracteres")
    private String currentPassword;
}
