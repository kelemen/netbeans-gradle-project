package org.netbeans.gradle.project.query;

import java.net.URI;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbOutput;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service=FileOwnerQueryImplementation.class)})
public final class GradleFileOwnerQuery implements FileOwnerQueryImplementation {
    private static final Logger LOGGER = Logger.getLogger(GradleFileOwnerQuery.class.getName());

    public GradleFileOwnerQuery() {
    }

    @Override
    public Project getOwner(URI file) {
        for (Project openedProject: OpenProjects.getDefault().getOpenProjects()) {
            NbGradleProject project = openedProject.getLookup().lookup(NbGradleProject.class);
            if (project == null) {
                continue;
            }

            FileObject fileObject = NbModelUtils.uriToFileObject(file);
            if (fileObject != null) {
                NbGradleModule mainModule = project.getCurrentModel().getMainModule();
                for (NbSourceGroup sourceGroup: mainModule.getSources().values()) {
                    for (FileObject srcRoot: sourceGroup.getFileObjects()) {
                        if (FileUtil.getRelativePath(srcRoot, fileObject) != null) {
                            return project;
                        }
                    }
                }
                NbOutput output = mainModule.getProperties().getOutput();

                FileObject buildDir = FileUtil.toFileObject(output.getBuildDir());
                if (buildDir != null && FileUtil.getRelativePath(buildDir, fileObject) != null) {
                    return project;
                }

                FileObject testBuildDir = FileUtil.toFileObject(output.getTestBuildDir());
                if (testBuildDir != null && FileUtil.getRelativePath(testBuildDir, fileObject) != null) {
                    return project;
                }
            }
        }
        return null;
    }

    @Override
    public Project getOwner(FileObject file) {
        return getOwner(file.toURI());
    }
}
