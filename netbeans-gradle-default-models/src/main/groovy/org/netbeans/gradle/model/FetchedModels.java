package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, Object> buildInfoResults;
    private final FetchedProjectModels defaultProjectModels;
    private final Collection<FetchedProjectModels> otherProjectModels;

    public FetchedModels(
            Map<Object, Object> buildInfoResults,
            FetchedProjectModels defaultProjectModels,
            Collection<FetchedProjectModels> otherProjectModels) {
        if (defaultProjectModels == null) throw new NullPointerException("defaultProjectModels");

        this.buildInfoResults = CollectionUtils.copyNullSafeHashMap(buildInfoResults);
        this.defaultProjectModels = defaultProjectModels;
        this.otherProjectModels = Collections.unmodifiableList(new ArrayList<FetchedProjectModels>(otherProjectModels));

        CollectionUtils.checkNoNullElements(this.otherProjectModels, "otherProjectModels");
    }

    public Map<Object, Object> getBuildInfoResults() {
        return buildInfoResults;
    }

    public FetchedProjectModels getDefaultProjectModels() {
        return defaultProjectModels;
    }

    public Collection<FetchedProjectModels> getOtherProjectModels() {
        return otherProjectModels;
    }
}
