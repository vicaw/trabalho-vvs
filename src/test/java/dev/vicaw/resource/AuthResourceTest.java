package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import dev.vicaw.service.AuthService;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class AuthResourceTest {

    @Inject
    AuthService authService;

    @Inject
    AuthInfoRepository authInfoRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        User user = User.builder()
                .name("Teste Usuario")
                .photoUrl("http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png")
                .build();

        userRepository.persist(user);

        AuthInfo authInfo = AuthInfo.builder()
                .email("usuario@example.com")
                .password(BcryptUtil.bcryptHash("senha123"))
                .user(user)
                .build();

        authInfoRepository.persist(authInfo);
    }

    @Test
    void testAuthenticate_validUser() {
        UserAuthRequest userAuthRequest = new UserAuthRequest("usuario@example.com", "senha123");

        given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.name", equalTo("Teste Usuario"))
                .body("user.photoUrl", equalTo(
                        "http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png"));
    }

    @Test
    @Transactional
    void testAuthenticate_invalidCredentials() {
        UserAuthRequest userAuthRequest = new UserAuthRequest("usuario@example.com", "senhaIncorreta");

        given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Seu usuário ou senha estão incorretos."));
    }

}