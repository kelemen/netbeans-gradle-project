package org.netbeans.gradle.project.newproject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.properties.standard.SourceLevelProperty;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.GroovyScripts;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
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

    @StaticResource
    private static final String MULTI_PROJECT_COMMON_GRADLE = "org/netbeans/gradle/project/resources/newproject/multi-project-common.gradle";

    @StaticResource
    private static final String MULTI_PROJECT_SETTINGS_GRADLE = "org/netbeans/gradle/project/resources/newproject/multi-project-settings.gradle";

    private static final String EXTENSION = GroovyScripts.EXTENSION;

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleMultiProjectConfig> configRef;
    private int descriptorIndex;

    public GradleMultiProjectWizardIterator() {
        this.descriptors = new ArrayList<>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<>(null);
    }

    private static void createSettingsGradle(Path projectDir) throws IOException {
        Map<String, String> varReplaceMap =
                Collections.singletonMap("${PROJECT_NAME}", NbFileUtils.getFileNameStr(projectDir));

        NewProjectUtils.copyTemplateFile(MULTI_PROJECT_SETTINGS_GRADLE,
                projectDir.resolve(CommonScripts.SETTINGS_BASE_NAME + EXTENSION),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
    }

    private static void createParentGradle(
            Path projectDir,
            GradleMultiProjectConfig config) throws IOException {

        Map<String, String> varReplaceMap = new HashMap<>();
        varReplaceMap.put("${MAVEN_GROUP}", config.getMavenGroupId());
        varReplaceMap.put("${MAVEN_VERSION}", config.getMavenVersion());
        varReplaceMap.put("${SOURCE_LEVEL}", SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault()));

        NewProjectUtils.copyTemplateFile(
                MULTI_PROJECT_COMMON_GRADLE,
                projectDir.resolve("common.gradle"),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
    }

    private static void copyToProject(
            Path projectDir,
            String resourcePath,
            String fileName) throws IOException {

        String content = StringUtils.getResourceAsString(
                resourcePath,
                NewProjectUtils.DEFAULT_FILE_ENCODING);
        Path file = projectDir.resolve(fileName);
        content = StringUtils.replaceLFWithPreferredLineSeparator(content);
        StringUtils.writeStringToFile(content, NewProjectUtils.DEFAULT_FILE_ENCODING, file);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleMultiProjectConfig config = configRef.get();
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
        createParentGradle(projectDir, config);
        copyToProject(projectDir, MULTI_PROJECT_BUILD_GRADLE, CommonScripts.BUILD_BASE_NAME + EXTENSION);
        createSettingsGradle(projectDir);

        return Collections.singleton(projectDirObj);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        uninitialize(wizard);

        descriptorIndex = 0;
        descriptors.add(new GradleMultiProjectConfigPanel(wizard, configRef::set));
        wizard.putProperty ("NewProjectWizard_Title", "Gradle Project"); // NOI18N
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
