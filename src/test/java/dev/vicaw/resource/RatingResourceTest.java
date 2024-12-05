package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vicaw.model.Rating;
import dev.vicaw.model.Recipe;
import dev.vicaw.model.User;
import dev.vicaw.model.request.RatingCreateRequest;
import dev.vicaw.repository.RatingRepository;
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
class RatingResourceTest {

    @Inject
    RatingRepository ratingRepository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    UserRepository userRepository;

    private static final String BASE_URL = "/api/ratings";
    private List<User> users;
    private Recipe recipe;
    private List<Rating> ratings;

    private String jwtTokenUser1;
    private String jwtTokenUser4;

    private void populateUsers() {
        User user1 = User.builder()
                .name("Joao")
                .photoUrl("http://localhost:8080/images/default1.jpg")
                .build();

        User user2 = User.builder()
                .name("Maria")
                .photoUrl("http://localhost:8080/images/default2.jpg")
                .build();

        User user3 = User.builder()
                .name("Jose")
                .photoUrl("http://localhost:8080/images/default3.jpg")
                .build();

        User user4 = User.builder()
                .name("Valentina")
                .photoUrl("http://localhost:8080/images/default4.jpg")
                .build();

        users = List.of(user1, user2, user3, user4);
    }

    private void populateRecipes() {
        LocalDateTime now = LocalDateTime.now();

        recipe = Recipe.builder()
                .titulo("Bolo de Chocolate")
                .ingredientes("Farinha, Chocolate, Ovos, Açúcar")
                .modoPreparo("Misture os ingredientes e asse.")
                .about("Um delicioso bolo")
                .urlFoto("http://localhost:8080/images/recipe1.jpg")
                .createdAt(now.minusDays(5))
                .updatedAt(now.minusDays(2))
                .user(users.get(0))
                .build();
    }

    private void populateRatings() {
        User user2 = users.get(1);
        User user3 = users.get(2);

        Rating rating1 = Rating.builder()
                .comment("Excelente receita! Muito saborosa.")
                .score(5)
                .recipe(recipe)
                .user(user2)
                .build();

        Rating rating2 = Rating.builder()
                .comment("Gostei, mas pode melhorar.")
                .score(3)
                .recipe(recipe)
                .user(user3)
                .build();

        ratings = List.of(rating1, rating2); // Persistindo as avaliações
    }

    private String generateToken(User user) {
        return Jwt.issuer("http://localhost:8080")
                .upn("email@qualquer.com")
                .claim(Claims.full_name, user.getName())
                .claim(Claims.sub, user.getId().toString())
                .expiresIn(60 * 60 * 7L)
                .sign();
    }

    @BeforeAll
    @Transactional
    void insertData() {
        populateUsers();
        populateRecipes();
        populateRatings();

        userRepository.persist(users);
        recipeRepository.persist(recipe);
        ratingRepository.persist(ratings);

        jwtTokenUser1 = generateToken(users.get(0));
        jwtTokenUser4 = generateToken(users.get(3));
    }

    @AfterAll
    @Transactional
    void cleanupDatabase() {
        ratingRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    void testListRecipeRatings_Success() {
        given()
                .pathParam("id", recipe.getId())
                .when()
                .get(BASE_URL + "/{id}")
                .then()
                .statusCode(200)
                .body("ratings.size()", equalTo(ratings.size()));
    }

    @Test
    @Order(2)
    void testGetRecipeRatingInfo_Success() {
        double averageScore = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);

        given()
                .pathParam("id", recipe.getId())
                .when()
                .get(BASE_URL + "/{id}/info")
                .then()
                .statusCode(200)
                .body("score", equalTo((float) averageScore))
                .body("count", equalTo(ratings.size()));
    }

    @Test
    void testPostRecipeRating_Success() {

        RatingCreateRequest ratingRequest = RatingCreateRequest.builder()
                .comment("Muito bom!")
                .score(5)
                .build();

        given()
                .header("Authorization", "Bearer " + jwtTokenUser4)
                .contentType(ContentType.JSON)
                .body(ratingRequest)
                .pathParam("id", recipe.getId())
                .when()
                .post(BASE_URL + "/{id}")
                .then()
                .statusCode(200)
                .body("comment", equalTo(ratingRequest.getComment()))
                .body("score", equalTo(ratingRequest.getScore()));
    }

    @Test
    void testPostRecipeRating_UserIsAuthor() {

        RatingCreateRequest ratingRequest = RatingCreateRequest.builder()
                .comment("Muito bom!")
                .score(5)
                .build();

        given()
                .header("Authorization", "Bearer " + jwtTokenUser1)
                .contentType(ContentType.JSON)
                .body(ratingRequest)
                .pathParam("id", recipe.getId())
                .when()
                .post(BASE_URL + "/{id}")
                .then()
                .statusCode(403)
                .body("message", equalTo("Você não pode avaliar sua própria receita."));
    }

    @Test
    void testPostRecipeRating_InvalidData() {

        RatingCreateRequest ratingRequest = RatingCreateRequest.builder()
                .score(6)
                .build();

        given()
                .header("Authorization", "Bearer " + jwtTokenUser4)
                .contentType(ContentType.JSON)
                .body(ratingRequest)
                .when()
                .post(BASE_URL + "/" + recipe.getId())
                .then()
                .statusCode(400)
                .body("message", hasItem("O score deve ser no mínimo 1 e no máximo 5"));
    }

    @Test
    void testGetUserRating_Success() {
        User user = users.get(1);
        Rating rating = ratings.get(0);

        given()
                .when()
                .pathParam("recipeId", recipe.getId())
                .pathParam("userId", user.getId())
                .when()
                .get(BASE_URL + "/{recipeId}/{userId}")
                .then()
                .statusCode(200)
                .body("comment", equalTo(rating.getComment()))
                .body("score", equalTo(rating.getScore()));
    }
}