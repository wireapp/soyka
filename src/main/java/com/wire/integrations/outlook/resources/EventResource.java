package com.wire.integrations.outlook.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.integrations.outlook.*;
import com.wire.integrations.outlook.models.Token;
import io.swagger.annotations.Api;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

import static com.wire.integrations.outlook.Helpers.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/event")
@Consumes(MediaType.APPLICATION_JSON)
@Api
public class EventResource {
    private final Client client;

    public EventResource(Client client) {
        this.client = client;
    }

    @POST
    public Response newEvent(@CookieParam(ZCALENDAR_TOKEN) String accessToken,
                             @CookieParam(ZCALENDAR_REFRESH) String refreshToken,
                             @Valid _Payload payload) {
        try {
            Token token = null;

            if (accessToken == null) {
                return Response
                        .status(401)
                        .build();
            }

            Response response = client.target(App.config.wireServer)
                    .path("v2/self")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .get();

            if (response.getStatus() == 401) {
                Form form = new Form();
                form.param("client_id", App.config.clientId.toString());
                form.param("grant_type", "refresh_token");
                form.param("refresh_token", refreshToken);

                response = client.target(App.config.wireServer)
                        .path("oauth/token")
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.form(form));

                if (response.getStatus() >= 400) {
                    return error(response, "Refresh");
                }

                token = response.readEntity(Token.class);

                accessToken = token.token;

                response = client.target(App.config.wireServer)
                        .path("v2/self")
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .get();
            }

            if (response.getStatus() >= 400) {
                return error(response, "Profile");
            }

            _Profile profile = response.readEntity(_Profile.class);

            NewConversation newConversation = new NewConversation();
            newConversation.name = payload.name;
            newConversation.team.teamId = profile.teamId;

            response = client.target(App.config.wireServer)
                    .path("v2/conversations")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .post(Entity.entity(newConversation, MediaType.APPLICATION_JSON));

            if (response.getStatus() >= 400) {
                return error(response, "New Conversation");
            }

            _Conv conv = response.readEntity(_Conv.class);
            conv.creator = profile.name;

            response = client.target(App.config.wireServer)
                    .path("v2/conversations")
                    .path(conv.id.toString())
                    .path("code")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .post(Entity.json(""));

            if (response.getStatus() >= 400) {
                return error(response, "Link");
            }

            _Link link = response.readEntity(_Link.class);

            conv.link = link.data.uri;

            if (token != null) {
                String accessCookie = String.format("%s=%s;Version=1;Domain=%s;Secure;HttpOnly;SameSite=None",
                        ZCALENDAR_TOKEN,
                        token.token,
                        App.config.domain);
                String refreshCookie = String.format("%s=%s;Version=1;Domain=%s;Secure;HttpOnly;SameSite=None",
                        ZCALENDAR_REFRESH,
                        token.refresh,
                        App.config.domain);
                return Response
                        .ok(conv)
                        .header("Set-Cookie", accessCookie)
                        .header("Set-Cookie", refreshCookie)
                        .build();
            }

            return Response
                    .ok(conv)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response
                    .serverError()
                    .build();
        }
    }

    private static Response error(Response res, String method) {
        String error = res.readEntity(String.class);
        Logger.warning("%s: %s %d", method, error, res.getStatus());
        return Response
                .status(res.getStatus())
                .entity(error)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Profile {
        @JsonProperty("id")
        public UUID userId;
        @JsonProperty("team")
        public UUID teamId;
        @JsonProperty
        public String name;
    }

    public static class NewConversation {
        public String[] access = new String[]{"invite", "code"};
        public String[] access_role_v2 = new String[]{"guest", "non_team_member", "team_member", "service"};
        public String conversation_role = "wire_member";
        public String name;
        public String protocol = "proteus";
        public String[] qualified_users = new String[]{};
        public int receipt_mode = 1;
        public _TeamInfo team = new _TeamInfo();
        public String[] users = new String[]{};
    }

    static class _TeamInfo {
        @JsonProperty("teamid")
        public UUID teamId;

        @JsonProperty
        public boolean managed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Conv {
        @JsonProperty
        public UUID id;

        @JsonProperty
        public String link;

        @JsonProperty
        public String creator;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Link {
        @JsonProperty
        public _Data data;

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class _Data {
            @JsonProperty
            public String uri;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Payload {
        @JsonProperty
        public String name;
    }
}
