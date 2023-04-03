package com.wire.integrations.outlook.resources;

import com.wire.integrations.outlook.*;
import com.wire.integrations.outlook.models.Token;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import static com.wire.integrations.outlook.Helpers.*;

@Path("/oauth2callback")
@Api
public class OAuth2Callback {
    private final Client client;

    public final static ConcurrentHashMap<String, String> challengeMap = new ConcurrentHashMap<>();

    public OAuth2Callback(Client client) {
        this.client = client;
    }

    @GET
    public Response callback(@QueryParam("code") String code,
                             @QueryParam("state") String stateId) {

        String verifier = challengeMap.get(stateId);

        Form form = new Form();
        form.param("client_id", App.config.clientId.toString());
        form.param("code", code);
        form.param("grant_type", "authorization_code");
        form.param("redirect_uri", App.config.callback);
        form.param("code_verifier", verifier);

        try (Response post = client.target(App.config.wireServer)
                .path("oauth/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form))) {

            if (post.getStatus() >= 400) {
                String error = post.readEntity(String.class);
                Logger.warning("OAuth2Callback: %s %d", error, post.getStatus());
                return Response
                        .status(400)
                        .entity(error)
                        .build();
            }

            Token token = post.readEntity(Token.class);

            String accessCookie = String.format("%s=%s;Version=1;Domain=%s;Secure;HttpOnly;SameSite=None",
                    ZCALENDAR_TOKEN,
                    token.token,
                    App.config.domain);
            String refreshCookie = String.format("%s=%s;Version=1;Domain=%s;Secure;HttpOnly;SameSite=None",
                    ZCALENDAR_REFRESH,
                    token.refresh,
                    App.config.domain);
            return Response
                    .ok(getResource("success-authorized.html"))
                    .header("Set-Cookie", accessCookie)
                    .header("Set-Cookie", refreshCookie)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response
                    .serverError()
                    .build();
        }
    }

    public static InputStream getResource(String name) {
        ClassLoader classLoader = OAuth2Callback.class.getClassLoader();
        return classLoader.getResourceAsStream(name);
    }
}