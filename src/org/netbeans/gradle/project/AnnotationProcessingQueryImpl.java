package org.netbeans.gradle.project;

import java.net.URL;
import java.util.Arrays;
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
 *
 * @author César Izurieta
 */
class AnnotationProcessingQueryImpl implements AnnotationProcessingQueryImplementation {

    private final URL sourceOutputDirectory;

    AnnotationProcessingQueryImpl(NbGradleProject proj) {
        // FIXME: find the right directory
        sourceOutputDirectory = null;
    }

    @Override
    public Result getAnnotationProcessingOptions(FileObject file) {
        return new Result() {

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
                return sourceOutputDirectory;
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
        };
    }

}
