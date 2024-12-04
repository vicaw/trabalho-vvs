package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.vicaw.exception.RecipeNotFoundException;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.response.RecipeListResponse;
import dev.vicaw.model.response.RecipeResponse;
import dev.vicaw.repository.RecipeRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

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
    PanacheQuery<Recipe> page;

    @InjectMocks
    RecipeService recipeService;

    @Captor
    ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);

    private Recipe recipe;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("User Test").photoUrl("http://localhost:8080/images/default.jpg").build();
        recipe = Recipe.builder()
                .id(1L)
                .titulo("Recipe Test")
                .ingredientes("Ingredients")
                .modoPreparo("Preparation steps")
                .about("About the recipe")
                .user(user)
                .build();

    }

    @Test
    void testList_RecipesWithoutFilters() {
        when(recipesQuery.list()).thenReturn(List.of(recipe, recipe, recipe));
        when(recipeRepository.listAllRecipes(isNull())).thenReturn(recipesQuery);

        RecipeListResponse response = recipeService.list(null, null, null, null);

        assertEquals(3, response.getRecipes().size());
    }

    @Test
    void testList_RecipesWithPaginationAndAuthor() {
        int pageNumber = 0;
        int pageSize = 10;

        when(recipesQuery.page(pageCaptor.capture())).thenReturn(page);
        when(page.list()).thenReturn(List.of(recipe, recipe));
        when(page.hasNextPage()).thenReturn(false);
        when(recipeRepository.listAllUserRecipes(1L, null)).thenReturn(recipesQuery);

        RecipeListResponse response = recipeService.list(1L, pageSize, pageNumber, null);

        Page capturedPage = pageCaptor.getValue();
        assertEquals(pageNumber, capturedPage.index);
        assertEquals(pageSize, capturedPage.size);

        assertEquals(2, response.getRecipes().size());
        assertFalse(response.isHasMore());
    }

    @Test
    void testGetById_RecipeExists() {
        when(recipeRepository.findByIdOptional(1L)).thenReturn(Optional.of(recipe));

        RecipeResponse response = recipeService.getById(1L);

        assertEquals(recipe.getId(), response.getId());
        assertEquals(recipe.getTitulo(), response.getTitulo());
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

}
