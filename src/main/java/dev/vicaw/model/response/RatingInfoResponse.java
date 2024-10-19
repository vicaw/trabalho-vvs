package dev.vicaw.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingInfoResponse {
    private Double score;
    private Long count;
}