package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.task.TaskOutputProcessor;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class SmartOutputHandler implements LineOutputWriter.Handler {
    private static final Logger LOGGER = Logger.getLogger(SmartOutputHandler.class.getName());

    public static interface Consumer {
        public boolean tryConsumeLine(String line, InputOutput ioParent, OutputWriter output) throws IOException;
    }

    private final InputOutput ioParent;
    private final OutputWriter output;
    private final TaskOutputProcessor[] visitors;
    private final Consumer[] processors;

    public SmartOutputHandler(
            InputOutput ioParent,
            OutputWriter output,
            List<TaskOutputProcessor> visitors,
            List<Consumer> processors) {

        this.ioParent = Objects.requireNonNull(ioParent, "ioParent");
        this.output = Objects.requireNonNull(output, "output");
        this.visitors = visitors.toArray(new TaskOutputProcessor[0]);
        this.processors = processors.toArray(new Consumer[0]);

        ExceptionHelper.checkNotNullElements(this.visitors, "visitors");
        ExceptionHelper.checkNotNullElements(this.processors, "processors");
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
                if (processor.tryConsumeLine(line, ioParent, output)) {
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

        ExceptionHelper.rethrowCheckedIfNotNull(error, IOException.class);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }
}
