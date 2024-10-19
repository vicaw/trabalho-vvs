package dev.vicaw.repository;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import dev.vicaw.model.Image;
import dev.vicaw.model.response.ImageInfoResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class ImageRepository implements PanacheRepository<Image> {
    public Optional<Image> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public List<ImageInfoResponse> listImageInfo() {
        return findAll().project(ImageInfoResponse.class).list();
    }

    public Optional<ImageInfoResponse> findImageInfoByName(String name) {
        return find("name", name).project(ImageInfoResponse.class).firstResultOptional();
    }

}