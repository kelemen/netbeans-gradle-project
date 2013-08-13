package org.netbeans.gradle.project.query;

import java.io.File;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class)})
public final class GradleHomeSourceForBinaryQuery extends AbstractSourceForBinaryQuery {
    public GradleHomeSourceForBinaryQuery() {
    }

    @Override
    protected Result tryFindSourceRoot(File binaryRoot) {
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
        }

        FileObject gradleHomeObj = GlobalGradleSettings.getGradleLocation();
        if (gradleHomeObj == null) {
            return null;
        }

        File gradleHome = FileUtil.toFile(gradleHomeObj);
        if (gradleHome == null) {
            return null;
        }

        FileObject gradleLibs = GradleFileUtils.getLibDirOfGradle(gradleHomeObj);
        if (gradleLibs == null) {
            return null;
        }

        if (!FileUtil.isParentOf(gradleLibs, binaryRootObj)) {
            return null;
        }

        final FileObject gradleSrc = GradleFileUtils.getSrcDirOfGradle(gradleHomeObj);
        if (gradleSrc == null) {
            return null;
        }

        return new Result() {
            @Override
            public boolean preferSources() {
                return false;
            }

            @Override
            public FileObject[] getRoots() {
                return new FileObject[]{gradleSrc};
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
