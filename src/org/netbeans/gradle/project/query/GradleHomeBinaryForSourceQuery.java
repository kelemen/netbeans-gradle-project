package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private static final URL[] NO_URLS = new URL[0];

    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<File, Result> cache;

    public GradleHomeBinaryForSourceQuery() {
        this.cache = new ConcurrentHashMap<File, Result>();
    }

    private static URL[] getGradleBinaries(FileObject gradleHomeObj, FilenameFilter filter) {
        File gradleHome = FileUtil.toFile(gradleHomeObj);
        if (gradleHome == null) {
            return NO_URLS;
        }

        if (!gradleHome.isDirectory()) {
            return NO_URLS;
        }

        File libDir = new File(gradleHome, "lib");
        if (!libDir.isDirectory()) {
            return NO_URLS;
        }

        File[] jars = libDir.listFiles(filter);
        List<URL> result = new ArrayList<URL>(jars.length);
        for (File jar: jars) {
            URL url = FileUtil.urlForArchiveOrDir(jar);
            if (url != null) {
                result.add(url);
            }
        }

        return result.toArray(NO_URLS);
    }

    private static URL[] getGradleBinaries(FileObject gradleHomeObj) {
        return getGradleBinaries(gradleHomeObj, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase(Locale.US);
                return lowerCaseName.startsWith("gradle-") && lowerCaseName.endsWith(".jar");
            }
        });
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

        String gradleHomeStr = GradleOptionsPanelController.getGradleHome();
        if (gradleHomeStr.isEmpty()) {
            return null;
        }

        // We assume that the gradle home directory looks like this:
        //
        // binaries: GRADLE_HOME\\lib\\*.jar
        // sources for all binaries: GRADLE_HOME\\src

        File gradleHome = new File(gradleHomeStr);
        FileObject gradleHomeObj = FileUtil.toFileObject(gradleHome);
        if (gradleHomeObj == null) {
            return null;
        }

        FileObject gradleSrc = gradleHomeObj.getFileObject("src");
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

        final URL[] gradleLibs = getGradleBinaries(gradleHomeObj);

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
