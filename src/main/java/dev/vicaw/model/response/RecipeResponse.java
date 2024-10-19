package dev.vicaw.model.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class RecipeResponse {
    private Long id;
    private UserResponse user;
    private String titulo;
    private String ingredientes;
    private String modoPreparo;
    private String about;
    private String urlFoto;
    private Double rating;
    private Long ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
