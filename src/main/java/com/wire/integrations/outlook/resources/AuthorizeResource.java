package com.wire.integrations.outlook.resources;

import com.wire.integrations.outlook.App;
import com.wire.integrations.outlook.Helpers;
import io.swagger.annotations.Api;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

@Path("/authorize")
@Api
public class AuthorizeResource {
    @GET
    public Response authorize() throws URISyntaxException, NoSuchAlgorithmException {
        final String state = Helpers.randomName(16);
        final String verifier = Helpers.randomName(64);
        final String challenge = Helpers.sha256(verifier.getBytes());

        OAuth2Callback.challengeMap.put(state, verifier);

        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost(App.config.wireWeb)
                .setPath("auth")
                .setParameter("client_id", App.config.clientId.toString())
                .addParameter("response_type", "code")
                .addParameter("code_challenge_method", "S256")
                .addParameter("code_challenge", challenge)
                .addParameter("state", state)
                .addParameter("redirect_uri", App.config.callback)
                .addParameter("scope", "write:conversations write:conversations_code read:self read:feature_configs")
                .setFragment("/authorize")
                .build();

        return Response
                .temporaryRedirect(uri)
                .build();
    }
}
