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
        displayName="#template.singleGradleProject",
        description="SingleProject.html",
        iconBase=NbIcons.PROJECT_ICON_PATH)
@Messages("template.singleGradleProject=Single Gradle Project")
public final class GradleSingleProjectWizardIterator
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    @StaticResource
    private static final String SINGLE_PROJECT_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/single-project.gradle";

    @StaticResource
    private static final String SINGLE_PROJECT_SETTINGS_GRADLE = "org/netbeans/gradle/project/resources/newproject/single-project-settings.gradle";

    private static final String EXTENSION = GroovyScripts.EXTENSION;

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private int descriptorIndex;

    public GradleSingleProjectWizardIterator() {
        this.descriptors = new ArrayList<>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<>(null);
    }

    private static void createBuildGradle(
            Path projectDir,
            GradleSingleProjectConfig config) throws IOException {
        String mainClass = config.getMainClass();
        String sourceLevel = SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        Map<String, String> varReplaceMap = new HashMap<>();
        varReplaceMap.put("${MAIN_CLASS}", StringUtils.emptyForNull(mainClass));
        varReplaceMap.put("${SOURCE_LEVEL}", sourceLevel);

        NewProjectUtils.copyTemplateFile(SINGLE_PROJECT_BUILD_GRADLE,
                projectDir.resolve(CommonScripts.BUILD_BASE_NAME + EXTENSION),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
    }

    private static void createSettingsGradle(Path projectDir) throws IOException {
        Map<String, String> varReplaceMap =
                Collections.singletonMap("${PROJECT_NAME}", NbFileUtils.getFileNameStr(projectDir));

        NewProjectUtils.copyTemplateFile(SINGLE_PROJECT_SETTINGS_GRADLE,
                projectDir.resolve(CommonScripts.SETTINGS_BASE_NAME + EXTENSION),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
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
        createSettingsGradle(projectDir);

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
        descriptors.add(new GradleSingleProjectConfigPanel(wizard, configRef::set));
        wizard.putProperty ("NewProjectWizard_Title", "Gradle Single Project"); // NOI18N
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
        return "GradleSingleProjectTemplate";
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
