package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.jtrim2.utils.LazyValues;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleMultiProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final Supplier<GradleMultiProjectPropertiesPanel> panel;
    private final Consumer<? super GradleMultiProjectConfig> configRef;

    public GradleMultiProjectConfigPanel(WizardDescriptor wizard, Consumer<? super GradleMultiProjectConfig> configRef) {
        this.configRef = Objects.requireNonNull(configRef, "configRef");

        Objects.requireNonNull(wizard, "wizard");
        this.panel = LazyValues.lazyValue(() -> new GradleMultiProjectPropertiesPanel(wizard));
    }

    private GradleMultiProjectPropertiesPanel getPanel() {
        return panel.get();
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
