package dev.vicaw.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserCreateRequest;
import dev.vicaw.model.request.UserUpdateRequest;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class UserResourceTest {
	@Inject
	AuthInfoRepository authInfoRepository;

	@Inject
	UserRepository userRepository;

	private static final String BASE_URL = "/api/users";

	private File testImage;

	private User user;
	private AuthInfo authInfo;
	private String jwtToken;

	@BeforeAll
	@Transactional
	void insertUser() {
		user = User.builder()
				.name("Teste Usuario")
				.photoUrl("http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png")
				.build();

		authInfo = AuthInfo.builder()
				.email("usuario@example.com")
				.password(BcryptUtil.bcryptHash("senha123"))
				.user(user)
				.build();

		userRepository.persist(user);
		authInfoRepository.persist(authInfo);

		jwtToken = Jwt
				.issuer("http://localhost:8080")
				.upn(authInfo.getEmail())
				// .groups(user.getRole().toString())
				.claim(Claims.full_name, authInfo.getUser().getName())
				.claim(Claims.sub, authInfo.getUser().getId().toString())
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
		authInfoRepository.deleteAll();
		userRepository.deleteAll();
	}

	@AfterAll
	void deleteTestImage() {
		testImage.delete();
	}

	@Test
	@Order(1)
	void testGetUserById_Success() {
		given()
				.pathParam("id", user.getId())
				.when()
				.get(BASE_URL + "/{id}")
				.then()
				.statusCode(200)
				.body("id", notNullValue())
				.body("name", equalTo(user.getName()))
				.body("photoUrl", equalTo(user.getPhotoUrl()))
				.body("email", nullValue());
	}

	@Test
	@Order(2)
	void testUpdateUser_Success() {
		UserUpdateRequest updateNameRequest = UserUpdateRequest.builder()
				.name("Novo Nome")
				.build();

		given()
				.contentType(ContentType.MULTIPART)
				.header("Authorization", "Bearer " + jwtToken)
				.multiPart("object", updateNameRequest, "application/json")
				.pathParam("id", user.getId())
				.when()
				.put(BASE_URL + "/{id}")
				.then()
				.statusCode(200)
				.body("name", equalTo(updateNameRequest.getName()));

	}

	@Test
	void testUpdateUserPassword_IncorrectCurrentPassword() {
		UserUpdateRequest updatePasswordRequest = UserUpdateRequest.builder()
				.currentPassword("senhaIncorreta")
				.newPassword("novaSenha")
				.build();

		given()
				.contentType(ContentType.MULTIPART)
				.header("Authorization", "Bearer " + jwtToken)
				.multiPart("object", updatePasswordRequest, "application/json")
				.pathParam("id", user.getId())
				.when()
				.put(BASE_URL + "/{id}")
				.then()
				.statusCode(401)
				.body("message", equalTo("Senha incorreta."));
	}

	@Test
	void testCreateUser_Success() {
		UserCreateRequest userCreateRequest = UserCreateRequest.builder()
				.name("Novo Usuario")
				.email("novoemail@example.com")
				.password("senha123")
				.build();

		given()
				.contentType(ContentType.MULTIPART)
				.multiPart("file", testImage)
				.multiPart("fileName", testImage.getName())
				.multiPart("object", userCreateRequest, "application/json")
				.when()
				.post(BASE_URL)
				.then()
				.statusCode(200)
				.body("token", notNullValue())
				.body("user.name", equalTo(userCreateRequest.getName()))
				.body("user.email", equalTo(userCreateRequest.getEmail()))
				.body("user.photoUrl", notNullValue());
	}

	@Test
	void testCreateUser_InvalidData() {
		UserCreateRequest invalidUserCreateRequest = UserCreateRequest.builder()
				.email("email_malformatado")
				.name("1")
				.password("1")
				.build();

		given()
				.contentType(ContentType.MULTIPART)
				.multiPart("object", invalidUserCreateRequest, "application/json")
				.when()
				.post(BASE_URL)
				.then()
				.statusCode(400)
				.body("message", hasItems(
						"Seu nome deve ter entre 3 e 30 caracteres",
						"Sua senha deve ter entre 8 e 128 caracteres",
						"E-mail mal formatado"));
	}

	@Test
	void testCreateUser_DuplicateEmail() {
		UserCreateRequest duplicateUserCreateRequest = UserCreateRequest.builder()
				.name("Novo Usuario")
				.email(authInfo.getEmail())
				.password("senha123")
				.build();

		given()
				.contentType(ContentType.MULTIPART)
				.multiPart("object", duplicateUserCreateRequest, "application/json")
				.when()
				.post(BASE_URL)
				.then()
				.statusCode(409)
				.body("message", equalTo("O e-mail informado já está cadastrado."));
	}

	@Test
	void testUpdateUser_Unauthenticated() {
		given()
				.contentType(ContentType.MULTIPART)
				.multiPart("object", new UserCreateRequest(), "application/json")
				.pathParam("id", user.getId())
				.when()
				.put(BASE_URL + "/{id}")
				.then()
				.statusCode(401);
	}

}
