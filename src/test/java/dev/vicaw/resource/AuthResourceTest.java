package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import dev.vicaw.service.AuthService;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
                .photoUrl("photoUrl")
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

        Response response = given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        UserAuthResponse userAuthResponse = response.as(UserAuthResponse.class);
        assertNotNull(userAuthResponse.getToken());
        assertEquals("Teste Usuario", userAuthResponse.getUser().getName());
    }

    @Test

    @Transactional
    void testAuthenticate_invalidCredentials() {
        UserAuthRequest userAuthRequest = new UserAuthRequest("usuario@example.com", "senhaIncorreta");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(userAuthRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .extract().response();

        assertEquals("Seu usuário ou senha estão incorretos.",
                response.jsonPath().getString("message"));
    }

}