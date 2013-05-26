package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.validate.Problem;
import org.netbeans.gradle.project.validate.Validator;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleSubProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final AtomicReference<GradleSingleProjectPropertiesPanel> panel;
    private final AtomicReference<GradleSingleProjectConfig> configRef;

    public GradleSubProjectConfigPanel(AtomicReference<GradleSingleProjectConfig> configRef) {
        if (configRef == null) throw new NullPointerException("configRef");

        this.configRef = configRef;
        this.panel = new AtomicReference<GradleSingleProjectPropertiesPanel>();
    }

    private GradleSingleProjectPropertiesPanel getPanel() {
        GradleSingleProjectPropertiesPanel result = panel.get();
        if (result == null) {
            GradleSingleProjectPropertiesPanel newPanel
                    = new GradleSingleProjectPropertiesPanel();
            if (panel.compareAndSet(null, newPanel)) {
                newPanel.addProjectLocationValidator(new Validator<String>() {
                    private Problem checkFile(File projectDir, String fileName) {
                        if (!new File(projectDir, fileName).isFile()) {
                            return Problem.severe(NewProjectStrings.getNotRootProject());
                        }
                        return null;
                    }

                    @Override
                    public Problem validateInput(String inputType) {
                        File rootProject = new File(inputType);

                        Problem problemRootBuild = checkFile(rootProject, GradleProjectConstants.BUILD_FILE_NAME);
                        if (problemRootBuild != null) {
                            return problemRootBuild;
                        }

                        Problem problemRootSettings = checkFile(rootProject, GradleProjectConstants.SETTINGS_FILE_NAME);
                        if (problemRootSettings != null) {
                            return problemRootSettings;
                        }

                        Problem problemOldFormat = checkFile(rootProject, "parent.gradle");
                        Problem problemNewFromat = checkFile(rootProject, "common.gradle");

                        if (problemNewFromat != null && problemOldFormat != null) {
                            return problemNewFromat;
                        }

                        if (problemOldFormat == null && problemNewFromat == null) {
                            // Cannot determine which format.
                            return problemNewFromat;
                        }

                        return null;
                    }
                });
            }
            result = panel.get();
        }
        return result;
    }

    @Override
    public Component getComponent() {
        return getPanel();
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
        getPanel().startValidation();
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        configRef.set(getPanel().getConfig());
    }

    @Override
    public boolean isValid() {
        return getPanel().containsValidData();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        getPanel().addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        getPanel().removeChangeListener(listener);
    }
}
