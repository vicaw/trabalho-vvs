package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vicaw.exception.RecipeNotFoundException;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.request.RecipeCreateRequest;
import dev.vicaw.model.request.RecipeUpdateRequest;
import dev.vicaw.repository.RecipeRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class RecipeResourceTest {
    @Inject
    RecipeRepository recipeRepository;

    @Inject
    UserRepository userRepository;

    private static final String BASE_URL = "/api/recipes";

    private File testImage;

    private List<User> users;
    private List<Recipe> recipes;
    private String jwtToken;

    @BeforeAll
    @Transactional
    void insertData() {
        populateUsers();
        populateRecipes();

        userRepository.persist(users);
        recipeRepository.persist(recipes);

        jwtToken = Jwt
                .issuer("http://localhost:8080")
                .upn("email@qualquer.com")
                .claim(Claims.full_name, users.get(0).getName())
                .claim(Claims.sub, users.get(0).getId().toString())
                .expiresIn(60 * 60 * 7L)
                .sign();
    }

    @BeforeAll
    void createTempImage() throws IOException {
        File tempFile = File.createTempFile("test-image", ".jpg");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 });
        }
        testImage = tempFile;
    }

    @AfterAll
    @Transactional
    void cleanupDatabase() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterAll
    void deleteTestImage() {
        testImage.delete();
    }

    void populateUsers() {
        User user1 = User.builder()
                .name("Joao")
                .photoUrl("http://localhost:8080/images/default1.jpg")
                .build();

        User user2 = User.builder()
                .name("Maria")
                .photoUrl("http://localhost:8080/images/default2.jpg")
                .build();

        users = Arrays.asList(user1, user2);
    }

    void populateRecipes() {
        LocalDateTime now = LocalDateTime.now();

        Recipe recipeUser1 = Recipe.builder()
                .titulo("Bolo de Chocolate")
                .ingredientes("Farinha, Chocolate, Ovos, Açúcar")
                .modoPreparo("Misture os ingredientes e asse.")
                .about("Um delicioso bolo")
                .urlFoto("http://localhost:8080/images/recipe1.jpg")
                .createdAt(now.minusDays(5))
                .updatedAt(now.minusDays(2))
                .user(users.get(0))
                .build();

        Recipe recipe2User1 = Recipe.builder()
                .titulo("Sopa de Legumes")
                .ingredientes("Cenoura, Batata, Cebola, Caldo")
                .modoPreparo("Ferva os legumes e adicione o caldo.")
                .about("Sopa gostosa")
                .urlFoto("http://localhost:8080/images/recipe2.jpg")
                .createdAt(now.minusDays(4))
                .updatedAt(now.minusDays(1))
                .user(users.get(0))
                .build();

        Recipe recipeUser2 = Recipe.builder()
                .titulo("Bolo de Cenoura")
                .ingredientes("Farinha, Cenoura, Ovos, Açúcar")
                .modoPreparo("Misture os ingredientes e asse.")
                .about("Um delicioso bolo")
                .urlFoto("http://localhost:8080/images/recipe3.jpg")
                .createdAt(now.minusDays(3))
                .updatedAt(now.minusDays(1))
                .user(users.get(1))
                .build();

        Recipe recipe2User2 = Recipe.builder()
                .titulo("Bolo de Cenoura com Chocolate")
                .ingredientes("Farinha, Cenoura, Chocolate, Ovos, Açúcar")
                .modoPreparo("Misture os ingredientes e asse.")
                .about("Um delicioso bolo")
                .urlFoto("http://localhost:8080/images/recipe3.jpg")
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(1))
                .user(users.get(1))
                .build();

        recipes = Arrays.asList(recipeUser1, recipe2User1, recipeUser2, recipe2User2);
    }

    @Test
    @Order(1)
    void testGetAllRecipes_WithoutFilters() {
        given()
                .when()
                .get(BASE_URL)
                .then()
                .statusCode(200)
                .body("recipes.size()", equalTo(4));
    }

    @Test
    @Order(2)
    void testGetAllRecipes_WithFilters() {
        User user = users.get(0);
        Recipe recipe = recipes.get(1);

        given()
                .queryParam("authorId", user.getId())
                .queryParam("pagesize", 5)
                .queryParam("page", 0)
                .queryParam("orderBy", "recent")
                .when()
                .get(BASE_URL)
                .then()
                .statusCode(200)
                .body("recipes.size()", equalTo(2))
                .body("recipes[0].id", equalTo(recipe.getId().intValue()));
    }

    @Test
    @Order(3)
    void testSearchRecipes_WithValidQueryParams() {
        Recipe recipeRecent = recipes.get(3);
        Recipe recipeOlder = recipes.get(2);

        given()
                .queryParam("q", "bolo")
                .queryParam("pagesize", 2)
                .queryParam("orderBy", "recent")
                .when()
                .get(BASE_URL + "/search")
                .then()
                .statusCode(200)
                .body("recipes.size()", equalTo(2))
                .body("recipes[0].id", equalTo(recipeRecent.getId().intValue()))
                .body("recipes[1].id", equalTo(recipeOlder.getId().intValue()))
                .body("hasMore", equalTo(true));
    }

    @Test
    void testSearchRecipes_WithInvalidQueryParams() {
        given()
                .queryParam("q", "bo")
                .when()
                .get(BASE_URL + "/search")
                .then()
                .statusCode(400)
                .body("message", hasItem("O termo de busca deve ser maior que 3 caracteres"));
    }

    @Test
    @Order(4)
    void testGetRecipeById_Success() {
        Recipe recipe = recipes.get(1);

        given()
                .pathParam("id", recipe.getId())
                .when()
                .get(BASE_URL + "/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(recipe.getId().intValue()));
    }

    @Test
    void testGetRecipeById_InvalidId() {
        given()
                .pathParam("id", 999)
                .when()
                .get(BASE_URL + "/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo(RecipeNotFoundException.ERROR_MESSAGE));
    }

    @Test
    void testCreateRecipe_Success() {
        RecipeCreateRequest request = RecipeCreateRequest.builder()
                .titulo("New Recipe")
                .ingredientes("New Ingredients")
                .modoPreparo("New Preparation")
                .about("New About")
                .build();

        given()
                .contentType(ContentType.MULTIPART)
                .header("Authorization", "Bearer " + jwtToken)
                .multiPart("file", testImage)
                .multiPart("fileName", testImage.getName())
                .multiPart("object", request, "application/json")
                .when()
                .post(BASE_URL)
                .then()
                .statusCode(200)
                .body("titulo", equalTo(request.getTitulo()));
    }

    @Test
    void testCreateRecipe_InvalidData() {
        RecipeCreateRequest request = RecipeCreateRequest.builder()
                .titulo("a")
                .ingredientes("a")
                .modoPreparo("a")
                .about("a")
                .build();

        given()
                .contentType(ContentType.MULTIPART)
                .header("Authorization", "Bearer " + jwtToken)
                .multiPart("file", testImage)
                .multiPart("fileName", testImage.getName())
                .multiPart("object", request, "application/json")
                .when()
                .post(BASE_URL)
                .then()
                .statusCode(400)
                .body("message", hasItems(
                        "O título deve ter entre 3 e 30 caracteres",
                        "O campo dos ingredientes deve ter entre 3 e 3000 caracteres",
                        "O campo dos ingredientes deve ter entre 3 e 3000 caracteres",
                        "O campo de apresentação deve ter entre 3 e 3000 caracteres"));
    }

    @Test
    void testUpdateRecipe_Success() {
        Recipe recipe = recipes.get(0);

        RecipeUpdateRequest request = RecipeUpdateRequest.builder()
                .titulo("Novo Titulo")
                .build();

        given()
                .contentType(ContentType.MULTIPART)
                .header("Authorization", "Bearer " + jwtToken)
                .multiPart("object", request, "application/json")
                .pathParam("id", recipe.getId())
                .when()
                .put(BASE_URL + "/{id}")
                .then()
                .statusCode(200)
                .body("titulo", equalTo(request.getTitulo()))
                .body("ingredientes", equalTo(recipe.getIngredientes()))
                .body("about", equalTo(recipe.getAbout()))
                .body("modoPreparo", equalTo(recipe.getModoPreparo()));
    }

    @Test
    void testeDeleteRecipe_Success() {
        Recipe recipeFromUser = recipes.get(0);

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .pathParam("id", recipeFromUser.getId())
                .when()
                .delete(BASE_URL + "/{id}")
                .then()
                .statusCode(200);

    }

    @Test
    void testeDeleteRecipe_TokenDoesNotMatchAuthor() {
        Recipe recipeFromAnotherUser = recipes.get(2);

        given()
                .header("Authorization", "Bearer " + jwtToken)
                .pathParam("id", recipeFromAnotherUser.getId())
                .when()
                .delete(BASE_URL + "/{id}")
                .then()
                .statusCode(403)
                .body("message", equalTo("Você não pode deletar receitas de outros usuários."));

    }

}
