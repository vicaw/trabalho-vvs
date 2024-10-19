package dev.vicaw.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.request.UserCreateRequest;
import dev.vicaw.model.request.UserUpdateRequest;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import io.quarkus.elytron.security.common.BcryptUtil;

@RequestScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    AuthInfoRepository authInfoRepository;

    @Inject
    AuthService authService;

    @Inject
    ImageService imageService;

    @Inject
    JsonWebToken token;

    public List<User> list() {
        return userRepository.listAll();
    }

    public UserResponse getById(Long id) {
        Optional<User> userOptional = userRepository.findByIdOptional(id);

        if (userOptional.isEmpty())
            throw new ApiException(404, "Não foi encontrado nenhum usuário com o ID informado.");

        User user = userOptional.get();
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .build();

        if (token.getRawToken() != null && token.getSubject().equals(user.getId().toString())) {
            AuthInfo authInfo = authInfoRepository.findByUserId(user.getId());
            userResponse.setEmail(authInfo.getEmail());
        }

        return userResponse;
    }

    @Transactional
    public UserAuthResponse create(MultipartBody body, @Valid UserCreateRequest userCreateRequest) {
        if (authInfoRepository.findByEmail(userCreateRequest.getEmail()).isPresent())
            throw new ApiException(409, "O e-mail informado já está cadastrado.");

        String photoUrl = imageService.save(body);

        User user = User.builder()
                .name(userCreateRequest.getName())
                .photoUrl(photoUrl)
                .build();

        userRepository.persist(user);

        AuthInfo authInfo = AuthInfo.builder()
                .email(userCreateRequest.getEmail())
                .password(BcryptUtil.bcryptHash(userCreateRequest.getPassword()))
                .user(user)
                .build();

        authInfoRepository.persist(authInfo);

        return authService
                .authenticate(new UserAuthRequest(userCreateRequest.getEmail(), userCreateRequest.getPassword()));
    }

    @Transactional
    public UserResponse update(Long userId, MultipartBody body, @Valid UserUpdateRequest updateInput) {

        Optional<User> userOptional = userRepository.findByIdOptional(userId);

        if (userOptional.isEmpty())
            throw new ApiException(404, "Não foi encontrado nenhum usuário com o ID informado.");

        User user = userOptional.get();
        AuthInfo authInfo = authInfoRepository.findByUserId(userId);

        if (updateInput.getName() != null && !updateInput.getName().isBlank()) {
            user.setName(updateInput.getName());
        }

        if (body.getImage() != null) {
            String photoUrl = imageService.save(body);
            user.setPhotoUrl(photoUrl);
        }

        if (updateInput.getNewPassword() != null && !updateInput.getNewPassword().isBlank()) {
            if (!BcryptUtil.matches(updateInput.getCurrentPassword(), authInfo.getPassword()))
                throw new ApiException(401, "Senha incorreta.");

            authInfo.setPassword(BcryptUtil.bcryptHash(updateInput.getNewPassword()));
        }

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(authInfo.getEmail())
                .photoUrl(user.getPhotoUrl())
                .build();

        return userResponse;
    }

    @Transactional
    public void delete(Long id) {
        Optional<User> userOptional = userRepository.findByIdOptional(id);

        if (userOptional.isEmpty())
            throw new ApiException(404, "Não foi encontrado nenhum usuário com o ID informado.");

        User user = userOptional.get();
        // Não é permitido deletar administradores
        // if (user.getRole() == Role.ADMIN)
        // throw new ApiException(403, "Não é permitido remover outros
        // administradores.");

        userRepository.delete(user);

        return;
    }

}