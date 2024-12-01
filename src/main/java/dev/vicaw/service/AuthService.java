package dev.vicaw.service;

import java.util.Optional;

import org.eclipse.microprofile.jwt.Claims;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.AuthInfo;
import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.model.response.UserAuthResponse;
import dev.vicaw.model.response.UserResponse;
import dev.vicaw.repository.AuthInfoRepository;
import dev.vicaw.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class AuthService {

    @Inject
    AuthInfoRepository authInfoRepository;

    @Inject
    UserRepository userRepository;

    public UserAuthResponse authenticate(UserAuthRequest userAuthRequest) {
        Optional<AuthInfo> entity = authInfoRepository.findByEmail(userAuthRequest.getEmail());

        if (entity.isEmpty())
            throw new ApiException(401, "Seu usuário ou senha estão incorretos.");

        AuthInfo authInfo = entity.get();

        if (!BcryptUtil.matches(userAuthRequest.getPassword(), authInfo.getPassword()))
            throw new ApiException(401, "Seu usuário ou senha estão incorretos.");

        String token = Jwt
                .issuer("http://localhost:8080")
                .upn(authInfo.getEmail())
                // .groups(user.getRole().toString())
                .claim(Claims.full_name, authInfo.getUser().getName())
                .claim(Claims.sub, authInfo.getUser().getId().toString())
                .expiresIn(60 * 60 * 7L)
                .sign();

        UserResponse userResponse = UserResponse.builder()
                .id(authInfo.getUser().getId())
                .name(authInfo.getUser().getName())
                .photoUrl(authInfo.getUser().getPhotoUrl())
                .email(authInfo.getEmail())
                .build();

        return new UserAuthResponse(userResponse, token);
    }

}
