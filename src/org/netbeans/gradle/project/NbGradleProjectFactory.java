package org.netbeans.gradle.project;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;

@org.openide.util.lookup.ServiceProvider(service = ProjectFactory.class)
public class NbGradleProjectFactory implements ProjectFactory {
    @Override
    public boolean isProject(FileObject projectDirectory) {
        return projectDirectory.getFileObject(GradleProjectConstants.BUILD_FILE_NAME) != null;
    }

    @Override
    public Project loadProject(FileObject dir, ProjectState state) throws IOException {
        // Note: Netbeans might call this method without calling isProject
        //  first on directories within the project. If this method throws
        //  an exception in this case, NetBeans will fail to check for the class
        //  path of files. And finding the cause of such behaviour is extremly
        //  hard if this is not known.
        if (!isProject(dir)) {
            return null;
        }
        return new NbGradleProject(dir, state);
    }

    @Override
    public void saveProject(final Project project) throws IOException {
    }
}