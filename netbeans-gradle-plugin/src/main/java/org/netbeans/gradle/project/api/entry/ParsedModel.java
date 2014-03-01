package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Stores the parsed model for an extension for multiple projects.
 * There is always a main model associated with the project for which the
 * models (from which {@code ParsedModel} was parsed) were retrieved for.
 * <P>
 * Additionally {@code ParsedModel} may store models of other projects. This
 * makes sense if the models for projects can be parsed with no additional cost
 * when parsing a model for a particular project. This might happen if the
 * project is completely parsed from a {@link org.gradle.tooling.model.idea.IdeaProject IdeaProject}.
 * <P>
 * The main model and any other models might be {@code null}, meaning that the
 * associated extension is to be disabled.
 *
 * @param <ModelType> the type of the parsed model storing the information
 *   retrieved from the evaluated build script of the Gradle project
 *
 * @see GradleProjectExtensionDef#parseModel(ModelLoadResult) GradleProjectExtensionDef.parseModel
 */
public final class ParsedModel<ModelType> {
    private static final ParsedModel<?> EMPTY = new ParsedModel<>();

    private final ModelType mainModel;
    private final Map<File, ModelType> otherProjectsModel;

    private ParsedModel() {
        this.mainModel = null;
        this.otherProjectsModel = Collections.emptyMap();
    }

    /**
     * Creates a new {@code ParsedModel} from the given parsed model.
     * This method is effectively the same as calling the two argument
     * constructor with an empty map.
     *
     * @param mainModel the model for the project for which the
     *   models (from which {@code ParsedModel} was parsed) were retrieved for.
     *   This argument can be {@code null} if the associated extension is to be
     *   disabled.
     */
    public ParsedModel(@Nullable ModelType mainModel) {
        this.mainModel = mainModel;
        this.otherProjectsModel = Collections.emptyMap();
    }

    /**
     * Creates a new {@code ParsedModel} from the given parsed models.
     * Other projects are referenced by their project directory.
     *
     * @param mainModel the model for the project for which the
     *   models (from which {@code ParsedModel} was parsed) were retrieved for.
     *   This argument can be {@code null} if the associated extension is to be
     *   disabled.
     * @param otherProjectsModel the models parsed for other projects. The
     *   specified map, maps projects based on their project directory to the
     *   parsed model. This argument cannot be {@code null}, its keys can
     *   neither be {@code null} but the values in the map are allowed to be
     *   {@code null}. A {@code null} value for a given entry means that
     *   the associated extension for that project is to be disabled.
     */
    public ParsedModel(
            @Nullable ModelType mainModel,
            @Nonnull Map<File, ? extends ModelType> otherProjectsModel) {
        this.mainModel = mainModel;
        this.otherProjectsModel = CollectionUtils.copyNullSafeHashMapWithNullValues(otherProjectsModel);
    }

    /**
     * Returns a {@code ParsedModel} with {@code null}
     * {@link #getMainModel() main model} and an empty map returned by its
     * {@link #getOtherProjectsModel() getOtherProjectsModel} method.
     *
     * @param <ModelType> the type of the parsed model. Not relevant as the
     *   returned instances stores no instance of {@code ModelType}.
     *
     * @return an empty {@code ParsedModel}. This method never returns
     * {@code null}.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <ModelType> ParsedModel<ModelType> noModels() {
        // This is a safe cast because we only return {@code null} entries
        // from methods which must return a ModelType instance.
        return (ParsedModel<ModelType>)EMPTY;
    }

    /**
     * Returns the model for the project for which the models (from which this
     * {@code ParsedModel} was parsed) were retrieved for.
     *
     * @return the model for the project for which the models (from which this
     *   {@code ParsedModel} was parsed) were retrieved for.
     */
    @Nullable
    public ModelType getMainModel() {
        return mainModel;
    }

    /**
     * Returns the models parsed for other projects. The returned map, maps
     * projects based on their project directory to the parsed model. The keys
     * of the returned map cannot be {@code null} but the values in the map are
     * allowed to be {@code null}. A {@code null} value for a given entry means
     * that the associated extension for that project is to be disabled.
     *
     * @return the models parsed for other projects. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public Map<File, ModelType> getOtherProjectsModel() {
        return otherProjectsModel;
    }
}
