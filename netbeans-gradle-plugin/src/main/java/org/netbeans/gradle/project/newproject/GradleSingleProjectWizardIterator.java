package org.netbeans.gradle.project.newproject;

import java.io.File;
import java.io.IOException;
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
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.properties2.standard.SourceLevelProperty;
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

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private int descriptorIndex;

    public GradleSingleProjectWizardIterator() {
        this.descriptors = new ArrayList<>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<>(null);
    }

    private static void createBuildGradle(
            File projectDir,
            GradleSingleProjectConfig config) throws IOException {
        String mainClass = config.getMainClass();
        String sourceLevel = SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        Map<String, String> varReplaceMap = new HashMap<>();
        varReplaceMap.put("${MAIN_CLASS}", StringUtils.emptyForNull(mainClass));
        varReplaceMap.put("${SOURCE_LEVEL}", sourceLevel);

        NewProjectUtils.copyTemplateFile(
                SINGLE_PROJECT_BUILD_GRADLE,
                new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
    }

    private static void createSettingsGradle(File projectDir) throws IOException {
        Map<String, String> varReplaceMap =
                Collections.singletonMap("${PROJECT_NAME}", projectDir.getName());

        NewProjectUtils.copyTemplateFile(
                SINGLE_PROJECT_SETTINGS_GRADLE,
                new File(projectDir, GradleProjectConstants.SETTINGS_FILE_NAME),
                NewProjectUtils.DEFAULT_FILE_ENCODING,
                varReplaceMap);
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
        createSettingsGradle(projectDirAsFile);

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
        descriptors.add(new GradleSingleProjectConfigPanel(configRef, wizard));
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
