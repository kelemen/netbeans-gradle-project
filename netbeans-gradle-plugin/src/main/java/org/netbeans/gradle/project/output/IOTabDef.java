package org.netbeans.gradle.project.output;

import java.io.IOException;

public interface IOTabDef {
    public boolean isDestroyed();
    public void close() throws IOException;
}
