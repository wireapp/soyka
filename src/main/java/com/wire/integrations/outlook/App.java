package com.wire.integrations.outlook;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.integrations.outlook.models.Config;
import com.wire.integrations.outlook.resources.AuthorizeResource;
import com.wire.integrations.outlook.resources.EventResource;
import com.wire.integrations.outlook.resources.OAuth2Callback;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.client.Client;
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
        bootstrap.addBundle(new SwaggerBundle<Config>() {
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Config configuration) {
                return configuration.swagger;
            }
        });
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

        Jdbi jdbi = new JdbiFactory().build(environment, config.database, "soyka");

        Flyway flyway = Flyway
                .configure()
                .dataSource(config.database.build(metrics, "soyka"))
                .load();
        flyway.migrate();

        environment.jersey().register(new AuthorizeResource(jdbi));
        environment.jersey().register(new OAuth2Callback(jdbi, client));
        environment.jersey().register(new EventResource(jdbi, client));
    }
}