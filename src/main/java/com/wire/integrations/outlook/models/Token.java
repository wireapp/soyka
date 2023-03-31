package com.wire.integrations.outlook.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
    @JsonProperty("access_token")
    public String token;

    @JsonProperty("refresh_token")
    public String refresh;

    @JsonProperty("token_type")
    public String type;

    @JsonProperty("expires_in")
    public Integer expires;
}
