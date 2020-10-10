package com.instaclustr.sidecar.http;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.io.IOException;
import java.net.InetSocketAddress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.instaclustr.guice.GuiceInjectorHolder;
import com.instaclustr.sidecar.jersey.DefaultExceptionMapperProvider;
import com.instaclustr.sidecar.validation.ValidationConfigurationContextResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.InjectionManagerProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

public class JerseyHttpServerModule extends AbstractModule {

    private InetSocketAddress httpServerAddress;
    private boolean disableCors;

    public JerseyHttpServerModule(final InetSocketAddress httpServerAddress,
                                  final boolean disableCors) {
        this.httpServerAddress = httpServerAddress;
        this.disableCors = disableCors;
    }

    public JerseyHttpServerModule() {
        // for testing
    }

    @ProvidesIntoSet()
    @Singleton
    Service provideHttpServerService(final ResourceConfig resourceConfig) {
        return new JerseyHttpServerService(httpServerAddress, resourceConfig);
    }

    @Provides
    @Singleton
    ResourceConfig provideResourceConfig(final GuiceHK2BridgeFeature guiceHK2BridgeFeature,
                                         final CustomObjectMapperFeature customObjectMapperFeature,
                                         final Injector injector,
                                         final CORSFilter corsFilter) {

        GuiceInjectorHolder.INSTANCE.setInjector(injector);

        ResourceConfig config = new ResourceConfig()
            .packages("com.instaclustr")
            .register(customObjectMapperFeature)
            .register(guiceHK2BridgeFeature)
            .register(DefaultExceptionMapperProvider.class)
            .register(ValidationConfigurationContextResolver.class)
            .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        if (!disableCors) {
            config.register(corsFilter);
        }

        return config;
    }

    static class CORSFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
            responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        }
    }

    /**
     * Implementation of feature where overriding of built-in ObjectMapper is happening.
     * There is auto discovery of JacksonFeature because it is on the class path from jersey-media-json-jackson
     * which provides its default ObjectMapper hence if we want to provide our own, this is the standard way how to do that.
     */
    static class CustomObjectMapperFeature implements Feature {

        private final ObjectMapper objectMapper;

        @Inject
        public CustomObjectMapperFeature(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(new JacksonJaxbJsonProvider(objectMapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
            return true;
        }
    }

    static class GuiceHK2BridgeFeature implements Feature {

        private final Injector injector;

        @Inject
        public GuiceHK2BridgeFeature(final Injector injector) {
            this.injector = injector;
        }

        @Override
        public boolean configure(final FeatureContext context) {

            final ServiceLocator serviceLocator = InjectionManagerProvider.getInjectionManager(context).getInstance(ServiceLocator.class);

            GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);

            serviceLocator.getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

            return true;
        }
    }
}
