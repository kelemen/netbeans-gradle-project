package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.util.SerializationUtils2;

public final class PersistentProjectModelStoreFactory {
    public PersistentProjectModelStoreFactory() {
    }

    public ModelPersister<NbGradleModel> getModelPersister() {
        return ProjectModelPersister.INSANCE;
    }

    public PersistentModelStore<NbGradleModel> createModelStore(NbGradleProject ownerProject) {
        return new PersistentProjectModelStore(ownerProject);
    }

    private enum ProjectModelPersister implements ModelPersister<NbGradleModel> {
        INSANCE;

        @Override
        public void persistModel(NbGradleModel model, Path dest) throws IOException {
            Path destDir = dest.getParent();
            if (destDir != null) {
                Files.createDirectories(destDir);
            }

            SerializedNbGradleModels toSave = SerializedNbGradleModels.createSerialized(model);
            SerializationUtils2.serializeToFile(dest, toSave);
        }
    }

    private static final class PersistentProjectModelStore implements PersistentModelStore<NbGradleModel> {
        private final NbGradleProject ownerProject;

        public PersistentProjectModelStore(NbGradleProject ownerProject) {
            this.ownerProject = Objects.requireNonNull(ownerProject, "ownerProject");
        }

        @Override
        public void persistModel(NbGradleModel model, Path dest) throws IOException {
            ProjectModelPersister.INSANCE.persistModel(model, dest);
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
}
