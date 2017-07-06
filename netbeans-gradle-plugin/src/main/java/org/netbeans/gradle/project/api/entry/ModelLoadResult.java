package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.openide.util.Lookup;

/**
 * Defines the result of the evaluation of a build script for a particular
 * extension. That is, the argument for the
 * {@link GradleProjectExtensionDef#parseModel(ModelLoadResult)}.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently. Though the contents of the lookup may
 * change in general, the Gradle plugin only passes a lookup to the
 * {@code parseModel} method which never changes.
 *
 * @see GradleProjectExtensionDef#parseModel(ModelLoadResult)
 */
public final class ModelLoadResult {
    private final GradleTarget evaluationEnvironment;
    private final File mainProjectDir;
    private final Lookup mainProjectModels;

    private final Map<File, Lookup> evaluatedProjectsModel;

    /**
     * Creates a {@code ModelLoadResult} with the specified main project
     * (given by its project directory) and retrieved models for all evaluated
     * projects.
     *
     * @param evaluationEnvironment defines the Java and Gradle versions used
     *   when the project was evaluated. This argument cannot be {@code null}.
     * @param mainProjectDir the project directory of the main project.
     *   This argument cannot be {@code null}.
     * @param evaluatedProjectsModel the map mapping projects (specified by
     *   their project directory) to retrieved models. This map must contain a
     *   {@code Lookup} for the specified {@code mainProjectDir}. This argument
     *   cannot be {@code null} and neither can its keys and values be
     *   {@code null}.
     */
    public ModelLoadResult(
            @Nonnull GradleTarget evaluationEnvironment,
            @Nonnull File mainProjectDir,
            @Nonnull Map<File, Lookup> evaluatedProjectsModel) {

        this.evaluationEnvironment = Objects.requireNonNull(evaluationEnvironment, "this.evaluationEnvironment");
        this.mainProjectDir = Objects.requireNonNull(mainProjectDir, "mainProjectDir");
        this.evaluatedProjectsModel = CollectionUtils.copyNullSafeHashMap(evaluatedProjectsModel);
        this.mainProjectModels = this.evaluatedProjectsModel.get(mainProjectDir);

        Objects.requireNonNull(mainProjectModels, "evaluatedProjectsModel.get(mainProjectDir)");
    }

    /**
     * Creates a {@code ModelLoadResult} with the specified main project
     * (given by its project directory) assuming a single evaluated project.
     * The evaluated projects map will contain a single entry: The main project.
     *
     * @param evaluationEnvironment defines the Java and Gradle versions used
     *   when the project was evaluated. This argument cannot be {@code null}.
     * @param mainProjectDir the project directory of the main project.
     *   This argument cannot be {@code null}.
     * @param mainProjectModels the models retrieved for the main project.
     *   This argument cannot be {@code null}.
     */
    public ModelLoadResult(
            @Nonnull GradleTarget evaluationEnvironment,
            @Nonnull File mainProjectDir,
            @Nonnull Lookup mainProjectModels) {

        this(evaluationEnvironment,
                mainProjectDir,
                mainProjectModels,
                Collections.singletonMap(mainProjectDir, mainProjectModels));
    }

    private ModelLoadResult(
            @Nonnull GradleTarget evaluationEnvironment,
            @Nonnull File mainProjectDir,
            @Nonnull Lookup mainProjectModels,
            @Nonnull Map<File, Lookup> evaluatedProjectsModel) {

        this.evaluationEnvironment = Objects.requireNonNull(evaluationEnvironment, "evaluationEnvironment");
        this.mainProjectDir = Objects.requireNonNull(mainProjectDir, "mainProjectDir");
        this.mainProjectModels = Objects.requireNonNull(mainProjectModels, "mainProjectModels");
        this.evaluatedProjectsModel = evaluatedProjectsModel;
    }

    /**
     * Returns a {@code ModelLoadResult} with the same evaluated projects but
     * with the given project as a main project. This {@code ModelLoadResult}
     * must contain an entry for the specified project directory.
     *
     * @param projectDir the project directory of the main project of the
     *   returned {@code ModelLoadResult}. This argument cannot be {@code null}.
     * @return the {@code ModelLoadResult} with the same evaluated projects but
     *   with the given project as a main project. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public ModelLoadResult withMainProject(@Nonnull File projectDir) {
        Objects.requireNonNull(projectDir, "projectDir");

        Lookup models = evaluatedProjectsModel.get(projectDir);
        if (models == null) {
            throw new IllegalArgumentException("Not an evaluated project: " + projectDir);
        }

        return new ModelLoadResult(evaluationEnvironment, projectDir, models, evaluatedProjectsModel);
    }

    /**
     * Returns the Java and Gradle versions used to evaluate the projects for these models.
     *
     * @return the Java and Gradle versions used to evaluate the projects for these models.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public GradleTarget getEvaluationEnvironment() {
        return evaluationEnvironment;
    }

    /**
     * Returns the project directory of the main evaluated project.
     * <P>
     * Note: {@code getEvaluatedProjectsModel().get(getMainProjectDir())} is
     * never {@code null}.
     *
     * @return the project directory of the main evaluated project. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public File getMainProjectDir() {
        return mainProjectDir;
    }

    /**
     * Returns the retrieved models for the main project.
     * <P>
     * This method returns the same lookup as
     * {@code getEvaluatedProjectsModel().get(getMainProjectDir())}.
     *
     * @return the retrieved models for the main project. This method never
     *   returns {@code null}.
     */
    @Nonnull
    public Lookup getMainProjectModels() {
        return mainProjectModels;
    }

    /**
     * Returns the retrieved models for all the evaluated projects, including
     * the main project.
     *
     * @return the retrieved models for all the evaluated projects. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public Map<File, Lookup> getEvaluatedProjectsModel() {
        return evaluatedProjectsModel;
    }
}
