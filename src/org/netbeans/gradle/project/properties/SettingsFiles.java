package org.netbeans.gradle.project.properties;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class SettingsFiles {
    private static final String SETTINGS_FILENAME = ".nb-gradle-properties";
    private static final String PROFILE_FILE_NAME_SUFFIX = ".profile";
    private static final String SETTINGS_DIR_NAME = ".nb-gradle";
    private static final String PROFILE_DIRECTORY = "profiles";

    public static Collection<String> getAvailableProfiles(File rootDir) {
        File profileDir = getProfileDirectory(rootDir);
        if (!profileDir.isDirectory()) {
            return Collections.emptySet();
        }

        File[] profileFiles = profileDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ROOT).endsWith(PROFILE_FILE_NAME_SUFFIX);
            }
        });

        if (profileFiles == null) {
            return Collections.emptySet();
        }

        List<String> result = new ArrayList<String>(profileFiles.length);
        int suffixLength = PROFILE_FILE_NAME_SUFFIX.length();
        for (File profileFile: profileFiles) {
            String fileName = profileFile.getName();
            if (fileName.length() >= suffixLength) {
                result.add(fileName.substring(0, fileName.length() - suffixLength));
            }
        }
        return result;
    }

    public static Collection<String> getAvailableProfiles(NbGradleProject project) {
        return getAvailableProfiles(getRootDirectory(project));
    }

    private static File getSettingsDir(File rootDir) {
        if (rootDir == null) throw new NullPointerException("rootDir");
        return new File(rootDir, SETTINGS_DIR_NAME);
    }

    private static File getProfileDirectory(File rootDir) {
        return new File(getSettingsDir(rootDir), PROFILE_DIRECTORY);
    }

    public static File[] getFilesForProfile(File rootDir, String profile) {
        if (rootDir == null) throw new NullPointerException("rootDir");

        File mainFile = new File(rootDir, SETTINGS_FILENAME);

        if (profile == null) {
            return new File[]{mainFile};
        }
        else {
            File profileFile = getProfileDirectory(rootDir);
            profileFile = new File(profileFile, profile + PROFILE_FILE_NAME_SUFFIX);
            return new File[]{profileFile, mainFile};
        }
    }

    public static File[] getFilesForProfile(NbGradleProject project, String profile) {
        return getFilesForProfile(getRootDirectory(project), profile);
    }

    public static File[] getFilesForProject(NbGradleProject project) {
        return getFilesForProfile(project, project.getCurrentProfile().getProfileName());
    }

    public static File getRootDirectory(NbGradleProject project) {
        NbGradleModel model = project.getAvailableModel();
        FileObject settingsFile = model.getSettingsFile();
        FileObject dir = settingsFile != null
                ? settingsFile.getParent()
                : project.getProjectDirectory();
        if (dir == null) {
            dir = project.getProjectDirectory();
        }

        File outputDir = FileUtil.toFile(dir);
        if (outputDir == null) {
            throw new IllegalArgumentException("Cannot get the root directory because the directory is missing: " + dir);
        }
        return outputDir;
    }

    private SettingsFiles() {
        throw new AssertionError();
    }
}
