package com.instaclustr.sidecar.operations;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Collections2;
import com.instaclustr.operations.Operation;
import com.instaclustr.operations.OperationRequest;
import com.instaclustr.operations.OperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public OperationsResource(final OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GET
    public Collection<Operation> getOperations(@QueryParam("type") final Set<Class<? extends Operation>> operationTypesFilter,
                                               @QueryParam("state") final Set<Operation.State> statesFilter) {

        final List<Operation> operations = new LinkedList<>(operationsService.operations().values());

        // from latest to oldest, the latest at top
        Collections.reverse(operations);

        Collection<Operation> collection = new ArrayList<>(operations);

        if (!operationTypesFilter.isEmpty()) {
            collection = Collections2.filter(collection, input -> {
                if (input == null) {
                    return false;
                }

                return operationTypesFilter.contains(input.getClass());
            });
        }

        if (!statesFilter.isEmpty()) {
            collection = Collections2.filter(collection, input -> {
                if (input == null) {
                    return false;
                }

                return statesFilter.contains(input.state);
            });
        }

        return collection;
    }

    @GET
    @Path("{id}")
    public Operation getOperationById(@NotNull @PathParam("id") final UUID id) {
        return operationsService.operation(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response createNewOperation(@Valid final OperationRequest request) {

        logger.info("Received operation " + request.toString());

        final Operation operation = operationsService.submitOperationRequest(request);

        final URI operationLocation = UriBuilder.fromResource(OperationsResource.class)
            .path(OperationsResource.class, "getOperationById")
            .build(operation.id);

        return Response.created(operationLocation).entity(operation).build();
    }

    @DELETE
    @Path("{id}")
    public Response cancelOperation(@NotNull @PathParam("id") final UUID id) {
        final Optional<Operation> operation = operationsService.operation(id);

        if (operation.isPresent()) {
            operationsService.closeOperation(operation.get().id);
            return Response.accepted().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
