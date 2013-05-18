package org.netbeans.gradle.project.java.query;

import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleSourceLevelQueryImplementation
implements
        SourceLevelQueryImplementation2,
        ProjectInitListener {

    private final JavaExtension javaExt;
    private final ChangeSupport changes;

    public GradleSourceLevelQueryImplementation(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");

        this.javaExt = javaExt;

        EventSource eventSource = new EventSource();
        this.changes = new ChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    @Override
    public void onInitProject() {
        javaExt.getProjectLookup().lookup(GradleProperty.SourceLevel.class).addChangeListener(new Runnable() {
            @Override
            public void run() {
                changes.fireChange();
            }
        });
    }

    @Override
    public Result getSourceLevel(FileObject javaFile) {
        // Assume that every source file must reside in the project directory.
        if (FileUtil.getRelativePath(javaExt.getProjectDirectory(), javaFile) == null) {
            return null;
        }

        final GradleProperty.SourceLevel sourceLevel
                = javaExt.getProjectLookup().lookup(GradleProperty.SourceLevel.class);

        return new Result() {
            @Override
            public String getSourceLevel() {
                return sourceLevel.getValue();
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                changes.addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                changes.addChangeListener(listener);
            }
        };
    }

    private static final class EventSource implements Result {
        private volatile ChangeSupport changes;

        public void init(ChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public String getSourceLevel() {
            return AbstractProjectProperties.DEFAULT_SOURCE_LEVEL;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            changes.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            changes.removeChangeListener(l);
        }
    }
}
