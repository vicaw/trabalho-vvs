package dev.vicaw.model.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeRatingsResponse {
    boolean hasMore;
    List<RatingResponse> ratings;
    RatingInfoResponse ratingInfo;
}
