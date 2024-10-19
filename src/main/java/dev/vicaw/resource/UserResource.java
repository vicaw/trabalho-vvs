package dev.vicaw.resource;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.UserCreateRequest;
import dev.vicaw.model.request.UserUpdateRequest;
import dev.vicaw.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/users")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    JsonWebToken token;

    @GET
    // @RolesAllowed({ "ADMIN" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        return Response.status(Status.OK).entity(userService.list()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(@MultipartForm MultipartBody body) {
        ObjectMapper mapper = new ObjectMapper();
        UserCreateRequest userCreateRequest = null;

        try {
            userCreateRequest = mapper.readValue(body.getObject(), UserCreateRequest.class);
        } catch (JacksonException e) {
            throw new ApiException(400, "Falha ao mapear objeto.");
        }

        return Response.status(Status.OK).entity(userService.create(body, userCreateRequest)).build();
    }

    @Path("/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("userId") Long userId) {
        return Response.status(Status.OK).entity(userService.getById(userId)).build();
    }

    @Path("/{userId}")
    @PUT
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("userId") Long userId, @MultipartForm MultipartBody body) {

        // Apenas ADMIN pode editar outros usuários além dele.
        boolean isAdmin = token.getGroups().contains("ADMIN");
        if (!isAdmin) {
            if (!token.getSubject().equals(userId.toString())) {
                throw new ApiException(403, "Você não pode editar outros usuários.");
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        UserUpdateRequest userUpdateRequest = null;

        try {
            userUpdateRequest = mapper.readValue(body.getObject(), UserUpdateRequest.class);
        } catch (JacksonException e) {
            throw new ApiException(400, "Falha ao mapear objeto.");
        }

        return Response.status(Status.OK).entity(userService.update(userId, body, userUpdateRequest)).build();
    }

}