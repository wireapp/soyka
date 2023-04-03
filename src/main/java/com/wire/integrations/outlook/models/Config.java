package com.wire.integrations.outlook.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config extends Configuration {
    public JerseyClientConfiguration jersey;

    @Valid
    @JsonProperty
    public SwaggerBundleConfiguration swagger;

    @JsonProperty
    public UUID clientId;

    @JsonProperty
    public String wireServer;

    @JsonProperty
    public String wireWeb;

    @JsonProperty
    public String callback;

    @JsonProperty
    public String domain;

}