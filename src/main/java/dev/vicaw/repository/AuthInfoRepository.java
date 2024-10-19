package dev.vicaw.repository;

import java.util.Optional;

import dev.vicaw.model.AuthInfo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthInfoRepository implements PanacheRepository<AuthInfo> {
    public Optional<AuthInfo> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public AuthInfo findByUserId(Long userId) {
        return find("user.id", userId).firstResult();
    }
}
