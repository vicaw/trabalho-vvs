package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthResourceTest {
    @Inject
    AuthInfoRepository authInfoRepository;

    @Inject
    UserRepository userRepository;

    private static String validEmail = "usuario@example.com";
    private static String validPassword = "senha123";
    private static String invalidPassword = "senhaIncorreta";

    private static final String BASE_URL = "/api/auth";

    private User user;
    private AuthInfo authInfo;

    @BeforeAll
    @Transactional
    void insertUser() {
        user = User.builder()
                .name("Teste Usuario")
                .photoUrl("http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png")
                .build();

        authInfo = AuthInfo.builder()
                .email(validEmail)
                .password(BcryptUtil.bcryptHash(validPassword))
                .user(user)
                .build();

        userRepository.persist(user);
        authInfoRepository.persist(authInfo);
    }

    @Test
    void testAuthenticate_validUser() {
        UserAuthRequest userAuthRequest = new UserAuthRequest(validEmail, validPassword);

        given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post(BASE_URL + "/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.name", equalTo(user.getName()))
                .body("user.email", equalTo(authInfo.getEmail()))
                .body("user.photoUrl", equalTo(user.getPhotoUrl()));
    }

    @Test
    void testAuthenticate_invalidCredentials() {
        UserAuthRequest userAuthRequest = new UserAuthRequest(validEmail, invalidPassword);

        given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post(BASE_URL + "/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Seu usuário ou senha estão incorretos."));
    }

    @AfterAll
    @Transactional
    void cleanupDatabase() {
        authInfoRepository.deleteAll();
        userRepository.deleteAll();
    }

}