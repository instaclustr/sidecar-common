package com.instaclustr.sidecar.operations;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.instaclustr.operations.Operation;
import com.instaclustr.operations.OperationRequest;
import com.instaclustr.operations.OperationsExpirationService;
import com.instaclustr.operations.OperationsModule;
import com.instaclustr.operations.OperationsService;
import com.instaclustr.threading.ExecutorsModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static com.instaclustr.operations.Operation.State.RUNNING;
import static com.instaclustr.operations.OperationBindings.installOperationBindings;
import static org.junit.Assert.assertTrue;


public class OperationsExpirationServiceTest {

    @Inject
    OperationsService operationsService;

    @Inject
    OperationsExpirationService operationsExpirationService;

    private final Operation testingOperation = new TestingOperation(new TestingRequest());

    static class TestingRequest extends OperationRequest {
    }

    static class TestingOperation extends Operation<TestingRequest> {

        @Inject
        TestingOperation(@Assisted final TestingRequest request) {
            super(request);
        }

        @Override
        protected void run0() throws Exception {
            Thread.sleep(5000);
        }
    }

    @Before
    public void setup() {
        final Injector injector = Guice.createInjector(
                new OperationsModule(3),
                new ExecutorsModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        installOperationBindings(binder(),
                                                 "testing",
                                                 TestingRequest.class,
                                                 TestingOperation.class);
                    }
                }
        );

        injector.injectMembers(this);

        operationsExpirationService.startAsync().awaitRunning();
    }

    @Test
    public void testOperationsExpiration() throws InterruptedException {

        operationsService.submitOperation(testingOperation);

        // after two seconds, operation is still running
        Thread.sleep(2000);
        final Optional<Operation<?>> submittedOperation = operationsService.operation(testingOperation.id);
        assertTrue(submittedOperation.isPresent());

        // after another two seconds, service is still running and it was not expired as expiration runs every three seconds
        Thread.sleep(2000);
        assertTrue(operationsService.operation(testingOperation.id).isPresent());
        Assert.assertEquals(submittedOperation.get().state, RUNNING);

        Thread.sleep(5000);
        // after 5 seconds (9 in total from start of the operation submission), operation is expired
        Assert.assertFalse(operationsService.operation(testingOperation.id).isPresent());
    }
}
