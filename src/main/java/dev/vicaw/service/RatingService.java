package dev.vicaw.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.vicaw.exception.ApiException;
import dev.vicaw.exception.RecipeNotFoundException;
import dev.vicaw.model.Rating;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.request.RatingCreateRequest;
import dev.vicaw.model.response.RatingInfoResponse;
import dev.vicaw.model.response.RatingResponse;
import dev.vicaw.model.response.RecipeRatingsResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.RatingRepository;
import dev.vicaw.repository.RecipeRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@RequestScoped
public class RatingService {

    @Inject
    JsonWebToken token;

    @Inject
    UserRepository userRepository;

    @Inject
    RatingRepository ratingRepository;

    @Inject
    RecipeRepository recipeRepository;

    public RecipeRatingsResponse listRecipeRatings(Long recipeId, int pagesize, int pagenumber, String orderBy) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        PanacheQuery<Rating> page = ratingRepository.listAllRecipeRatings(recipeId, orderBy)
                .page(Page.of(pagenumber, pagesize));

        List<Rating> ratings = page.list();
        boolean hasMore = page.hasNextPage();

        List<RatingResponse> ratingsResponse = ratings.stream()
                .map(rating -> RatingResponse.builder()
                        .id(rating.getId())
                        .user(UserResponse.builder().id(rating.getUser().getId()).name(rating.getUser().getName())
                                .photoUrl(rating.getUser().getPhotoUrl()).build())
                        .comment(rating.getComment())
                        .score(rating.getScore())
                        .createdAt(rating.getCreatedAt())
                        .updatedAt(rating.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return RecipeRatingsResponse.builder()
                .ratings(ratingsResponse)
                .ratingInfo(getRatingInfo(recipeId))
                .hasMore(hasMore)
                .build();
    }

    @Transactional
    public RatingResponse postRating(Long recipeId, RatingCreateRequest ratingCreateRequest) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        Long userId = Long.valueOf(token.getSubject());
        User user = userRepository.findById(userId);

        if (recipeOptional.get().getUser().getId().equals(userId)) {
            throw new ApiException(403, "Você não pode avaliar sua própria receita.");
        }

        if (ratingRepository.existsByUserAndRecipeId(userId, recipeId)) {
            throw new ApiException(409, "Este usuário já comentou nesta receita.");
        }

        Rating rating = Rating.builder()
                .comment(ratingCreateRequest.getComment())
                .score(ratingCreateRequest.getScore())
                .recipe(recipeOptional.get())
                .user(user)
                .build();

        ratingRepository.persist(rating);

        return RatingResponse.builder()
                .id(rating.getId())
                .user(UserResponse.builder().id(rating.getUser().getId()).name(rating.getUser().getName())
                        .photoUrl(rating.getUser().getPhotoUrl()).build())
                .comment(rating.getComment())
                .score(rating.getScore())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    public RatingInfoResponse getRatingInfo(Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        Long count = ratingRepository.ratingCount(recipeId);
        Double score = ratingRepository.calculateAverageScore(recipeId);

        return RatingInfoResponse.builder().count(count)
                .score(score).build();
    }

    public Double getAverageRating(Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        return ratingRepository.calculateAverageScore(recipeId);
    }

    public Long getRatingCount(Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        return ratingRepository.ratingCount(recipeId);
    }

    public RatingResponse getUserRating(Long recipeId, Long userId) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new ApiException(404, "Não existe nenhuma receita com o ID informado.");

        Optional<User> userOptional = userRepository.findByIdOptional(userId);

        if (userOptional.isEmpty()) {
            throw new ApiException(404, "Não existe nenhum usuário com o ID informado.");
        }

        Optional<Rating> ratingOptional = ratingRepository.getUserRating(userId, recipeId);
        if (ratingOptional.isEmpty()) {
            throw new ApiException(404, "O usuário não avaliou a receita informada.");
        }

        Rating rating = ratingOptional.get();

        return RatingResponse.builder()
                .id(rating.getId())
                .user(UserResponse.builder().id(rating.getUser().getId()).name(rating.getUser().getName())
                        .photoUrl(rating.getUser().getPhotoUrl()).build())
                .comment(rating.getComment())
                .score(rating.getScore())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

}
