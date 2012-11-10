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
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(
        folder="Project/Gradle",
        displayName="#template.multiGradleProject",
        description="MultiProject.html",
        iconBase=NbIcons.PROJECT_ICON_PATH)
@Messages("template.multiGradleProject=Gradle Root Project")
public final class GradleMultiProjectWizardIterator
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    @StaticResource
    private static final String MULTI_PROJECT_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/multi-project-root.gradle";
    private static final String MULTI_PROJECT_PARENT_GRADLE = "org/netbeans/gradle/project/resources/newproject/multi-project-parent.gradle";
    private static final String MULTI_PROJECT_SETTINGS_GRADLE = "org/netbeans/gradle/project/resources/newproject/multi-project-settings.gradle";

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleMultiProjectConfig> configRef;
    private int descriptorIndex;

    public GradleMultiProjectWizardIterator() {
        this.descriptors = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<GradleMultiProjectConfig>(null);
    }

    private static void createParentGradle(
            File projectDir,
            GradleMultiProjectConfig config) throws IOException {
        String parentGradleContent = StringUtils.getResourceAsString(
                MULTI_PROJECT_PARENT_GRADLE,
                NewProjectUtils.DEFAULT_FILE_ENCODING);

        parentGradleContent = parentGradleContent.replace("${MAVEN_GROUP}",
                config.getMavenGroupId());
        parentGradleContent = parentGradleContent.replace("${MAVEN_VERSION}",
                config.getMavenVersion());
        parentGradleContent = parentGradleContent.replace("${SOURCE_LEVEL}",
                AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault()));

        File parentGradle = new File(projectDir, "parent.gradle");
        StringUtils.writeStringToFile(parentGradleContent, NewProjectUtils.DEFAULT_FILE_ENCODING, parentGradle);
    }

    private static void copyToProject(
            File projectDir,
            String resourcePath,
            String fileName) throws IOException {

        String content = StringUtils.getResourceAsString(
                resourcePath,
                NewProjectUtils.DEFAULT_FILE_ENCODING);
        File file = new File(projectDir, fileName);
        StringUtils.writeStringToFile(content, NewProjectUtils.DEFAULT_FILE_ENCODING, file);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleMultiProjectConfig config = configRef.get();
        if (config == null) {
            throw new IOException("Missing configuration.");
        }

        File projectDirAsFile = FileUtil.normalizeFile(config.getProjectFolder());
        FileObject projectDir = FileUtil.createFolder(projectDirAsFile);

        NewProjectUtils.createDefaultSourceDirs(projectDir);
        createParentGradle(projectDirAsFile, config);
        copyToProject(projectDirAsFile, MULTI_PROJECT_BUILD_GRADLE, GradleProjectConstants.BUILD_FILE_NAME);
        copyToProject(projectDirAsFile, MULTI_PROJECT_SETTINGS_GRADLE, GradleProjectConstants.SETTINGS_FILE_NAME);

        return Collections.singleton(projectDir);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        uninitialize(wizard);

        descriptorIndex = 0;
        descriptors.add(new GradleMultiProjectConfigPanel(configRef));
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
        return "GradleMultiProjectTemplate";
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
