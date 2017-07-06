package org.netbeans.gradle.project.filesupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.api.templates.TemplateRegistration;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.TemplateWizard;

@TemplateRegistration(
    folder=GradleTemplateConsts.FOLDER_NAME,
    id=GradleTemplateConsts.TEMPLATE_SCRIPT_NAME,
    displayName="#GradleFiles.template",
    iconBase="org/netbeans/gradle/project/resources/gradle.png",
    description="Gradle-Files.html",
    category=GradleTemplateConsts.CATEGORY_NAME
)
public final class GradleTemplateRegistration
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final MutableProperty<GradleTemplateWizardConfig> config;
    private int descriptorIndex;

    public GradleTemplateRegistration() {
        this.config = PropertyFactory.memPropertyConcurrent(null, true, SwingExecutors.getSimpleExecutor(false));
        this.descriptors = new ArrayList<>(1);
        this.descriptorIndex = 0;
    }

    public static FileObject getTemplateFileObj() {
        return GradleTemplateConsts.getTemplateFolder().getFileObject(GradleTemplateConsts.TEMPLATE_SCRIPT_NAME);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleTemplateWizardConfig currentConfig = config.getValue();
        if (currentConfig == null) {
            throw new IOException("Missing config.");
        }

        Path gradleFile = currentConfig.getGradleFile();
        Path dir = gradleFile.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Files.createFile(gradleFile);

        FileObject gradleFileObj = FileUtil.toFileObject(gradleFile.toFile());
        return gradleFileObj != null
                ? Collections.singleton(gradleFileObj)
                : Collections.<FileObject>emptySet();
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        if (!(wizard instanceof TemplateWizard)) {
            return;
        }

        descriptorIndex = 0;
        descriptors.add(new GradleTemplateWizardPanelWrapper(config, (TemplateWizard)wizard));
        JComponent c = (JComponent)descriptors.get(0).getComponent();
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, new String[] {"File name"});
        c.setName("File name");
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        descriptors.clear();
        config.setValue(null);
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return descriptors.get(descriptorIndex);
    }

    @Override
    public String name() {
        return "GradleFileTemplate";
    }

    @Override
    public boolean hasNext() {
        return descriptorIndex < descriptors.size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return descriptorIndex > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        descriptorIndex++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        descriptorIndex--;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }
}
