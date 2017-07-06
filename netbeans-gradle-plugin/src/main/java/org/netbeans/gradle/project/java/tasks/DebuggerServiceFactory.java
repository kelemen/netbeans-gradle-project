package org.netbeans.gradle.project.java.tasks;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.Transport;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.MonitorableTaskExecutorService;
import org.netbeans.api.debugger.jpda.DebuggerStartException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.gradle.project.api.task.GradleCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandService;
import org.netbeans.gradle.project.api.task.GradleCommandServiceFactory;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.windows.OutputWriter;

public final class DebuggerServiceFactory implements GradleCommandServiceFactory {
    private static final Logger LOGGER = Logger.getLogger(DebuggerServiceFactory.class.getName());

    private static final MonitorableTaskExecutorService DEBUGGER_ATTACH_LISTENER
            = NbTaskExecutors.newExecutor("Debugger attach listener", Integer.MAX_VALUE, 5000);

    public static final TaskVariable JPDA_PORT_VAR = new TaskVariable("jpda.port");

    private static final String TRANSPORT_NAME = "dt_socket";

    private final JavaExtension javaExt;

    public DebuggerServiceFactory(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
    }

    @Override
    public boolean isServiceTaskVariable(TaskVariable variable) {
        return JPDA_PORT_VAR.equals(variable);
    }

    private static ListeningConnector findConnector(String transportName) {
        for (ListeningConnector connector: Bootstrap.virtualMachineManager().listeningConnectors()) {
            Transport transport = connector.transport();
            if (transport != null && transport.name().equals(transportName)) {
                return connector;
            }
        }
        throw new RuntimeException("No transports named " + transportName + " found!");
    }

    private static void stopListening(ListeningConnector connector, Map<String, Connector.Argument> defaultArgs) throws IOException {
        try {
            connector.stopListening(defaultArgs);
        } catch (IllegalConnectorArgumentsException ex) {
            // IllegalConnectorArgumentsException in the case the debugee
            // already connected.
            LOGGER.log(Level.FINE, "Failed to stop listening", ex);
        }
    }

    private void startListening(
            CancellationToken cancelToken,
            final ListeningConnector connector,
            final Map<String, Connector.Argument> defaultArgs) {
        final Map<String, Object> services = AttacherListener.getJpdaServiceObjects(javaExt);

        DEBUGGER_ATTACH_LISTENER.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            Thread currentThread = Thread.currentThread();
            ListenerRef cancelRef = taskCancelToken.addCancellationListener(currentThread::interrupt);
            try {
                JPDADebugger.startListening(connector, defaultArgs, new Object[]{services});
                LOGGER.log(Level.INFO, "JPDADebugger.startListening has successfully connected to the debugee.");
            } catch (DebuggerStartException ex) {
                LOGGER.log(Level.INFO, "JPDADebugger.startListening failed.", ex);
            } finally {
                cancelRef.unregister();
            }
        }).exceptionally(AsyncTasks::expectNoError);
    }

    @Override
    public GradleCommandService startService(CancellationToken cancelToken, GradleCommandContext context) throws IOException {
        OutputWriter output = context.getOutputTab().getOut();

        final ListeningConnector connector = findConnector(TRANSPORT_NAME);

        final Map<String, Connector.Argument> defaultArgs = connector.defaultArguments();
        String address;
        try {
            address = connector.startListening(defaultArgs);
            output.println("JPDA Address: " + address);
        } catch (IllegalConnectorArgumentsException ex) {
            throw new IOException(ex);
        }

        String portStr = extractPort(address);
        output.println("Port: " + portStr);
        final TaskVariableMap varMap = singleVar(JPDA_PORT_VAR, portStr);

        startListening(cancelToken, connector, defaultArgs);

        // FIXME: Stop listening even if this method fails with an error.

        return new GradleCommandService() {
            @Override
            public TaskVariableMap getTaskVariables() {
                return varMap;
            }

            @Override
            public void close() throws IOException {
                // We are closing the listing process just in case the
                // debugee fails to connect.
                stopListening(connector, defaultArgs);
            }
        };
    }

    private static String extractPort(String addr) {
        int sepIndex = addr.lastIndexOf(':');
        if (sepIndex < 0) {
            return addr;
        }

        return addr.substring(sepIndex + 1);
    }

    private static TaskVariableMap singleVar(TaskVariable mapVar, String mapValue) {
        return Collections.singletonMap(mapVar, mapValue)::get;
    }
}
