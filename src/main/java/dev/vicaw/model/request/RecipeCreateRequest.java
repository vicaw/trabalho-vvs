package dev.vicaw.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipeCreateRequest {
    @NotNull
    @Size(min = 3, max = 30, message = "O título deve ter entre 3 e 30 caracteres")
    private String titulo;

    @NotNull
    @Size(min = 3, max = 3000, message = "O campo dos ingredientes deve ter entre 3 e 3000 caracteres")
    private String ingredientes;

    @NotNull
    @Size(min = 3, max = 3000, message = "O campo dos ingredientes deve ter entre 3 e 3000 caracteres")
    private String modoPreparo;

    @NotNull
    @Size(min = 3, max = 3000, message = "O campo de apresentação deve ter entre 3 e 3000 caracteres")
    private String about;
}
