package org.netbeans.gradle.project.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Mutex;
import org.openide.util.lookup.Lookups;

@ActionRegistration(
  displayName="org.netbeans.gradle.project.Bundle#NbStrings.Build"
)
@ActionID(
  category="GradleProject",
  id="GradleBuildAction" 
)
@ActionReference(path="Actions/GradleProject", position = 4, separatorBefore = 3)
public class BuildAction implements ActionListener {

    private List<NbGradleProject> projects;

    public BuildAction(List<NbGradleProject> projects) {
        this.projects = projects;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Project p : projects) {
            final ActionProvider ap = p.getLookup().lookup(ActionProvider.class);
            if (ap == null) {
                return;
            }

            Mutex.EVENT.writeAccess(new Runnable() {
                @Override
                public void run() {
                    ap.invokeAction(ActionProvider.COMMAND_BUILD, Lookups.singleton(new ActionProgress() {
                        @Override
                        protected void started() {
                        }

                        @Override
                        public void finished(boolean success) {
                        }
                    }));
                }
            });
        }
    }

}
