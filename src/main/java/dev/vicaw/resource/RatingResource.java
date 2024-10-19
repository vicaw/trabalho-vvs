package dev.vicaw.resource;

import dev.vicaw.model.request.RatingCreateRequest;
import dev.vicaw.service.RatingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/ratings")
public class RatingResource {

    @Inject
    RatingService ratingService;

    @Path("/{recipeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecipeRating(
            @PathParam("recipeId") Long recipeId,
            @DefaultValue("10") @QueryParam("pagesize") int pagesize,
            @DefaultValue("0") @QueryParam("page") int page,
            @DefaultValue("recent") @QueryParam("orderBy") String orderBy) {
        return Response.status(Status.OK).entity(ratingService.listRecipeRatings(recipeId, pagesize, page, orderBy))
                .build();
    }

    @Path("/{recipeId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postRecipeRating(@PathParam("recipeId") Long recipeId,
            @Valid RatingCreateRequest ratingCreateRequest) {
        return Response.status(Status.OK).entity(ratingService.postRating(recipeId, ratingCreateRequest)).build();
    }

    @Path("/{recipeId}/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response recipeRatingInfo(@PathParam("recipeId") Long recipeId) {
        return Response.status(Status.OK).entity(ratingService.getRatingInfo(recipeId)).build();
    }

    @Path("/{recipeId}/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response userRating(@PathParam("recipeId") Long recipeId, @PathParam("userId") Long userId) {
        return Response.status(Status.OK).entity(ratingService.getUserRating(recipeId, userId)).build();
    }

}
