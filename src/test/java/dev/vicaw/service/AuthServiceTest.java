package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.repository.AuthInfoRepository;
import io.quarkus.elytron.security.common.BcryptUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    AuthService authService;

    @Mock
    AuthInfoRepository authInfoRepository;

    private static UserAuthRequest userAuthRequest = new UserAuthRequest("email@example.com", "senha");

    @Test
    void testAuthenticate_userNotFound() {
        when(authInfoRepository.findByEmail("email@example.com")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.authenticate(userAuthRequest));

        assertEquals(401, exception.getCode());
        assertEquals("Seu usuário ou senha estão incorretos.", exception.getMessage());
    }

    @Test
    void testAuthenticate_invalidPassword() {
        AuthInfo authInfo = AuthInfo.builder()
                .password(BcryptUtil.bcryptHash("senhaIncorreta"))
                .build();

        when(authInfoRepository.findByEmail("email@example.com")).thenReturn(Optional.of(authInfo));

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.authenticate(userAuthRequest));

        assertEquals(401, exception.getCode());
        assertEquals("Seu usuário ou senha estão incorretos.", exception.getMessage());
    }

    @Test
    void testAuthenticate_success() {
        User user = User.builder()
                .id(1L)
                .name("Usuario Teste")
                .build();

        AuthInfo authInfo = AuthInfo.builder()
                .user(user)
                .password(BcryptUtil.bcryptHash("senha"))
                .build();

        when(authInfoRepository.findByEmail("email@example.com")).thenReturn(Optional.of(authInfo));

        UserAuthResponse response = authService.authenticate(userAuthRequest);

        assertNotNull(response.getToken());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getName(), response.getUser().getName());
    }
}