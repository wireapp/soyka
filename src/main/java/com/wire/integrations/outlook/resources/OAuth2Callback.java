package com.wire.integrations.outlook.resources;

import com.wire.integrations.outlook.*;
import com.wire.integrations.outlook.models.Token;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.io.InputStream;

import static com.wire.integrations.outlook.Helpers.ZCALENDAR_SID;

@Path("/oauth2callback")
public class OAuth2Callback {

    private final SessionsDAO sessionsDAO;
    private final Client client;

    public OAuth2Callback(Jdbi jdbi, Client client) {
        sessionsDAO = jdbi.onDemand(SessionsDAO.class);
        this.client = client;
    }

    @GET
    public Response callback(@QueryParam("code") String code,
                             @QueryParam("state") String stateId) {

        String verifier = sessionsDAO.getState(stateId);

        Form form = new Form();
        form.param("client_id", App.config.clientId.toString());
        form.param("code", code);
        form.param("grant_type", "authorization_code");
        form.param("redirect_uri", App.config.callback);
        form.param("code_verifier", verifier);

        Response post = client.target(App.config.wireServer)
                .path("oauth/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));

        if (post.getStatus() >= 400) {
            String error = post.readEntity(String.class);
            Logger.warning("OAuth2Callback: %s %d", error, post.getStatus());
            return Response
                    .status(400)
                    .entity(error)
                    .build();
        }

        Token token = post.readEntity(Token.class);

        String sessionId = Helpers.randomName(32);
        sessionsDAO.upsert(sessionId, token.token, token.refresh);


        String cookie = String.format("%s=%s;Version=1;Domain=outlook.integrations.zinfra.io;Secure;HttpOnly;SameSite=None",
                ZCALENDAR_SID,
                sessionId);
        return Response
                .ok(getResource("success-authorized.html"))
                .header("Set-Cookie", cookie)
                .build();
    }

    public static InputStream getResource(String name) {
        ClassLoader classLoader = OAuth2Callback.class.getClassLoader();
        return classLoader.getResourceAsStream(name);
    }
}