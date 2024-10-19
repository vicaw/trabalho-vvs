package dev.vicaw.model.request;

import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;

@Data
public class MultipartBody {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream image;

    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    public String imageName;

    @FormParam("object")
    @PartType(MediaType.TEXT_PLAIN)
    public String object;
}