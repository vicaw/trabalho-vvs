package dev.vicaw.model.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecipeUpdateRequest {
    @Size(min = 3, max = 30, message = "O título deve ter entre 3 e 30 caracteres")
    private String titulo;

    @Size(min = 3, max = 3000, message = "O campo dos ingredientes deve ter entre 3 e 3000 caracteres")
    private String ingredientes;

    @Size(min = 3, max = 3000, message = "O campo dos ingredientes deve ter entre 3 e 3000 caracteres")
    private String modoPreparo;

    @Size(min = 3, max = 3000, message = "O campo de apresentação deve ter entre 3 e 3000 caracteres")
    private String about;
}
