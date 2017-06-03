package org.netbeans.gradle.project.keys;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.util.ContextUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

@ActionRegistration(displayName = "Reload Gradle Project", key = "", lazy = true)
@ActionID(category = "Project", id = "org.netbeans.gradle.project.keys.ReloadProjectAction")
@SuppressWarnings("serial")
public final class ReloadProjectAction extends AbstractAction implements ContextAwareAction {
    private final Lookup context;

    public ReloadProjectAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ReloadProjectAction(Lookup context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NbGradleProject project = ContextUtils.tryGetGradleProjectFromContext(context);
        if (project != null) {
            project.reloadProject();
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ReloadProjectAction(context);
    }
}
