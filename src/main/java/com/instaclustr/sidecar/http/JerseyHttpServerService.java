package com.instaclustr.sidecar.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.jdkhttp.JdkHttpHandlerContainer;
import org.glassfish.jersey.jdkhttp.JdkHttpHandlerContainerProvider;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guava Service that manages a HttpServer for a Jersey application
 */
public class JerseyHttpServerService extends AbstractIdleService {

    private static final Logger logger = LoggerFactory.getLogger(JerseyHttpServerService.class);

    private final InetSocketAddress httpServerAddress;
    private final ResourceConfig resourceConfig;

    private JdkHttpHandlerContainer container;
    private HttpServer httpServer;

    public JerseyHttpServerService(final InetSocketAddress httpServerAddress, final ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        this.httpServerAddress = httpServerAddress;
    }

    public InetSocketAddress getServerInetAddress() {
        return httpServerAddress;
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    @Override
    protected void startUp() throws Exception {

        container = new JdkHttpHandlerContainerProvider().createContainer(JdkHttpHandlerContainer.class, resourceConfig);

        try {
            logger.info("Starting HTTP server on address " + httpServerAddress);
            httpServer = HttpServer.create(httpServerAddress, 0);


        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        httpServer.setExecutor(Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                                 .setNameFormat("jdk-http-server-%d")
                                                                 .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                                                                 .build()));

        httpServer.createContext("/", container);

        httpServer.start();
        container.getApplicationHandler().onStartup(container);

        if (logger.isInfoEnabled()) {
            final InetSocketAddress socketAddress = httpServer.getAddress();
            final URI serverUrl = new URI("http",
                                          null,
                                          InetAddresses.toUriString(socketAddress.getAddress()), socketAddress.getPort(),
                                          "/",
                                          null,
                                          null);

            logger.info("Started HTTP server on {}", serverUrl);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        container.getApplicationHandler().onShutdown(container);
        httpServer.stop(0);
    }

    public void shutdown() throws Exception {
        shutDown();
    }
}
