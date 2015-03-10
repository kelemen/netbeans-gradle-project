package org.netbeans.gradle.project.properties;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties2.ProfileKey;

public final class SettingsFiles {
    private static final String SETTINGS_FILENAME = ".nb-gradle-properties";
    private static final String PROFILE_FILE_NAME_SUFFIX = ".profile";
    private static final String SETTINGS_DIR_NAME = ".nb-gradle";
    private static final String PROFILE_DIRECTORY = "profiles";
    private static final String PRIVATE_SETTINGS_DIR = "private";
    private static final String CACHE_DIR = "cache";

    public static File getPrivateSettingsDir(File rootDir) {
        return new File(getSettingsDir(rootDir), PRIVATE_SETTINGS_DIR);
    }

    public static File getCacheDir(File rootDir) {
        return new File(getPrivateSettingsDir(rootDir), CACHE_DIR);
    }

    public static Collection<ProfileDef> getAvailableProfiles(File rootDir) {
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

        List<ProfileDef> result = new ArrayList<>(profileFiles.length);
        int suffixLength = PROFILE_FILE_NAME_SUFFIX.length();
        for (File profileFile: profileFiles) {
            String fileName = profileFile.getName();
            if (fileName.length() >= suffixLength) {
                String profileName = fileName.substring(0, fileName.length() - suffixLength);
                result.add(new ProfileDef(null, fileName, profileName));
            }
        }
        return result;
    }

    public static ProfileDef getStandardProfileDef(String profileName) {
        if (profileName == null) {
            return null;
        }
        else {
            return new ProfileDef(null, profileName + PROFILE_FILE_NAME_SUFFIX, profileName);
        }
    }

    public static Collection<ProfileDef> getAvailableProfiles(NbGradleProject project) {
        return getAvailableProfiles(getRootDirectory(project));
    }

    private static File getSettingsDir(File rootDir) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");
        return new File(rootDir, SETTINGS_DIR_NAME);
    }

    private static File getProfileDirectory(File rootDir) {
        return new File(getSettingsDir(rootDir), PROFILE_DIRECTORY);
    }

    public static File getProfileFile(File rootDir, ProfileKey profileKey) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");

        if (profileKey != null) {
            File profileFileDir = getProfileDirectory(rootDir);
            String group = profileKey.getGroupName();
            if (group != null) {
                profileFileDir = new File(profileFileDir, group);
            }

            return new File(profileFileDir, profileKey.getFileName());
        }
        else {
            return new File(rootDir, SETTINGS_FILENAME);
        }
    }

    public static File[] getFilesForProfile(File rootDir, ProfileKey profileKey) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");

        File mainFile = new File(rootDir, SETTINGS_FILENAME);

        if (profileKey == null) {
            return new File[]{mainFile};
        }
        else {
            File profileFile = getProfileFile(rootDir, profileKey);
            return new File[]{profileFile, mainFile};
        }
    }

    public static File getProfileFile(NbGradleProject project, ProfileKey profileKey) {
        return getProfileFile(getRootDirectory(project), profileKey);
    }

    public static File getProfileFile(NbGradleProject project, ProfileDef profileDef) {
        return getProfileFile(project, ProfileKey.fromProfileDef(profileDef));
    }

    public static File[] getFilesForProfile(NbGradleProject project, ProfileKey profileKey) {
        return getFilesForProfile(getRootDirectory(project), profileKey);
    }

    public static File[] getFilesForProfile(NbGradleProject project, ProfileDef profileDef) {
        return getFilesForProfile(project, ProfileKey.fromProfileDef(profileDef));
    }

    public static File[] getFilesForProject(NbGradleProject project) {
        return getFilesForProfile(project, project.getCurrentProfile().getProfileDef());
    }

    public static File getRootDirectory(NbGradleProject project) {
        NbGradleModel model = project.getAvailableModel();
        File settingsFile = model.getSettingsFile();
        File dir = settingsFile != null
                ? settingsFile.getParentFile()
                : project.getProjectDirectoryAsFile();
        if (dir == null) {
            dir = project.getProjectDirectoryAsFile();
        }

        return dir;
    }

    private SettingsFiles() {
        throw new AssertionError();
    }
}
