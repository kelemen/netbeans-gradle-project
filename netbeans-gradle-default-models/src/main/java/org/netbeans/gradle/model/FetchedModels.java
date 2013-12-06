package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FetchedBuildModels buildModels;
    private final FetchedProjectModels defaultProjectModels;
    private final Collection<FetchedProjectModels> otherProjectModels;

    public FetchedModels(
            FetchedBuildModels buildModels,
            FetchedProjectModels defaultProjectModels,
            Collection<FetchedProjectModels> otherProjectModels) {
        if (buildModels == null) throw new NullPointerException("buildModels");
        if (defaultProjectModels == null) throw new NullPointerException("defaultProjectModels");

        this.buildModels = buildModels;
        this.defaultProjectModels = defaultProjectModels;
        this.otherProjectModels = Collections.unmodifiableList(new ArrayList<FetchedProjectModels>(otherProjectModels));

        CollectionUtils.checkNoNullElements(this.otherProjectModels, "otherProjectModels");
    }

    public FetchedBuildModels getBuildModels() {
        return buildModels;
    }

    public Map<Object, List<BuilderResult>> getBuildInfoResults() {
        return buildModels.getBuildInfoResults();
    }

    public FetchedProjectModels getDefaultProjectModels() {
        return defaultProjectModels;
    }

    public Collection<FetchedProjectModels> getOtherProjectModels() {
        return otherProjectModels;
    }
}
