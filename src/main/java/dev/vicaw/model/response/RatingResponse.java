package dev.vicaw.model.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class RatingResponse {
    private Long id;
    private UserResponse user;
    private String comment;
    private Integer score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}