package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.api.task.TaskOutputProcessor;
import org.openide.windows.OutputWriter;

public final class SmartOutputHandler implements LineOutputWriter.Handler {
    private static final Logger LOGGER = Logger.getLogger(SmartOutputHandler.class.getName());

    public static interface Consumer {
        public boolean tryConsumeLine(String line, OutputWriter output) throws IOException;
    }

    private final OutputWriter output;
    private final TaskOutputProcessor[] visitors;
    private final Consumer[] processors;

    public SmartOutputHandler(
            OutputWriter output,
            List<TaskOutputProcessor> visitors,
            List<Consumer> processors) {
        if (output == null) throw new NullPointerException("output");
        if (visitors == null) throw new NullPointerException("visitors");
        if (processors == null) throw new NullPointerException("processors");

        this.output = output;
        this.visitors = visitors.toArray(new TaskOutputProcessor[0]);
        this.processors = processors.toArray(new Consumer[0]);

        for (TaskOutputProcessor visitor: this.visitors) {
            if (visitor == null) throw new NullPointerException("visitor");
        }
        for (Consumer processor: this.processors) {
            if (processor == null) throw new NullPointerException("processor");
        }
    }

    @Override
    public void writeLine(String line) throws IOException {
        Throwable error = null;

        for (TaskOutputProcessor visitor: visitors) {
            try {
                visitor.processLine(line);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Unexpected failure while checking a line of the output.", error);
                error = ex;
            }
        }

        for (Consumer processor: processors) {
            try {
                if (processor.tryConsumeLine(line, output)) {
                    return;
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Unexpected failure while analysing a line of the output.", error);
                error = ex;
            }
        }

        try {
            output.println(line);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Unexpected failure while printing a line of the output.", error);
            error = ex;
        }

        if (error != null) {
            if (error instanceof IOException) {
                throw (IOException)error;
            }
            if (error instanceof RuntimeException) {
                throw (RuntimeException)error;
            }
            if (error instanceof Error) {
                throw (Error)error;
            }
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }
}
