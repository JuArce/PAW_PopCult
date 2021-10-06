package ar.edu.itba.paw.services;

import ar.edu.itba.paw.interfaces.ImageDao;
import ar.edu.itba.paw.interfaces.ImageService;
import ar.edu.itba.paw.models.image.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    ImageDao imageDao;

    @Transactional(readOnly = true)
    @Override
    public Optional<Image> getImage(int imageId) {
        return imageDao.getImage(imageId);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Image> getImage(String imagePath) {
        try {
            final byte[] imageBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(this.getClass().getClassLoader().getResource(imagePath)).toURI()));
            final Image image = new Image(0, imageBytes);
            return Optional.of(image);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Error loading locale image");
        }
    }

    @Transactional
    @Override
    public Optional<Image> uploadImage(byte[] photoBlob) {
        return imageDao.uploadImage(photoBlob);
    }
}
