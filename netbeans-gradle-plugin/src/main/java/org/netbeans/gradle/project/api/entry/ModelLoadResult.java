package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.openide.util.Lookup;

/**
 *
 * @see GradleProjectExtensionDef#parseModel(ModelLoadResult)
 */
public final class ModelLoadResult {
    private final File mainProjectDir;
    private final Lookup mainProjectModels;

    private final Map<File, Lookup> evaluatedProjectsModel;

    /***/
    public ModelLoadResult(
            @Nonnull File mainProjectDir,
            @Nonnull Map<File, Lookup> evaluatedProjectsModel) {

        if (mainProjectDir == null) throw new NullPointerException("mainProjectDir");

        this.mainProjectDir = mainProjectDir;
        this.evaluatedProjectsModel = CollectionUtils.copyNullSafeHashMap(evaluatedProjectsModel);
        this.mainProjectModels = this.evaluatedProjectsModel.get(mainProjectDir);

        if (mainProjectModels == null) {
            throw new NullPointerException("evaluatedProjectsModel.get(mainProjectDir)");
        }
    }

    /***/
    public ModelLoadResult(
            @Nonnull File mainProjectDir,
            @Nonnull Lookup mainProjectModels) {

        this(mainProjectDir,
                mainProjectModels,
                Collections.singletonMap(mainProjectDir, mainProjectModels));
    }

    private ModelLoadResult(
            @Nonnull File mainProjectDir,
            @Nonnull Lookup mainProjectModels,
            @Nonnull Map<File, Lookup> evaluatedProjectsModel) {

        if (mainProjectDir == null) throw new NullPointerException("mainProjectDir");
        if (mainProjectModels == null) throw new NullPointerException("mainProjectModels");

        this.mainProjectDir = mainProjectDir;
        this.mainProjectModels = mainProjectModels;
        this.evaluatedProjectsModel = evaluatedProjectsModel;
    }

    /***/
    @Nonnull
    public ModelLoadResult withMainProject(@Nonnull File projectDir) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        Lookup models = evaluatedProjectsModel.get(projectDir);
        if (models == null) {
            throw new IllegalArgumentException("Not an evaluated project: " + projectDir);
        }

        return new ModelLoadResult(projectDir, models, evaluatedProjectsModel);
    }

    /***/
    @Nonnull
    public File getMainProjectDir() {
        return mainProjectDir;
    }

    /***/
    @Nonnull
    public Lookup getMainProjectModels() {
        return mainProjectModels;
    }

    /***/
    @Nonnull
    public Map<File, Lookup> getEvaluatedProjectsModel() {
        return evaluatedProjectsModel;
    }
}
