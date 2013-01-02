package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
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
public final class GradleHomeSourceForBinaryQuery implements SourceForBinaryQueryImplementation2 {
    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<File, Result> cache;

    public GradleHomeSourceForBinaryQuery() {
        this.cache = new ConcurrentHashMap<File, Result>();
    }

    @Override
    public Result findSourceRoots2(URL binaryRoot) {
        if (GradleFileUtils.GRADLE_CACHE_HOME == null) {
            return null;
        }

        File binaryRootFile = FileUtil.archiveOrDirForURL(binaryRoot);
        if (binaryRootFile == null) {
            return null;
        }

        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRootFile);
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

        Result result = cache.get(gradleHome);
        if (result != null) {
            return result;
        }

        final FileObject gradleSrc = GradleFileUtils.getSrcDirOfGradle(gradleHomeObj);
        if (gradleSrc == null) {
            return null;
        }

        result = new Result() {
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

        Result oldResult = cache.putIfAbsent(gradleHome, result);
        return oldResult != null ? oldResult : result;
    }

    @Override
    public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
        return findSourceRoots2(binaryRoot);
    }
}
