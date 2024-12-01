package dev.vicaw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.vicaw.exception.UserNotFoundException;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    AuthInfoRepository authInfoRepository;

    @Mock
    JsonWebToken token;

    @InjectMocks
    UserService userService;

    @Test
    void testGetById_UserFound_TokenMatchesUser() {
        User user = User.builder()
                .id(1L)
                .name("Teste Usuario")
                .photoUrl("http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png")
                .build();

        AuthInfo authInfo = AuthInfo.builder()
                .email("email@example.com")
                .build();

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
    void testGetById_UserFound_TokenDoesNotMatchUser() {
        User user = User.builder()
                .id(1L)
                .name("Teste Usuario")
                .photoUrl("http://localhost:8080/images/acde070d-8c4c-4f0d-9d8a-162843c10333-profilePicture.png")
                .build();

        when(token.getRawToken()).thenReturn("userToken");
        when(token.getSubject()).thenReturn("2");

        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        UserResponse userResponse = userService.getById(user.getId());

        assertEquals(user.getId(), userResponse.getId());
        assertEquals(user.getName(), userResponse.getName());
        assertEquals(user.getPhotoUrl(), userResponse.getPhotoUrl());
        assertNull(userResponse.getEmail());
    }

    @Test
    void testGetById_userNotFound() {

        when(userRepository.findByIdOptional(-1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getById(-1L));

        assertEquals(404, exception.getCode());
        assertEquals("Não existe nenhum usuário com o ID informado.", exception.getMessage());
    }
}
