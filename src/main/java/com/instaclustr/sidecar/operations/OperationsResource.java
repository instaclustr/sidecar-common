package com.instaclustr.sidecar.operations;

import com.google.common.collect.Collections2;
import com.instaclustr.operations.Operation;
import com.instaclustr.operations.OperationRequest;
import com.instaclustr.operations.OperationsService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Common operation JAX-RS resource exposing operation endpoints.
 * This resource is automatically used / registered in any Sidecar just by mere presence on the classpath.
 */
@Path("/operations")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class OperationsResource {

    private static final Logger logger = LoggerFactory.getLogger(OperationsResource.class);

    private final OperationsService operationsService;

    @Inject
    @com.google.inject.Inject
    public OperationsResource(final OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GET
    public Collection<Operation<?>> getOperations(@QueryParam("type") final Set<String> operationTypesFilter,
                                                  @QueryParam("state") final Set<String> statesFilter) {
        Collection<Operation<?>> collection = new ArrayList<>(operationsService.operations().values());

        if (!operationTypesFilter.isEmpty()) {
            collection = Collections2.filter(collection, input -> input != null && operationTypesFilter.contains(input.type));
        }

        if (!statesFilter.isEmpty()) {
            collection = Collections2.filter(collection, input -> input != null && statesFilter.contains(input.state.name()));
        }

        List<Operation<?>> operations = new LinkedList<>(collection);
        // from latest to oldest, the latest at top
        Collections.reverse(operations);
        return operations;
    }

    @GET
    @Path("{id}")
    public Operation<?> getOperationById(@NotNull @PathParam("id") final UUID id) {
        return operationsService.operation(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response createNewOperation(@Valid final OperationRequest request) {

        logger.info("Received operation " + request.toString());

        final Operation<?> operation = operationsService.submitOperationRequest(request);

        final URI operationLocation = UriBuilder.fromResource(OperationsResource.class)
            .path(OperationsResource.class, "getOperationById")
            .build(operation.id);

        return Response.created(operationLocation).entity(operation).build();
    }

    @DELETE
    @Path("{id}")
    public Response cancelOperation(@NotNull @PathParam("id") final UUID id) {
        final Optional<Operation<?>> operation = operationsService.operation(id);

        if (operation.isPresent()) {
            operationsService.closeOperation(operation.get().id);
            return Response.accepted().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
