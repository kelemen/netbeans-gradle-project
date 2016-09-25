package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.util.SerializationUtils2;

public final class ProjectModelPersister implements ModelPersister<NbGradleModel> {
    private final NbGradleProject ownerProject;

    public ProjectModelPersister(NbGradleProject ownerProject) {
        ExceptionHelper.checkNotNullArgument(ownerProject, "ownerProject");
        this.ownerProject = ownerProject;
    }

    @Override
    public void persistModel(NbGradleModel model, Path dest) throws IOException {
        Path destDir = dest.getParent();
        if (destDir != null) {
            Files.createDirectories(destDir);
        }

        SerializedNbGradleModels toSave = SerializedNbGradleModels.createSerialized(model);
        SerializationUtils2.serializeToFile(dest, toSave);
    }

    @Override
    public NbGradleModel tryLoadModel(Path src) throws IOException {
        if (!Files.isRegularFile(src)) {
            return null;
        }

        SerializedNbGradleModels serializedModel
                = (SerializedNbGradleModels)SerializationUtils2.deserializeFile(src);
        return serializedModel != null
                ? serializedModel.deserializeModel(ownerProject)
                : null;
    }
}
