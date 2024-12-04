package dev.vicaw.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.vicaw.exception.ApiException;
import dev.vicaw.exception.RecipeNotFoundException;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.RecipeCreateRequest;
import dev.vicaw.model.request.RecipeUpdateRequest;
import dev.vicaw.model.response.RecipeListResponse;
import dev.vicaw.model.response.RecipeResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.RecipeRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RequestScoped
public class RecipeService {

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    ImageService imageService;

    @Inject
    UserRepository userRepository;

    @Inject
    RatingService ratingService;

    @Inject
    JsonWebToken token;

    public RecipeListResponse list(Long authorId, Integer pagesize, Integer pagenumber, String orderBy) {
        PanacheQuery<Recipe> recipesQuery;

        if (authorId != null) {
            recipesQuery = recipeRepository.listAllUserRecipes(authorId, orderBy);
        } else {
            recipesQuery = recipeRepository.listAllRecipes(orderBy);
        }

        List<Recipe> recipeList;
        boolean hasMore = false;

        if (pagesize != null && pagenumber != null) {
            PanacheQuery<Recipe> page = recipesQuery.page(Page.of(pagenumber, pagesize));
            hasMore = page.hasNextPage();
            recipeList = page.list();
        } else {
            recipeList = recipesQuery.list();
        }

        List<RecipeResponse> recipes = recipeList.stream()
                .map(recipe -> RecipeResponse.builder()
                        .id(recipe.getId())
                        .titulo(recipe.getTitulo())
                        .urlFoto(recipe.getUrlFoto())
                        .rating(ratingService.getAverageRating(recipe.getId()))
                        .ratingCount(ratingService.getRatingCount(recipe.getId()))
                        .ingredientes(recipe.getIngredientes())
                        .about(recipe.getAbout())
                        .modoPreparo(recipe.getModoPreparo())
                        .createdAt(recipe.getCreatedAt())
                        .updatedAt(recipe.getUpdatedAt())
                        .user(UserResponse.builder().id(recipe.getUser().getId()).name(recipe.getUser().getName())
                                .photoUrl(recipe.getUser().getPhotoUrl()).build())
                        .build())
                .collect(Collectors.toList());

        return RecipeListResponse.builder().hasMore(hasMore).recipes(recipes).build();
    }

    public RecipeResponse getById(Long id) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(id);

        if (recipeOptional.isEmpty())
            throw new RecipeNotFoundException();

        Recipe recipe = recipeOptional.get();

        UserResponse userResponse = UserResponse.builder()
                .id(recipe.getUser().getId())
                .name(recipe.getUser().getName())
                .photoUrl(recipe.getUser().getPhotoUrl())
                .build();

        return RecipeResponse.builder()
                .id(recipe.getId())
                .user(userResponse)
                .titulo(recipe.getTitulo())
                .ingredientes(recipe.getIngredientes())
                .about(recipe.getAbout())
                .urlFoto(recipe.getUrlFoto())
                .modoPreparo(recipe.getModoPreparo())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }

    @Transactional
    public RecipeResponse create(MultipartBody body, @Valid RecipeCreateRequest recipeCreateRequest) {
        String photoUrl = imageService.save(body);

        Long userId = Long.parseLong(token.getSubject());
        User user = userRepository.findById(userId);

        Recipe recipe = Recipe.builder()
                .titulo(recipeCreateRequest.getTitulo())
                .ingredientes(recipeCreateRequest.getIngredientes())
                .modoPreparo(recipeCreateRequest.getModoPreparo())
                .about(recipeCreateRequest.getAbout())
                .user(user)
                .urlFoto(photoUrl)
                .build();

        recipeRepository.persist(recipe);

        UserResponse userResponse = UserResponse.builder()
                .id(recipe.getUser().getId())
                .name(recipe.getUser().getName())
                .photoUrl(recipe.getUser().getPhotoUrl())
                .build();

        return RecipeResponse.builder()
                .id(recipe.getId())
                .user(userResponse)
                .titulo(recipe.getTitulo())
                .ingredientes(recipe.getIngredientes())
                .about(recipe.getAbout())
                .modoPreparo(recipe.getModoPreparo())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }

    @Transactional
    public RecipeResponse update(Long recipeId, MultipartBody body, @Valid RecipeUpdateRequest recipeUpdateRequest) {

        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new ApiException(404, "Não existe nenhuma receita com o ID informado.");

        Recipe recipe = recipeOptional.get();

        if (!token.getSubject().equals(recipe.getUser().getId().toString()))
            throw new ApiException(403, "Você não pode editar receitas de outros usuários.");

        String titulo = recipeUpdateRequest.getTitulo();
        String ingredientes = recipeUpdateRequest.getIngredientes();
        String modoPreparo = recipeUpdateRequest.getModoPreparo();

        if (titulo != null && !titulo.isBlank()) {
            recipe.setTitulo(titulo);
        }

        if (ingredientes != null && !ingredientes.isBlank()) {
            recipe.setIngredientes(ingredientes);
        }

        if (modoPreparo != null && !modoPreparo.isBlank()) {
            recipe.setModoPreparo(modoPreparo);
        }

        if (body.getImage() != null) {
            String photoUrl = imageService.save(body);
            recipe.setUrlFoto(photoUrl);
        }

        UserResponse userResponse = UserResponse.builder()
                .id(recipe.getUser().getId())
                .name(recipe.getUser().getName())
                .photoUrl(recipe.getUser().getPhotoUrl())
                .build();

        return RecipeResponse.builder()
                .id(recipe.getId())
                .titulo(recipe.getTitulo())
                .ingredientes(recipe.getIngredientes())
                .modoPreparo(recipe.getModoPreparo())
                .user(userResponse)
                .urlFoto(recipe.getUrlFoto())
                .build();
    }

    @Transactional
    public void delete(Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findByIdOptional(recipeId);

        if (recipeOptional.isEmpty())
            throw new ApiException(404, "Não existe nenhuma receita com o ID informado.");

        Recipe recipe = recipeOptional.get();

        if (!token.getSubject().equals(recipe.getUser().getId().toString()))
            throw new ApiException(403, "Você não pode deletar receitas de outros usuários.");

        recipeRepository.delete(recipe);
    }

    public RecipeListResponse searchRecipe(String query, Integer pagesize, Integer pagenumber, String orderBy) {
        PanacheQuery<Recipe> recipesQuery = recipeRepository.search(query, orderBy);
        PanacheQuery<Recipe> page = recipesQuery.page(Page.of(pagenumber, pagesize));
        Boolean hasMore = page.hasNextPage();

        List<RecipeResponse> recipesResponse = page.list().stream()
                .map(recipe -> RecipeResponse.builder()
                        .id(recipe.getId())
                        .titulo(recipe.getTitulo())
                        .urlFoto(recipe.getUrlFoto())
                        .about(recipe.getAbout())
                        .rating(ratingService.getAverageRating(recipe.getId()))
                        .ratingCount(ratingService.getRatingCount(recipe.getId()))
                        .createdAt(recipe.getCreatedAt())
                        .updatedAt(recipe.getUpdatedAt())
                        .user(UserResponse.builder().id(recipe.getUser().getId()).name(recipe.getUser().getName())
                                .photoUrl(recipe.getUser().getPhotoUrl()).build())
                        .build())
                .collect(Collectors.toList());

        return RecipeListResponse.builder().hasMore(hasMore).recipes(recipesResponse).build();
    }
}
