package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.Objects;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.util.CloseableAction;
import org.netbeans.gradle.project.util.CloseableActionContainer;
import org.netbeans.spi.project.ui.ProjectOpenedHook;

public final class GenericOpenHook extends ProjectOpenedHook {
    private final CloseableActionContainer closeableActions;
    private final Runnable openInitializer;

    private GenericOpenHook(
            CloseableActionContainer closeableActions,
            Runnable openInitializer) {
        this.closeableActions = closeableActions;
        this.openInitializer = openInitializer;
    }

    public static ProjectOpenedHook create(
            Collection<? extends PropertySource<? extends CloseableAction>> actionProperties,
            Runnable openInitializer) {
        Objects.requireNonNull(actionProperties, "actionProperties");
        Objects.requireNonNull(openInitializer, "openInitializer");

        CloseableActionContainer closeableActions = new CloseableActionContainer();

        for (PropertySource<? extends CloseableAction> actionProperty: actionProperties) {
            closeableActions.defineAction(actionProperty);
        }

        return new GenericOpenHook(closeableActions, openInitializer);
    }

    @Override
    protected void projectOpened() {
        closeableActions.open();
        openInitializer.run();
    }

    @Override
    protected void projectClosed() {
        closeableActions.close();
    }
}
