package org.netbeans.gradle.project.output;

import java.io.IOException;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class BuildErrorConsumer implements SmartOutputHandler.Consumer {
    private static final String EXCEPTION_CAPTION = "* Exception is:";

    private volatile boolean consume;

    public BuildErrorConsumer() {
        this.consume = false;
    }

    @Override
    public boolean tryConsumeLine(String line, InputOutput ioParent, OutputWriter output) throws IOException {
        if (consume) {
            return true;
        }

        if (EXCEPTION_CAPTION.equalsIgnoreCase(line.trim())) {
            consume = true;
            return true;
        }
        return false;
    }
}
