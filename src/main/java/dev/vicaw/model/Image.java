package dev.vicaw.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "images")
public class Image {
    @Id
    @Column(length = 16)
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(columnDefinition = "MEDIUMBLOB")
    @Lob
    private byte[] data;

    private String name;

    private Long article_id;

    @CreationTimestamp
    public LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    public static Image defaultImage() {
        try {
            ClassLoader classLoader = Image.class.getClassLoader();
            InputStream is = classLoader.getResourceAsStream("notfound.jpg");
            Image image = new Image(null, is.readAllBytes(), "notfound.jpg", null, null, null);
            return image;
        } catch (IOException e) {
            return null;
        }
    }

    @Transient
    public byte[] scale(int w, int h) {

        if (w == 0 || h == 0)
            return data;

        ByteArrayInputStream in = new ByteArrayInputStream(this.data);

        try {
            BufferedImage img = ImageIO.read(in);

            double targetAspectRatio = (double) w / (double) h;

            double imageWidth = (double) img.getWidth();
            double imageHeight = (double) img.getHeight();

            double currAspectRatio = imageWidth / imageHeight;

            if (currAspectRatio > targetAspectRatio) {
                imageWidth = imageHeight * targetAspectRatio;
            } else if (currAspectRatio < targetAspectRatio) {
                imageHeight = imageWidth / targetAspectRatio;
            }

            BufferedImage croppedImage = img.getSubimage((img.getWidth() - (int) imageWidth) / 2,
                    (img.getHeight() - (int) imageHeight) / 2, (int) imageWidth, (int) imageHeight);

            java.awt.Image scaledImage = croppedImage.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);

            BufferedImage imgBuff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            imgBuff.getGraphics().drawImage(scaledImage, 0, 0, new Color(0, 0, 0), null);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            ImageIO.write(imgBuff, "jpg", buffer);
            this.setData(buffer.toByteArray());
            return this.data;

        } catch (IOException e) {
            return this.data;
        }
    }

}
