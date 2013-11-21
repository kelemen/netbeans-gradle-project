package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Collection;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.util.CollectionUtils;

final class ActionFetchedModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CustomSerializedMap buildModels;
    private final ActionFetchedProjectModels defaultProjectModels;
    private final Collection<ActionFetchedProjectModels> otherProjectModels;

    public ActionFetchedModels(
            CustomSerializedMap buildModels,
            ActionFetchedProjectModels defaultProjectModels,
            Collection<ActionFetchedProjectModels> otherProjectModels) {
        if (buildModels == null) throw new NullPointerException("buildModels");
        if (defaultProjectModels == null) throw new NullPointerException("defaultProjectModels");

        this.buildModels = buildModels;
        this.defaultProjectModels = defaultProjectModels;
        this.otherProjectModels = CollectionUtils.copyNullSafeList(otherProjectModels);
    }

    public CustomSerializedMap getBuildModels() {
        return buildModels;
    }

    public ActionFetchedProjectModels getDefaultProjectModels() {
        return defaultProjectModels;
    }

    public Collection<ActionFetchedProjectModels> getOtherProjectModels() {
        return otherProjectModels;
    }
}
