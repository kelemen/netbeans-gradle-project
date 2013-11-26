package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;

public interface NbModelLoader {
    public static final class Result {
        private final NbGradleModel mainModel;
        private final List<NbGradleModel> otherModels;

        public Result(NbGradleModel mainModel, Collection<NbGradleModel> otherModels) {
            if (mainModel == null) throw new NullPointerException("mainModel");

            this.mainModel = mainModel;
            this.otherModels = CollectionUtils.copyNullSafeList(otherModels);
        }

        public NbGradleModel getMainModel() {
            return mainModel;
        }

        public List<NbGradleModel> getOtherModels() {
            return otherModels;
        }
    }

    public Result loadModels(NbGradleProject project, ProjectConnection connection, ProgressHandle progress) throws IOException;
}
