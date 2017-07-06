package org.netbeans.gradle.project.filesupport;

import java.awt.Component;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.MutableProperty;
import org.openide.WizardDescriptor;
import org.openide.loaders.TemplateWizard;
import org.openide.util.HelpCtx;

final class GradleTemplateWizardPanelWrapper implements WizardDescriptor.Panel<WizardDescriptor> {
    private final MutableProperty<GradleTemplateWizardConfig> config;
    private final TemplateWizard wizard;

    private final AtomicReference<GradleTemplateWizardPanel> panelRef;

    public GradleTemplateWizardPanelWrapper(MutableProperty<GradleTemplateWizardConfig> config, TemplateWizard wizard) {
        this.config = Objects.requireNonNull(config, "config");
        this.wizard = Objects.requireNonNull(wizard, "wizard");
        this.panelRef = new AtomicReference<>(null);
    }

    private GradleTemplateWizardPanel getPanel() {
        GradleTemplateWizardPanel result = panelRef.get();
        if (result == null) {
            result = new GradleTemplateWizardPanel(wizard);
            if (!panelRef.compareAndSet(null, result)) {
                result = panelRef.get();
            }
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
        config.setValue(getPanel().getConfig());
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
