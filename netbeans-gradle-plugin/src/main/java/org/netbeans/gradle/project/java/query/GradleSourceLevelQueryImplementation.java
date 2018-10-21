package org.netbeans.gradle.project.java.query;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingPropertySource;
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

    private static final Logger LOGGER = Logger.getLogger(GradleSourceLevelQueryImplementation.class.getName());

    private static final String JAVA_VERSION_PREFIX = "1.";

    private final FileObject projectDir;
    private final Result result;

    public GradleSourceLevelQueryImplementation(JavaExtension javaExt) {
        Objects.requireNonNull(javaExt, "javaExt");

        this.projectDir = javaExt.getProjectDirectory();

        GradleProperty.SourceLevel sourceLevel = javaExt.getOwnerProjectLookup().lookup(GradleProperty.SourceLevel.class);
        this.result = new ResultImpl(sourceLevel);
    }

    public static boolean isModularVersion(String version) {
        return getNumJavaVersion(version) >= 9;
    }

    public static int getNumJavaVersion(String sourceLevel) {
        String noPrefixLevel = sourceLevel.startsWith(JAVA_VERSION_PREFIX)
                ? sourceLevel.substring(JAVA_VERSION_PREFIX.length())
                : sourceLevel;

        int endIndex = noPrefixLevel.indexOf('.');
        if (endIndex < 0) {
            endIndex = noPrefixLevel.length();
        }

        try {
            return Integer.parseInt(noPrefixLevel.substring(0, endIndex));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.INFO, "Unexpected source level: " + sourceLevel, ex);
            return -1;
        }
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
