package org.netbeans.gradle.project.java.query;

import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.AnnotationProcessingQuery.Result;
import org.netbeans.api.java.queries.AnnotationProcessingQuery.Trigger;
import org.netbeans.spi.java.queries.AnnotationProcessingQueryImplementation;
import org.openide.filesystems.FileObject;

/**
 * AnnotationProcessingQueryImplementation implementation
 */
public final class GradleAnnotationProcessingQuery implements AnnotationProcessingQueryImplementation {
    @Override
    public Result getAnnotationProcessingOptions(FileObject file) {
        return EnableResult.INSTANCE;
    }

    private enum EnableResult implements Result {
        INSTANCE;

        @Override
        public Set<? extends Trigger> annotationProcessingEnabled() {
            return EnumSet.of(Trigger.IN_EDITOR, Trigger.ON_SCAN);
        }

        @Override
        public Iterable<? extends String> annotationProcessorsToRun() {
            return null;
        }

        @Override
        public URL sourceOutputDirectory() {
            return null;
        }

        @Override
        public Map<? extends String, ? extends String> processorOptions() {
            return Collections.emptyMap();
        }

        @Override
        public void addChangeListener(ChangeListener l) {
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
        }
    }

}
