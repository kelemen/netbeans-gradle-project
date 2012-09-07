package org.netbeans.gradle.project.newproject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.StringUtils;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(
        folder="Project/Gradle",
        displayName="#template.subGradleProject",
        iconBase=NbIcons.PROJECT_ICON_PATH)
@Messages("template.subGradleProject=Gradle Subproject")
public final class GradleSubProjectWizardIterator
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    @StaticResource
    private static final String SINGLE_PROJECT_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/subproject.gradle";

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private int descriptorIndex;

    public GradleSubProjectWizardIterator() {
        this.descriptors = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<GradleSingleProjectConfig>(null);
    }

    private static void createBuildGradle(
            File projectDir,
            GradleSingleProjectConfig config) throws IOException {
        String mainClass = config.getMainClass();

        String buildGradleContent = StringUtils.getResourceAsString(
                SINGLE_PROJECT_BUILD_GRADLE,
                NewProjectUtils.DEFAULT_FILE_ENCODING);

        buildGradleContent = buildGradleContent.replace("${MAIN_CLASS}",
                mainClass != null ? mainClass : "");

        File buildGradle = new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME);
        StringUtils.writeStringToFile(buildGradleContent, NewProjectUtils.DEFAULT_FILE_ENCODING, buildGradle);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleSingleProjectConfig config = configRef.get();
        if (config == null) {
            throw new IOException("Missing configuration.");
        }

        File projectDirAsFile = FileUtil.normalizeFile(config.getProjectFolder());
        FileObject projectDir = FileUtil.createFolder(projectDirAsFile);

        NewProjectUtils.createDefaultSourceDirs(projectDir);
        createBuildGradle(projectDirAsFile, config);

        String mainClass = config.getMainClass();
        if (mainClass != null) {
            NewProjectUtils.createMainClass(projectDirAsFile, mainClass);
        }

        return Collections.singleton(projectDir);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        uninitialize(wizard);

        descriptorIndex = 0;
        descriptors.add(new GradleSubProjectConfigPanel(configRef));
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
