package com.wire.integrations.outlook;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.wire.integrations.outlook.models.Config;
import com.wire.integrations.outlook.resources.AuthorizeResource;
import com.wire.integrations.outlook.resources.EventResource;
import com.wire.integrations.outlook.resources.OAuth2Callback;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.ws.rs.client.Client;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import java.util.EnumSet;

public class App extends Application<Config> {
    public static MetricRegistry metrics;
    public static Config config;

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    public void run(Config configuration, Environment environment) {
        App.config = configuration;
        App.metrics = environment.metrics();

        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", String.format("%s,%s,localhost", config.domain, config.wireWeb));
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        Client client = new JerseyClientBuilder(environment)
                .using(config.jersey)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());

        environment.jersey().register(new AuthorizeResource());
        environment.jersey().register(new OAuth2Callback(client));
        environment.jersey().register(new EventResource(client));
    }
}