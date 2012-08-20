package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.gradle.project.properties.GradleOptionsPanelController;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class GradleHomeBinaryForSourceQuery implements BinaryForSourceQueryImplementation {
    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<File, Result> cache;

    public GradleHomeBinaryForSourceQuery() {
        this.cache = new ConcurrentHashMap<File, Result>();
    }

    @Override
    public Result findBinaryRoots(URL sourceRoot) {
        if (GradleFileUtils.GRADLE_CACHE_HOME == null) {
            return null;
        }

        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }

        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRootFile);
        if (sourceRootObj == null) {
            return null;
        }

        File gradleHome = GradleOptionsPanelController.getGradleHome();
        if (gradleHome == null) {
            return null;
        }

        FileObject gradleHomeObj = FileUtil.toFileObject(gradleHome);
        if (gradleHomeObj == null) {
            return null;
        }

        FileObject gradleSrc = GradleFileUtils.getSrcDirOfGradle(gradleHomeObj);
        if (gradleSrc == null) {
            return null;
        }

        if (!FileUtil.isParentOf(gradleSrc, sourceRootObj)) {
            return null;
        }

        Result result = cache.get(gradleHome);
        if (result != null) {
            return result;
        }

        final URL[] gradleLibs = GradleHomeClassPathProvider.getGradleBinaries(gradleHomeObj);

        result = new Result() {
            @Override
            public URL[] getRoots() {
                return gradleLibs.clone();
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
}
