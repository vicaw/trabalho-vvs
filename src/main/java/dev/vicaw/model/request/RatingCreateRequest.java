package dev.vicaw.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class RatingCreateRequest {
    @NotNull
    @DecimalMin(value = "1", message = "O score deve ser no mínimo 1 e no máximo 5")
    @DecimalMax(value = "5", message = "O score deve ser no mínimo 1 e no máximo 5")
    private Integer score;

    @Size(min = 3, max = 200, message = "Seu comentário deve ter entre 3 e 200 caracteres")
    private String comment;
}
