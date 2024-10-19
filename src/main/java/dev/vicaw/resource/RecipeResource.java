package dev.vicaw.resource;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vicaw.exception.ApiException;
import dev.vicaw.model.request.MultipartBody;
import dev.vicaw.model.request.RecipeCreateRequest;
import dev.vicaw.model.request.RecipeUpdateRequest;
import dev.vicaw.service.RecipeService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/recipes")
public class RecipeResource {

    @Inject
    RecipeService recipeService;

    @Inject
    JsonWebToken token;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReceitas(@QueryParam("authorId") Long authorId,
            @QueryParam("pagesize") Integer pagesize,
            @QueryParam("page") Integer page,
            @DefaultValue("recent") @QueryParam("orderBy") String orderBy) {
        return Response.status(Status.OK).entity(recipeService.list(authorId, pagesize, page, orderBy)).build();
    }

    @POST
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response criarReceita(@MultipartForm MultipartBody body) {
        ObjectMapper mapper = new ObjectMapper();
        RecipeCreateRequest recipeCreateRequest = null;

        try {
            recipeCreateRequest = mapper.readValue(body.getObject(), RecipeCreateRequest.class);
        } catch (JacksonException e) {
            throw new ApiException(400, "Falha ao mapear objeto.");
        }

        return Response.status(Status.OK).entity(recipeService.create(body, recipeCreateRequest)).build();
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByIdReceita(@PathParam("id") long id) {
        return Response.status(Status.OK).entity(recipeService.getById(id)).build();
    }

    @Path("/{id}")
    @PUT
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateReceita(@PathParam("id") long id, @MultipartForm MultipartBody body) {
        ObjectMapper mapper = new ObjectMapper();
        RecipeUpdateRequest recipeUpdateRequest = null;

        try {
            recipeUpdateRequest = mapper.readValue(body.getObject(), RecipeUpdateRequest.class);
        } catch (JacksonException e) {
            throw new ApiException(400, "Falha ao mapear objeto.");
        }

        return Response.status(Status.OK).entity(recipeService.update(id, body, recipeUpdateRequest)).build();
    }

    @Path("/{id}")
    @DELETE
    @Authenticated
    public Response deleteByIdReceita(@PathParam("id") long id) {
        recipeService.delete(id);
        return Response.status(Status.OK).build();
    }

    @Path("/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchArticle(
            @QueryParam("q") @NotNull(message = "O termo de busca não foi informado.") @Size(min = 3, message = "O termo de busca deve ser maior que 3 caracteres") String query,
            @DefaultValue("10") @QueryParam("pagesize") int pagesize,
            @DefaultValue("0") @QueryParam("page") int page,
            @DefaultValue("rating") @QueryParam("orderBy") String orderBy) {

        return Response.status(Status.OK).entity(recipeService.searchRecipe(query, pagesize, page, orderBy)).build();
    }
}