package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = FileEncodingQueryImplementation.class)})
public final class GradleHomeSourceEncodingQuery extends FileEncodingQueryImplementation {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Charset getEncoding(FileObject file) {
        FileObject gradleHome = GlobalGradleSettings.getGradleLocation();
        if (gradleHome == null) {
            return null;
        }

        FileObject srcDir = GradleFileUtils.getSrcDirOfGradle(gradleHome);
        if (srcDir == null) {
            return null;
        }

        return FileUtil.isParentOf(srcDir, file) ? UTF8 : null;
    }
}
