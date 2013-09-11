package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.project.DynamicLookup;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Parameters;

public final class NbGradleModel {
    private final File buildFile;
    private final File settingsFile;
    private final NbGradleMultiProjectDef projectDef;

    private volatile boolean dirty;

    private final AtomicReference<DynamicLookup> mainModelsRef;

    // Maps extension name to DynamicLookup
    private final ConcurrentMap<String, DynamicLookup> extensionModels;
    private final DynamicLookup allModels;

    private final String displayName;
    // This field is provided, so that the governing project can detect that
    // something has changed in the model, so it needs to reparse things.
    private final AtomicReference<Object> stateID;

    public NbGradleModel(NbGradleMultiProjectDef projectDef) {
        this(projectDef, findSettingsGradle(projectDef.getProjectDir()));
    }

    public NbGradleModel(NbGradleMultiProjectDef projectDef, File settingsFile) {
        this(projectDef,
                getBuildFile(projectDef.getProjectDir()),
                settingsFile,
                new AtomicReference<DynamicLookup>(null),
                new ConcurrentHashMap<String, DynamicLookup>());
    }

    private NbGradleModel(
            NbGradleMultiProjectDef projectDef,
            File buildFile,
            File settingsFile,
            AtomicReference<DynamicLookup> mainLookupRef,
            ConcurrentMap<String, DynamicLookup> extensionModels) {
        if (projectDef == null) throw new NullPointerException("projectTree");
        if (mainLookupRef == null) throw new NullPointerException("mainLookupRef");
        if (extensionModels == null) throw new NullPointerException("extensionModels");

        this.projectDef = projectDef;
        this.buildFile = buildFile;
        this.settingsFile = settingsFile;
        this.extensionModels = extensionModels;
        this.dirty = false;
        this.displayName = findDisplayName();
        this.mainModelsRef = mainLookupRef;
        this.allModels = new DynamicLookup();
        this.stateID = new AtomicReference<Object>(new Object());

        updateAllModels();
    }

    private void changeState() {
        stateID.set(new Object());
    }

    public Object getStateID() {
        return stateID.get();
    }

    private void updateAllModels() {
        List<Lookup> lookups = new LinkedList<Lookup>();
        Lookup mainModels = mainModelsRef.get();
        if (mainModels != null) {
            lookups.add(mainModels);
        }

        lookups.addAll(extensionModels.values());

        allModels.replaceLookups(lookups);
    }

    private String findDisplayName() {
        if (isBuildSrc()) {
            File parentFile = getProjectDir().getParentFile();
            String parentName = parentFile != null ? parentFile.getName() : "?";
            return NbStrings.getBuildSrcMarker(parentName);
        }
        else {
            String scriptName = getMainProject().getProjectName();
            scriptName = scriptName.trim();
            if (scriptName.isEmpty()) {
                scriptName = getProjectDir().getName();
            }

            if (isRootProject()) {
                return NbStrings.getRootProjectMarker(scriptName);
            }
            else {
                return scriptName;
            }
        }
    }

    public boolean hasUnloadedExtensions(NbGradleProject project) {
        for (ProjectExtensionRef extensionRef: project.getExtensionRefs()) {
            if (!extensionModels.containsKey(extensionRef.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<ProjectExtensionRef> getUnloadedExtensions(NbGradleProject project) {
        List<ProjectExtensionRef> result = new LinkedList<ProjectExtensionRef>();
        for (ProjectExtensionRef extensionRef: project.getExtensionRefs()) {
            if (!extensionModels.containsKey(extensionRef.getName())) {
                result.add(extensionRef);
            }
        }
        return result;
    }

    public void setModelsForExtension(String extensionName, Lookup models) {
        Parameters.notNull("extensionName", extensionName);
        Parameters.notNull("models", models);

        DynamicLookup oldLookup = extensionModels.putIfAbsent(extensionName, new DynamicLookup(models));
        if (oldLookup != null) {
            oldLookup.replaceLookups(models);
        }

        changeState();
        updateAllModels();
    }

    public Lookup getModelsForExtension(String extensionName) {
        Parameters.notNull("extensionName", extensionName);

        Lookup result = extensionModels.get(extensionName);
        return result != null ? result : Lookup.EMPTY;
    }

    public void setMainModels(Lookup mainModels) {
        Parameters.notNull("mainModels", mainModels);
        if (!mainModelsRef.compareAndSet(null, new DynamicLookup(mainModels))) {
            mainModelsRef.get().replaceLookups(mainModels);
        }
        changeState();
        updateAllModels();
    }

    public Lookup getAllModels() {
        return DynamicLookup.viewLookup(allModels);
    }

    public static File getBuildFile(File projectDir) {
        File buildFile = new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME);
        if (buildFile.isFile()) {
            return buildFile;
        }

        buildFile = new File(projectDir, projectDir.getName() + GradleProjectConstants.DEFAULT_GRADLE_EXTENSION);
        if (buildFile.isFile()) {
            return buildFile;
        }

        return null;
    }

    public static File findSettingsGradle(File projectDir) {
        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        FileObject resultObj = findSettingsGradle(projectDirObj);
        return resultObj != null
                ? FileUtil.toFile(resultObj)
                : null;
    }

    public static FileObject findSettingsGradle(FileObject projectDir) {
        if (projectDir == null) {
            return null;
        }

        FileObject settingsGradle = projectDir.getFileObject(GradleProjectConstants.SETTINGS_FILE_NAME);
        if (settingsGradle != null && !settingsGradle.isVirtual()) {
            return settingsGradle;
        }
        else {
            return findSettingsGradle(projectDir.getParent());
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBuildSrc() {
        return getProjectDir().getName().equalsIgnoreCase(GradleProjectConstants.BUILD_SRC_NAME);
    }

    public boolean isRootProject() {
        String uniqueName = getMainProject().getProjectFullName();
        for (int i = 0; i < uniqueName.length(); i++) {
            if (uniqueName.charAt(i) != ':') {
                return false;
            }
        }
        return true;
    }

    public NbGradleMultiProjectDef getProjectDef() {
        return projectDef;
    }

    public NbGradleProjectTree getMainProject() {
        return projectDef.getMainProject();
    }

    public void setDirty() {
        this.dirty = true;
    }

    public NbGradleModel createNonDirtyCopy() {
        return new NbGradleModel(projectDef, buildFile, settingsFile, mainModelsRef, extensionModels);
    }

    public File getProjectDir() {
        return projectDef.getProjectDir();
    }

    public File getRootProjectDir() {
        File result = null;
        if (settingsFile != null) {
            result = settingsFile.getParentFile();
        }

        if (result == null) {
            result = getProjectDir();
        }
        return result;
    }

    public File getBuildFile() {
        return buildFile;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public boolean isDirty() {
        return dirty;
    }

    public FileObject tryGetProjectDirAsObj() {
        return FileUtil.toFileObject(getProjectDir());
    }

    public FileObject tryGetBuildFileObj() {
        return GradleFileUtils.asFileObject(buildFile);
    }

    public FileObject tryGetSettingsFileObj() {
        return GradleFileUtils.asFileObject(settingsFile);
    }
}
