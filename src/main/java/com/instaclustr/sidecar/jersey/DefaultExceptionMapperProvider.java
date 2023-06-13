package com.instaclustr.sidecar.jersey;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapperProvider implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(final Throwable exception) {
        return Response.serverError().entity(exception.getMessage()).build();
    }
}
