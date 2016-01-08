package org.netbeans.gradle.project.output;

import java.io.Closeable;
import java.io.IOException;

public interface IOTabRef<IOTab> extends Closeable {
    public IOTab getTab();

    @Override
    public void close() throws IOException;
}
