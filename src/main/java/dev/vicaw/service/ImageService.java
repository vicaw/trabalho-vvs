package dev.vicaw.service;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.Image;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.response.ImageInfoResponse;
import dev.vicaw.repository.ImageRepository;
import dev.vicaw.service.ImageService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class ImageService {
    private static final List<String> EXTENSIONS_PERMITIDAS = Arrays.asList("jpg", "jpeg", "png");

    @Inject
    ImageRepository imageRepository;

    @ConfigProperty(name = "baseurl", defaultValue = "")
    String baseurl;

    @Transactional
    public String save(MultipartBody body) {
        try {
            String fileName = body.imageName;
            // long fileSize = body.image.available();

            int indiceUltimoPonto = fileName.lastIndexOf('.');
            if (indiceUltimoPonto > 0 && indiceUltimoPonto < fileName.length() - 1) {
                String extensao = fileName.substring(indiceUltimoPonto + 1).toLowerCase();
                if (!EXTENSIONS_PERMITIDAS.contains(extensao.toLowerCase())) {
                    throw new ApiException(400, "Formato da imagem enviada não suportado.");
                }
            }

            Image image = new Image();

            image.setData(body.image.readAllBytes());
            image.setName(UUID.randomUUID().toString() + "-" + body.imageName);
            imageRepository.persist(image);

            String imageUrl = baseurl + "/images/" + image.getName();
            return imageUrl;

        } catch (IOException e) {
            throw new ApiException(500, "I/O Exception Error");
        } catch (OutOfMemoryError e) {
            throw new ApiException(500, "Out of Memory Error");
        }

    }

    @Transactional
    public void deleteImageByName(String name) {
        Optional<Image> imageResult = imageRepository.findByName(name);
        if (imageResult.isEmpty())
            throw new ApiException(404, "Não existe nenhuma imagem com o Nome informado.");

        imageRepository.delete(imageResult.get());
    }

    public ImageInfoResponse getImageInfoByName(String imageName) {
        Optional<ImageInfoResponse> image = imageRepository.findImageInfoByName(imageName);
        if (image.isEmpty())
            throw new ApiException(404, "Não existe nenhuma imagem com o Nome informado.");

        return image.get();
    }

    public Image getImageByName(String fileName) {
        Optional<Image> imageResult = imageRepository.findByName(fileName);

        if (imageResult.isEmpty())
            return Image.defaultImage();

        return imageResult.get();
    }

    public Image getImageByNameAndScale(String name, int w, int h) {
        Image image = getImageByName(name);
        image.scale(w, h);
        return image;
    }

}
