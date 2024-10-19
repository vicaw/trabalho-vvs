package dev.vicaw.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import dev.vicaw.model.Image;
import dev.vicaw.service.ImageService;

@Path("/")
public class ImageResource {

    @Inject
    ImageService imageService;

    @GET
    @Path("/api/images/{imageName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfoById(@PathParam("imageName") String imageName) {
        return Response.status(Status.OK).entity(imageService.getImageInfoByName(imageName)).build();
    }

    @DELETE
    @Path("/api/images/{imageName}")
    @RolesAllowed({ "EDITOR", "ADMIN" })
    public Response deleteByName(@PathParam("imageName") String imageName) {
        imageService.deleteImageByName(imageName);
        return Response.status(Status.OK).build();
    }

    // Retorna a Imagem pura.
    @Path("images/{fileName}")
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("image/jpg")
    public Response getImagebyName(@PathParam("fileName") String name) {
        Image image = imageService.getImageByName(name);
        return Response.status(Status.OK).entity(image.getData()).build();
    }

    @Path("images/{width}/{height}/{fileName}")
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("image/jpg")
    public Response getImageByNameAndScale(@PathParam("fileName") String name, @PathParam("width") int w,
            @PathParam("height") int h) {
        Image image = imageService.getImageByNameAndScale(name, w, h);
        return Response.status(Status.OK).entity(image.getData()).build();
    }

}
