package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.GroovyScripts;
import org.netbeans.gradle.project.validate.Problem;
import org.netbeans.gradle.project.validate.Validator;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleSubProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private static final String EXTENSION = GroovyScripts.EXTENSION;

    private final AtomicReference<GradleSingleProjectPropertiesPanel> panel;
    private final Consumer<? super GradleSingleProjectConfig> configRef;
    private final WizardDescriptor wizard;

    public GradleSubProjectConfigPanel(WizardDescriptor wizard, Consumer<? super GradleSingleProjectConfig> configRef) {
        this.configRef = Objects.requireNonNull(configRef, "configRef");
        this.wizard = wizard;
        this.panel = new AtomicReference<>();
    }

    private GradleSingleProjectPropertiesPanel getPanel() {
        GradleSingleProjectPropertiesPanel result = panel.get();
        if (result == null) {
            GradleSingleProjectPropertiesPanel newPanel
                    = new GradleSingleProjectPropertiesPanel(wizard);
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

                        Problem problemRootBuild = checkFile(rootProject, CommonScripts.BUILD_BASE_NAME + EXTENSION);
                        if (problemRootBuild != null) {
                            return problemRootBuild;
                        }

                        Problem problemRootSettings = checkFile(rootProject, CommonScripts.SETTINGS_BASE_NAME + EXTENSION);
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
        configRef.accept(getPanel().getConfig());
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
