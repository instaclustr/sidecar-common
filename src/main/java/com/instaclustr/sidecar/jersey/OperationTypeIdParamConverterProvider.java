package com.instaclustr.sidecar.jersey;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import com.google.inject.TypeLiteral;
import com.instaclustr.operations.Operation;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OperationTypeIdParamConverterProvider implements ParamConverterProvider {
    private static final Type PARAMETER_TYPE = new TypeLiteral<Class<? extends Operation>>() {}.getType();

    @Inject
    private Operation.TypeIdResolver typeIdResolver;

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (!genericType.equals(PARAMETER_TYPE)) {
            return null;
        }

        return (ParamConverter<T>) new ParamConverter<Class<? extends Operation>>() {
            @Override
            public Class<? extends Operation> fromString(final String value) {
                if (value == null) {
                    throw new InvalidTypeIdException(null, typeIdResolver.typeIds());
                }

                final Class<? extends Operation> clazz = typeIdResolver.classFromId(value);

                if (clazz == null) {
                    throw new InvalidTypeIdException(value, typeIdResolver.typeIds());
                }

                return clazz;
            }

            @Override
            public String toString(final Class<? extends Operation> value) {
                if (value == null) {
                    throw new IllegalArgumentException();
                }

                return typeIdResolver.idFromValue(value);
            }
        };
    }

    static class InvalidTypeIdException extends WebApplicationException {

        private static Response buildResponse(final String typeId, final Set<String> possibleTypes) {
            // TODO
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        InvalidTypeIdException(final String typeId, final Set<String> possibleTypes) {
            super(InvalidTypeIdException.buildResponse(typeId, possibleTypes));
        }
    }

    @Provider
    static class InvalidTypeIdExceptionMapper implements ExceptionMapper<InvalidTypeIdException> {
        @Override
        public Response toResponse(final InvalidTypeIdException exception) {
            return exception.getResponse();
        }
    }
}
