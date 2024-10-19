package dev.vicaw.resource;

import dev.vicaw.model.request.UserAuthRequest;
import dev.vicaw.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/auth")
public class AuthResource {

    @Inject
    AuthService authService;

    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@Valid UserAuthRequest userAuthRequest) {
        return Response.status(Status.OK)
                .entity(authService.authenticate(userAuthRequest))
                .build();
    }
}
