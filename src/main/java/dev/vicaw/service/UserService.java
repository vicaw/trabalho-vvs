package dev.vicaw.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.vicaw.exception.ApiException;
import dev.vicaw.exception.UserNotFoundException;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.User;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.request.UserCreateRequest;
import dev.vicaw.model.request.UserUpdateRequest;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

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
            throw new UserNotFoundException();

        User user = userOptional.get();
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .build();

        if (token.getRawToken() != null && token.getSubject().equals(user.getId().toString())) {
            Optional<AuthInfo> authInfo = authInfoRepository.findByUserId(user.getId());
            if (authInfo.isEmpty())
                throw new ApiException(404,
                        "Informações de autenticação não encontradas para o usuário com ID " + user.getId());

            userResponse.setEmail(authInfo.get().getEmail());
        }

        return userResponse;
    }

    @Transactional
    public UserAuthResponse create(MultipartBody body, @Valid UserCreateRequest userCreateRequest) {
        if (authInfoRepository.findByEmail(userCreateRequest.getEmail()).isPresent())
            throw new ApiException(409, "O e-mail informado já está cadastrado.");

        String photoUrl = "#";
        if (body != null && body.getImage() != null)
            photoUrl = imageService.save(body);

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
            throw new UserNotFoundException();

        User user = userOptional.get();

        Optional<AuthInfo> authInfoOptional = authInfoRepository.findByUserId(user.getId());
        if (authInfoOptional.isEmpty())
            throw new ApiException(404,
                    "Informações de autenticação não encontradas para o usuário com ID " + user.getId());

        AuthInfo authInfo = authInfoOptional.get();

        if (updateInput.getName() != null && !updateInput.getName().isBlank()) {
            user.setName(updateInput.getName());
        }

        if (body != null && body.getImage() != null) {
            String photoUrl = imageService.save(body);
            user.setPhotoUrl(photoUrl);
        }

        if (updateInput.getNewPassword() != null && !updateInput.getNewPassword().isBlank()) {
            if (!BcryptUtil.matches(updateInput.getCurrentPassword(), authInfo.getPassword()))
                throw new ApiException(401, "Senha incorreta.");

            authInfo.setPassword(BcryptUtil.bcryptHash(updateInput.getNewPassword()));
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(authInfo.getEmail())
                .photoUrl(user.getPhotoUrl())
                .build();
    }

    @Transactional
    public void delete(Long id) {
        Optional<User> userOptional = userRepository.findByIdOptional(id);

        if (userOptional.isEmpty())
            throw new UserNotFoundException();

        User user = userOptional.get();

        userRepository.delete(user);
    }

}