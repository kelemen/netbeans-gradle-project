package org.netbeans.gradle.project.query;

import java.io.File;
import java.nio.charset.Charset;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSourceEncodingQuery extends FileEncodingQueryImplementation {
    private final NbGradleProject project;

    public GradleSourceEncodingQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Charset getEncoding(FileObject file) {
        NbGradleModule mainModule = project.getCurrentModel().getMainModule();
        for (NbSourceGroup srcGroup: mainModule.getSources().values()) {
            for (File srcDir: srcGroup.getPaths()) {
                FileObject srcDirObj = FileUtil.toFileObject(srcDir);
                if (FileUtil.isParentOf(srcDirObj, file)) {
                    return project.getProperties().getSourceEncoding().getValue();
                }
            }
        }
        return null;
    }
}
