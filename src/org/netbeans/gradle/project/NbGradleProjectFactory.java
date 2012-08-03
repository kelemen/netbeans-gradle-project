package org.netbeans.gradle.project;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;

@org.openide.util.lookup.ServiceProvider(service = ProjectFactory.class)
public class NbGradleProjectFactory implements ProjectFactory {
    public static final String PROJECT_DIR = "build.gradle";

    //Specifies when a project is a project, i.e.,
    //if the project directory "texts" is present:
    @Override
    public boolean isProject(FileObject projectDirectory) {
        return projectDirectory.getFileObject(PROJECT_DIR) != null;
    }

    //Specifies when the project will be opened, i.e.,
    //if the project exists:
    @Override
    public Project loadProject(FileObject dir, ProjectState state) {
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