package com.instaclustr.sidecar.jersey;

import com.instaclustr.sidecar.jersey.OperationTypeIdParamConverterProvider.InvalidTypeIdException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidTypeIdExceptionMapperProvider implements ExceptionMapper<InvalidTypeIdException> {

    @Override
    public Response toResponse(final InvalidTypeIdException exception) {
        return exception.getResponse();
    }
}
