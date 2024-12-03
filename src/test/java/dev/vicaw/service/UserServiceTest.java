package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.vicaw.exception.ApiException;
import dev.vicaw.exception.UserNotFoundException;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.request.UserCreateRequest;
import dev.vicaw.model.request.UserUpdateRequest;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	AuthInfoRepository authInfoRepository;

	@Mock
	AuthService authService;

	@Mock
	JsonWebToken token;

	@InjectMocks
	UserService userService;

	private User user;
	private AuthInfo authInfo;
	private UserCreateRequest userCreateRequest;
	private UserUpdateRequest userUpdateRequest;

	@BeforeEach
	void setUp() {
		user = User.builder()
				.id(1L)
				.name("Usuário Teste")
				.photoUrl("http://localhost:8080/images/default.jpg")
				.build();

		authInfo = AuthInfo.builder()
				.email("email@example.com")
				.password(BcryptUtil.bcryptHash("senha"))
				.user(user)
				.build();

		userCreateRequest = UserCreateRequest.builder()
				.name("Novo Usuario")
				.email("novoemail@example.com")
				.password("senha123")
				.build();

		userUpdateRequest = UserUpdateRequest.builder()
				.name("Novo Nome")
				.currentPassword("senha")
				.newPassword("novaSenha")
				.build();
	}

	@Test
	void testGetById_UserFoundTokenMatchesUser() {
		when(token.getRawToken()).thenReturn("userToken");
		when(token.getSubject()).thenReturn(user.getId().toString());
		when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));
		when(authInfoRepository.findByUserId(user.getId())).thenReturn(Optional.of(authInfo));

		UserResponse userResponse = userService.getById(user.getId());

		assertEquals(user.getId(), userResponse.getId());
		assertEquals(user.getName(), userResponse.getName());
		assertEquals(user.getPhotoUrl(), userResponse.getPhotoUrl());
		assertEquals(authInfo.getEmail(), userResponse.getEmail());
	}

	@Test
	void testGetById_UserFoundTokenDoesNotMatchUser() {
		when(token.getRawToken()).thenReturn("userToken");
		when(token.getSubject()).thenReturn("-1L");
		when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

		UserResponse userResponse = userService.getById(user.getId());

		assertEquals(user.getId(), userResponse.getId());
		assertEquals(user.getName(), userResponse.getName());
		assertEquals(user.getPhotoUrl(), userResponse.getPhotoUrl());
		assertNull(userResponse.getEmail());
	}

	@Test
	void testGetById_UserDoesNotExist() {
		when(userRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class,
				() -> userService.getById(999L));
	}

	@Test
	void testCreateUser_DuplicateEmail() {
		userCreateRequest.setEmail(authInfo.getEmail());

		when(authInfoRepository.findByEmail(authInfo.getEmail()))
				.thenReturn(Optional.of(authInfo));

		ApiException exception = assertThrows(ApiException.class,
				() -> userService.create(null, userCreateRequest));

		assertEquals(409, exception.getCode());
		assertEquals("O e-mail informado já está cadastrado.", exception.getMessage());
	}

	@Test
	void testCreateUser_Success() {
		UserAuthRequest userAuthRequest = UserAuthRequest.builder()
				.email(userCreateRequest.getEmail())
				.password(userCreateRequest.getPassword())
				.build();

		UserResponse userResponse = UserResponse.builder()
				.id(1L)
				.name(userCreateRequest.getName())
				.email(userCreateRequest.getEmail())
				.photoUrl("http://localhost:8080/images/default.jpg")
				.build();

		UserAuthResponse userAuthResponse = UserAuthResponse.builder()
				.token("token")
				.user(userResponse)
				.build();

		when(authInfoRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(Optional.empty());

		when(authService.authenticate(userAuthRequest)).thenReturn(userAuthResponse);

		UserAuthResponse response = userService.create(null, userCreateRequest);

		assertNotNull(response);
		assertEquals(userAuthResponse, response);

		verify(userRepository).persist(any(User.class));
		verify(authInfoRepository).persist(any(AuthInfo.class));
		verify(authService).authenticate(any(UserAuthRequest.class));
	}

	@Test
	void testUpdateUserName_Success() {
		when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));
		when(authInfoRepository.findByUserId(user.getId())).thenReturn(Optional.of(authInfo));

		UserResponse updatedUser = userService.update(user.getId(), null, userUpdateRequest);

		assertEquals(userUpdateRequest.getName(), updatedUser.getName());
		assertEquals(authInfo.getEmail(), updatedUser.getEmail());
	}

	@Test
	void testUpdateUserPassword_IncorrectCurrentPassword() {
		userUpdateRequest.setCurrentPassword("senhaErrada");

		Long userId = user.getId();

		when(userRepository.findByIdOptional(userId)).thenReturn(Optional.of(user));
		when(authInfoRepository.findByUserId(userId)).thenReturn(Optional.of(authInfo));

		ApiException exception = assertThrows(ApiException.class,
				() -> userService.update(userId, null, userUpdateRequest));

		assertEquals(401, exception.getCode());
		assertEquals("Senha incorreta.", exception.getMessage());
	}
}