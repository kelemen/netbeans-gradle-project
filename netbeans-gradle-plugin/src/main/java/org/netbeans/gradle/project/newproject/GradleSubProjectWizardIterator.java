package org.netbeans.gradle.project.newproject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.GroovyScripts;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(
        folder="Project/Gradle",
        displayName="#template.subGradleProject",
        description="Subproject.html",
        iconBase=NbIcons.PROJECT_ICON_PATH)
@Messages("template.subGradleProject=Gradle Subproject")
public final class GradleSubProjectWizardIterator
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    @StaticResource
    private static final String SINGLE_PROJECT_OLD_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/subproject.gradle";

    @StaticResource
    private static final String SINGLE_PROJECT_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/subproject2.gradle";

    private static final String EXTENSION = GroovyScripts.EXTENSION;

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private int descriptorIndex;

    public GradleSubProjectWizardIterator() {
        this.descriptors = new ArrayList<>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<>(null);
    }

    private static void createBuildGradle(
            Path projectDir,
            GradleSingleProjectConfig config) throws IOException {
        Path rootDir = projectDir.getParent();
        if (rootDir == null) {
            throw new IOException("Invalid project directory for subproject.");
        }

        boolean oldFormat = Files.isRegularFile(rootDir.resolve("parent.gradle"));
        boolean newFormat = Files.isRegularFile(rootDir.resolve("common.gradle"));
        if (oldFormat && newFormat) {
            throw new IOException("Cannot determine if the project uses the new or the old format.");
        }
        if (!oldFormat && !newFormat) {
            throw new IOException("The parent directory does not appear to be created by the multi-project wizard.");
        }

        String mainClass = config.getMainClass();

        String buildGradleContent = StringUtils.getResourceAsString(
                newFormat ? SINGLE_PROJECT_BUILD_GRADLE : SINGLE_PROJECT_OLD_BUILD_GRADLE,
                NewProjectUtils.DEFAULT_FILE_ENCODING);

        buildGradleContent = buildGradleContent.replace("${MAIN_CLASS}",
                mainClass != null ? mainClass : "");

        buildGradleContent = StringUtils.replaceLFWithPreferredLineSeparator(buildGradleContent);

        Path buildGradle = projectDir.resolve(CommonScripts.BUILD_BASE_NAME + EXTENSION);
        StringUtils.writeStringToFile(buildGradleContent, NewProjectUtils.DEFAULT_FILE_ENCODING, buildGradle);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleSingleProjectConfig config = configRef.get();
        if (config == null) {
            throw new IOException("Missing configuration.");
        }

        Path projectDir = config.getProjectFolder().normalize();
        Files.createDirectories(projectDir);

        FileObject projectDirObj = FileUtil.toFileObject(projectDir.toFile());
        if (projectDirObj == null) {
            throw new IOException("Failed to open directory: " + projectDir);
        }


        NewProjectUtils.createDefaultSourceDirs(projectDir);
        createBuildGradle(projectDir, config);

        String mainClass = config.getMainClass();
        if (mainClass != null) {
            NewProjectUtils.createMainClass(projectDir, mainClass);
        }

        return Collections.singleton(projectDirObj);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        uninitialize(wizard);

        descriptorIndex = 0;
        descriptors.add(new GradleSubProjectConfigPanel(wizard, configRef::set));
        wizard.putProperty ("NewProjectWizard_Title", "Gradle Subproject"); // NOI18N
        JComponent c = (JComponent) descriptors.get(0).getComponent();
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, new String[] {"Name and Location"});
        c.setName("Name and Location");
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        descriptors.clear();
        configRef.set(null);
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return descriptors.get(descriptorIndex);
    }

    @Override
    public String name() {
        return "GradleSubProjectTemplate";
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
