package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    RatingRepository ratingRepository;

    @Mock
    RecipeRepository recipeRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    JsonWebToken token;

    @Mock
    PanacheQuery<Rating> ratingsQuery;

    @InjectMocks
    RatingService ratingService;

    private Recipe recipe;
    private User user;
    private Rating rating;

    @BeforeEach
    void setUp() {
        recipe = Recipe.builder()
                .id(1L)
                .user(User.builder()
                        .id(1L)
                        .name("Recipe Owner")
                        .photoUrl("photo.jpg")
                        .build())
                .build();

        user = User.builder()
                .id(2L)
                .name("Test User")
                .photoUrl("photo.jpg")
                .build();

        rating = Rating.builder()
                .id(3L)
                .comment("Muito gostoso!")
                .score(5)
                .user(user)
                .recipe(recipe)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void testList_ValidInput() {

        when(ratingsQuery.list()).thenReturn(List.of(rating));
        when(ratingsQuery.hasNextPage()).thenReturn(false);
        when(ratingsQuery.page(any(Page.class))).thenReturn(ratingsQuery);

        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));
        when(ratingRepository.listAllRecipeRatings(recipe.getId(), "highest"))
                .thenReturn(ratingsQuery);

        RecipeRatingsResponse response = ratingService.listRecipeRatings(recipe.getId(), 5, 0, "highest");

        assertEquals(1, response.getRatings().size());

        RatingResponse ratingResponse = response.getRatings().get(0);
        UserResponse userResponse = ratingResponse.getUser();

        assertEquals(rating.getId(), ratingResponse.getId());
        assertEquals(rating.getComment(), ratingResponse.getComment());
        assertEquals(rating.getScore(), ratingResponse.getScore());
        assertEquals(rating.getCreatedAt(), ratingResponse.getCreatedAt());

        assertEquals(userResponse.getId(), user.getId());
        assertEquals(userResponse.getName(), user.getName());
        assertEquals(userResponse.getPhotoUrl(), user.getPhotoUrl());

    }

    @Test
    void testPostRating_Success() {
        RatingCreateRequest request = RatingCreateRequest.builder()
                .comment("Muito bom")
                .score(4)
                .build();

        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));
        when(userRepository.findById(user.getId())).thenReturn(user);
        when(token.getSubject()).thenReturn(user.getId().toString());
        when(ratingRepository.existsByUserAndRecipeId(user.getId(), recipe.getId())).thenReturn(false);

        RatingResponse response = ratingService.postRating(recipe.getId(), request);

        assertEquals(request.getComment(), response.getComment());
        assertEquals(request.getScore(), response.getScore());

        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getName(), response.getUser().getName());
        assertEquals(user.getPhotoUrl(), response.getUser().getPhotoUrl());

        verify(ratingRepository).persist(any(Rating.class));
    }

    @Test
    void testPostRating_DuplicateRating() {
        RatingCreateRequest request = RatingCreateRequest.builder()
                .comment("Duplicate")
                .score(3)
                .build();

        Long recipeId = recipe.getId();

        when(recipeRepository.findByIdOptional(recipeId)).thenReturn(Optional.of(recipe));
        when(token.getSubject()).thenReturn(user.getId().toString());
        when(ratingRepository.existsByUserAndRecipeId(user.getId(), recipe.getId())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> ratingService.postRating(recipeId, request));

        assertEquals(409, exception.getCode());
        assertEquals("Este usuário já comentou nesta receita.", exception.getMessage());
    }

    @Test
    void testGetRatingInfo_Success() {
        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));
        when(ratingRepository.ratingCount(recipe.getId())).thenReturn(10L);
        when(ratingRepository.calculateAverageScore(recipe.getId())).thenReturn(4.5);

        RatingInfoResponse response = ratingService.getRatingInfo(recipe.getId());

        assertEquals(10L, response.getCount());
        assertEquals(4.5, response.getScore());
    }

    @Test
    void testGetRatingInfo_RecipeNotFound() {
        when(recipeRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(RecipeNotFoundException.class,
                () -> ratingService.getRatingInfo(999L));

    }

    @Test
    void testGetUserRating_Success() {
        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));
        when(ratingRepository.getUserRating(user.getId(), recipe.getId())).thenReturn(Optional.of(rating));

        RatingResponse response = ratingService.getUserRating(recipe.getId(), user.getId());

        assertEquals(rating.getComment(), response.getComment());
        assertEquals(rating.getScore(), response.getScore());

        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getName(), response.getUser().getName());
        assertEquals(user.getPhotoUrl(), response.getUser().getPhotoUrl());
    }

}