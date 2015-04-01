package org.netbeans.gradle.project.java.query;

import java.util.Objects;
import javax.swing.event.ChangeListener;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;

public final class GradleSourceLevelQueryImplementation
implements
        SourceLevelQueryImplementation2 {

    private final FileObject projectDir;
    private final Result result;

    public GradleSourceLevelQueryImplementation(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.projectDir = javaExt.getProjectDirectory();

        GradleProperty.SourceLevel sourceLevel = javaExt.getOwnerProjectLookup().lookup(GradleProperty.SourceLevel.class);
        this.result = new ResultImpl(sourceLevel);
    }

    @Override
    public Result getSourceLevel(FileObject javaFile) {
        Project owner = FileOwnerQuery.getOwner(javaFile);
        if (owner == null) {
            return null;
        }

        return Objects.equals(projectDir, owner.getProjectDirectory())
                ? result
                : null;
    }

    private static final class ResultImpl implements Result {
        private final SwingPropertySource<String, ChangeListener> property;

        @SuppressWarnings("LeakingThisInConstructor")
        public ResultImpl(PropertySource<String> sourceLevel) {
            this.property = NbProperties.toOldProperty(sourceLevel, this);
        }

        @Override
        public String getSourceLevel() {
            return property.getValue();
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            property.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            property.removeChangeListener(l);
        }
    }
}
