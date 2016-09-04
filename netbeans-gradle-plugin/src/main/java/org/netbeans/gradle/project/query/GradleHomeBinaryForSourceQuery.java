package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class GradleHomeBinaryForSourceQuery extends AbstractBinaryForSourceQuery {
    public GradleHomeBinaryForSourceQuery() {
    }

    @Override
    protected Result tryFindBinaryRoots(File sourceRoot) {
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
        if (sourceRootObj == null) {
            return null;
        }

        FileObject gradleHomeObj = CommonGlobalSettings.getDefault().tryGetGradleInstallation();
        if (gradleHomeObj == null) {
            return null;
        }

        File gradleHome = FileUtil.toFile(gradleHomeObj);
        if (gradleHome == null) {
            return null;
        }

        FileObject gradleSrc = GradleFileUtils.getSrcDirOfGradle(gradleHomeObj);
        if (gradleSrc == null) {
            return null;
        }

        if (!FileUtil.isParentOf(gradleSrc, sourceRootObj)) {
            return null;
        }

        final URL[] gradleLibs = GradleHomeClassPathProvider.getGradleBinaries(gradleHomeObj);

        return new Result() {
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
    }
}
