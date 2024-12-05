package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import dev.vicaw.exception.RecipeNotFoundException;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.RecipeCreateRequest;
import dev.vicaw.model.request.RecipeUpdateRequest;
import dev.vicaw.model.response.RecipeListResponse;
import dev.vicaw.model.response.RecipeResponse;
import dev.vicaw.repository.RecipeRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    RecipeRepository recipeRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ImageService imageService;

    @Mock
    RatingService ratingService;

    @Mock
    PanacheQuery<Recipe> recipesQuery;

    @Mock
    JsonWebToken token;

    @InjectMocks
    RecipeService recipeService;

    private User user1;
    private User user2;
    private List<Recipe> allRecipes;
    private List<Recipe> recipesUser1;
    private List<Recipe> recipesUser2;

    private void setUpUsers() {
        user1 = User.builder()
                .id(1L)
                .name("User Test 1")
                .photoUrl("http://localhost:8080/images/default1.jpg")
                .build();

        user2 = User.builder()
                .id(2L)
                .name("User Test 2")
                .photoUrl("http://localhost:8080/images/default2.jpg")
                .build();
    }

    private void setUpRecipes() {
        LocalDateTime now = LocalDateTime.now();

        Recipe recipe1 = Recipe.builder()
                .id(1L)
                .titulo("Receita 1 do Usuario 1")
                .ingredientes("Ingredientes 1")
                .modoPreparo("Passos de preparo 1")
                .about("Sobre a receita 1")
                .urlFoto("http://localhost:8080/images/recipe1.jpg")
                .createdAt(now.minusDays(5))
                .updatedAt(now.minusDays(2))
                .user(user1)
                .build();

        Recipe recipe2 = Recipe.builder()
                .id(2L)
                .titulo("Receita 2 do Usuario 1")
                .ingredientes("Ingredientes 2")
                .modoPreparo("Passos de preparo 2")
                .about("Sobre a receita 2")
                .urlFoto("http://localhost:8080/images/recipe2.jpg")
                .createdAt(now.minusDays(4))
                .updatedAt(now.minusDays(1))
                .user(user1)
                .build();

        Recipe recipe3 = Recipe.builder()
                .id(3L)
                .titulo("Receita 3 do Usuario 1")
                .ingredientes("Ingredientes 3")
                .modoPreparo("Passos de preparo 3")
                .about("Sobre a receita 3")
                .urlFoto("http://localhost:8080/images/recipe3.jpg")
                .createdAt(now.minusDays(3))
                .updatedAt(now)
                .user(user1)
                .build();

        Recipe recipe4 = Recipe.builder()
                .id(4L)
                .titulo("Receita 1 do Usuario 2")
                .ingredientes("Ingredientes 4")
                .modoPreparo("Passos de preparo 4")
                .about("Sobre a receita 4")
                .urlFoto("http://localhost:8080/images/recipe4.jpg")
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusHours(5))
                .user(user2)
                .build();

        Recipe recipe5 = Recipe.builder()
                .id(5L)
                .titulo("Receita 2 do Usuario 2")
                .ingredientes("Ingredientes 5")
                .modoPreparo("Passos de preparo 5")
                .about("Sobre a receita 5")
                .urlFoto("http://localhost:8080/images/recipe5.jpg")
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusHours(2))
                .user(user2)
                .build();

        allRecipes = List.of(recipe1, recipe2, recipe3, recipe4, recipe5);
        recipesUser1 = List.of(recipe1, recipe2, recipe3);
        recipesUser2 = List.of(recipe4, recipe5);
    }

    @BeforeEach
    void setUp() {
        setUpUsers();
        setUpRecipes();
    }

    @Test
    void testList_RecipesWithoutFilters() {
        when(ratingService.getAverageRating(anyLong())).thenReturn(4.5);
        when(ratingService.getRatingCount(anyLong())).thenReturn(10L);
        when(recipesQuery.list()).thenReturn(allRecipes);
        when(recipeRepository.listRecipes(null, null, null)).thenReturn(recipesQuery);

        RecipeListResponse response = recipeService.list(null, null, null, null);

        assertEquals(5, response.getRecipes().size());

        RecipeResponse recipeResponse = response.getRecipes().get(0);
        Recipe recipe = allRecipes.get(0);

        assertEquals(recipe.getId(), recipeResponse.getId());
        assertEquals(recipe.getTitulo(), recipeResponse.getTitulo());
        assertEquals(recipe.getIngredientes(), recipeResponse.getIngredientes());
        assertEquals(recipe.getModoPreparo(), recipeResponse.getModoPreparo());
        assertEquals(recipe.getAbout(), recipeResponse.getAbout());
        assertEquals(recipe.getCreatedAt(), recipeResponse.getCreatedAt());
        assertEquals(recipe.getUpdatedAt(), recipeResponse.getUpdatedAt());

        assertEquals(recipe.getUser().getId(), recipeResponse.getUser().getId());
        assertEquals(recipe.getUser().getName(), recipeResponse.getUser().getName());
        assertEquals(recipe.getUser().getPhotoUrl(), recipeResponse.getUser().getPhotoUrl());

        assertEquals(4.5, recipeResponse.getRating());
        assertEquals(10, recipeResponse.getRatingCount());
    }

    @Test
    void testList_RecipesWithPaginationAndAuthor() {
        int pageNumber = 0;
        int pageSize = 10;

        when(ratingService.getAverageRating(anyLong())).thenReturn(4.5);
        when(ratingService.getRatingCount(anyLong())).thenReturn(10L);
        when(recipesQuery.list()).thenReturn(recipesUser1);
        when(recipesQuery.hasNextPage()).thenReturn(false);
        when(recipeRepository.listUserRecipes(user1.getId(), null, pageSize, pageNumber))
                .thenReturn(recipesQuery);

        RecipeListResponse response = recipeService.list(1L, pageSize, pageNumber, null);

        verify(recipesQuery).hasNextPage();
        assertEquals(recipesUser1.size(), response.getRecipes().size());

    }

    @Test
    void testGetById_RecipeExists() {
        Recipe recipe = allRecipes.get(0);
        when(ratingService.getAverageRating(recipe.getId())).thenReturn(4.5);
        when(ratingService.getRatingCount(recipe.getId())).thenReturn(10L);
        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));

        RecipeResponse response = recipeService.getById(recipe.getId());

        assertEquals(recipe.getId(), response.getId());
        assertEquals(recipe.getTitulo(), response.getTitulo());
        assertEquals(recipe.getIngredientes(), response.getIngredientes());
        assertEquals(recipe.getModoPreparo(), response.getModoPreparo());
        assertEquals(recipe.getAbout(), response.getAbout());
        assertEquals(recipe.getUrlFoto(), response.getUrlFoto());
        assertEquals(recipe.getCreatedAt(), response.getCreatedAt());
        assertEquals(recipe.getUpdatedAt(), response.getUpdatedAt());

        assertEquals(recipe.getUser().getId(), response.getUser().getId());
        assertEquals(recipe.getUser().getName(), response.getUser().getName());
        assertEquals(recipe.getUser().getPhotoUrl(), response.getUser().getPhotoUrl());

        assertEquals(4.5, response.getRating());
        assertEquals(10, response.getRatingCount());
    }

    @Test
    void testGetById_RecipeNotFound() {
        when(recipeRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        RecipeNotFoundException exception = assertThrows(RecipeNotFoundException.class,
                () -> recipeService.getById(999L));

        assertEquals(404, exception.getCode());
        assertEquals("Não existe nenhuma receita com o ID informado.",
                exception.getMessage());
    }

    @Test
    void testCreateRecipe_Success() {
        MultipartBody multipartBody = new MultipartBody();
        RecipeCreateRequest request = RecipeCreateRequest.builder()
                .titulo("New Recipe")
                .ingredientes("New Ingredients")
                .modoPreparo("New Preparation")
                .about("New About")
                .build();

        String photoUrl = "http://localhost:8080/images/new.jpg";

        when(imageService.save(any(MultipartBody.class))).thenReturn(
                photoUrl);

        when(userRepository.findById(user2.getId())).thenReturn(user2);
        when(token.getSubject()).thenReturn(user2.getId().toString());

        RecipeResponse response = recipeService.create(multipartBody,
                request);

        assertEquals(response.getTitulo(), request.getTitulo());
        assertEquals(response.getIngredientes(), request.getIngredientes());
        assertEquals(response.getModoPreparo(), request.getModoPreparo());
        assertEquals(response.getAbout(), request.getAbout());

        assertEquals(response.getUser().getId(), user2.getId());
        assertEquals(response.getUser().getName(), user2.getName());
        assertEquals(response.getUser().getPhotoUrl(), user2.getPhotoUrl());

        assertEquals(null, response.getRating());
        assertEquals(null, response.getRatingCount());

        verify(recipeRepository).persist(any(Recipe.class));
    }

    @Test
    void testUpdateRecipe_Success() {
        MultipartBody multipartBody = new MultipartBody();
        RecipeUpdateRequest request = RecipeUpdateRequest.builder()
                .titulo("Updated Recipe")
                .ingredientes("Updated Ingredients")
                .modoPreparo("Updated Preparation")
                .build();

        Recipe recipe = recipesUser2.get(1);

        when(token.getSubject()).thenReturn(user2.getId().toString());
        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));

        RecipeResponse response = recipeService.update(recipe.getId(), multipartBody,
                request);

        assertEquals(response.getTitulo(), request.getTitulo());
        assertEquals(response.getIngredientes(), request.getIngredientes());
        assertEquals(response.getModoPreparo(), request.getModoPreparo());
        assertEquals(response.getAbout(), recipe.getAbout());

        assertEquals(response.getUser().getId(), user2.getId());
        assertEquals(response.getUser().getName(), user2.getName());
        assertEquals(response.getUser().getPhotoUrl(), user2.getPhotoUrl());
    }

    @Test
    void testDeleteRecipe_Success() {
        Recipe recipe = recipesUser2.get(1);
        when(token.getSubject()).thenReturn(user2.getId().toString());

        when(recipeRepository.findByIdOptional(recipe.getId())).thenReturn(Optional.of(recipe));

        recipeService.delete(recipe.getId());

        verify(recipeRepository).delete(recipe);
    }

    @Test
    void testSearchRecipe_Success() {
        String query = "do Usuario 2";
        String orderBy = "recent";
        int pageNumber = 0;
        int pageSize = 10;

        when(ratingService.getAverageRating(anyLong())).thenReturn(4.5);
        when(ratingService.getRatingCount(anyLong())).thenReturn(10L);
        when(recipesQuery.list()).thenReturn(recipesUser2);
        when(recipesQuery.hasNextPage()).thenReturn(false);

        when(recipeRepository.search(query, orderBy, pageNumber, pageSize)).thenReturn(recipesQuery);

        RecipeListResponse response = recipeService.searchRecipe(query, pageSize, pageNumber,
                orderBy);

        assertEquals(recipesUser2.size(), response.getRecipes().size());

        verify(recipesQuery).hasNextPage();

        RecipeResponse recipeResponse = response.getRecipes().get(0);
        Recipe recipe = recipesUser2.get(0);

        assertEquals(recipe.getId(), recipeResponse.getId());
        assertEquals(recipe.getTitulo(), recipeResponse.getTitulo());
        assertEquals(null, recipeResponse.getIngredientes());
        assertEquals(null, recipeResponse.getModoPreparo());
        assertEquals(recipe.getAbout(), recipeResponse.getAbout());
        assertEquals(recipe.getCreatedAt(), recipeResponse.getCreatedAt());
        assertEquals(recipe.getUpdatedAt(), recipeResponse.getUpdatedAt());

        assertEquals(recipe.getUser().getId(), recipeResponse.getUser().getId());
        assertEquals(recipe.getUser().getName(), recipeResponse.getUser().getName());
        assertEquals(recipe.getUser().getPhotoUrl(), recipeResponse.getUser().getPhotoUrl());

        assertEquals(4.5, recipeResponse.getRating());
        assertEquals(10, recipeResponse.getRatingCount());
    }

}
