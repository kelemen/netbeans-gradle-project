package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.netbeans.gradle.project.NbGradleProject;

public interface PersistentModelCache {
    public NbGradleModel tryGetModel(NbGradleProject project, Path rootProjectDir) throws IOException;
    public void saveGradleModels(Collection<NbGradleModel> models) throws IOException;
}
