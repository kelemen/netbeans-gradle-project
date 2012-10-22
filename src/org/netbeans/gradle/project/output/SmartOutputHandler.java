package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.util.List;
import org.openide.windows.OutputWriter;

public final class SmartOutputHandler implements LineOutputWriter.Handler {
    public static interface Visitor {
        public void visitLine(String line);
    }

    public static interface Consumer {
        public boolean tryConsumeLine(String line, OutputWriter output) throws IOException;
    }

    private final OutputWriter output;
    private final Visitor[] visitors;
    private final Consumer[] processors;

    public SmartOutputHandler(OutputWriter output, List<Visitor> visitors, List<Consumer> processors) {
        if (output == null) throw new NullPointerException("output");
        if (visitors == null) throw new NullPointerException("visitors");
        if (processors == null) throw new NullPointerException("processors");

        this.output = output;
        this.visitors = visitors.toArray(new Visitor[0]);
        this.processors = processors.toArray(new Consumer[0]);

        for (Visitor visitor: this.visitors) {
            if (visitor == null) throw new NullPointerException("visitor");
        }
        for (Consumer processor: this.processors) {
            if (processor == null) throw new NullPointerException("processor");
        }
    }

    @Override
    public void writeLine(String line) throws IOException {
        for (Visitor visitor: visitors) {
            visitor.visitLine(line);
        }

        for (Consumer processor: processors) {
            if (processor.tryConsumeLine(line, output)) {
                return;
            }
        }

        output.println(line);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }
}
