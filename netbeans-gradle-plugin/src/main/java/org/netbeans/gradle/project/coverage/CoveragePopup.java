package org.netbeans.gradle.project.coverage;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.codecoverage.api.CoverageActionFactory;
import org.netbeans.modules.gsf.codecoverage.api.CoverageManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

@ActionID(category="Project", id="org.netbeans.gradle.project.coverage.CoveragePopup")
@ActionRegistration(displayName="Gradle Coverage", lazy=false) // NOI18N
@ActionReference(path="Projects/org.netbeans.gradle.project/Actions", position=1205)
public class CoveragePopup extends AbstractAction implements ContextAwareAction {
    private static final long serialVersionUID = 1L;

    public CoveragePopup() {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        setEnabled(false);
    }

    public @Override void actionPerformed(ActionEvent e) {
        assert false;
    }

    public @Override Action createContextAwareInstance(Lookup ctx) {
        Project p = ctx.lookup(Project.class);
        if (p == null) {
            return this;
        }
        if (!CoverageManager.INSTANCE.isEnabled(p)) {
            // or could show it anyway in case provider is present
            return this;
        }
        return ((ContextAwareAction) CoverageActionFactory.createCollectorAction(null, null)).createContextAwareInstance(ctx);
    }

}
