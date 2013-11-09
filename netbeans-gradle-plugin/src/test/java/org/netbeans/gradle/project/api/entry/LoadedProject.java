package org.netbeans.gradle.project.api.entry;

import java.io.Closeable;
import org.netbeans.gradle.project.NbGradleProject;

public interface LoadedProject extends Closeable {
    public NbGradleProject getProject();
}
