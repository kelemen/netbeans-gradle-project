package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleMultiProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final AtomicReference<GradleMultiProjectPropertiesPanel> panel;
    private final AtomicReference<GradleMultiProjectConfig> configRef;
    private final WizardDescriptor wizard;

    public GradleMultiProjectConfigPanel(AtomicReference<GradleMultiProjectConfig> configRef, WizardDescriptor wizard) {
        if (configRef == null) throw new NullPointerException("configRef");

        this.configRef = configRef;
        this.panel = new AtomicReference<GradleMultiProjectPropertiesPanel>();
        this.wizard = wizard;
    }

    private GradleMultiProjectPropertiesPanel getPanel() {
        GradleMultiProjectPropertiesPanel result = panel.get();
        if (result == null) {
            panel.compareAndSet(null, new GradleMultiProjectPropertiesPanel(wizard));
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
