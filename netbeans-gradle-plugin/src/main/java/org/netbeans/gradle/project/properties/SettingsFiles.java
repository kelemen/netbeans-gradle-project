package org.netbeans.gradle.project.properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties2.ProfileKey;

public final class SettingsFiles {
    private static final Logger LOGGER = Logger.getLogger(SettingsFiles.class.getName());

    private static final String SETTINGS_FILENAME = ".nb-gradle-properties";
    private static final String PROFILE_FILE_NAME_SUFFIX = ".profile";
    private static final String SETTINGS_DIR_NAME = ".nb-gradle";
    private static final String PROFILE_DIRECTORY = "profiles";
    private static final String PRIVATE_SETTINGS_DIR = "private";
    private static final String CACHE_DIR = "cache";

    public static Path getPrivateSettingsDir(Path rootDir) {
        return getSettingsDir(rootDir).resolve(PRIVATE_SETTINGS_DIR);
    }

    public static Path getCacheDir(Path rootDir) {
        return getPrivateSettingsDir(rootDir).resolve(CACHE_DIR);
    }

    public static Collection<ProfileDef> getAvailableProfiles(Path rootDir) {
        Path profileDir = getProfileDirectory(rootDir);
        if (!Files.isDirectory(profileDir)) {
            return Collections.emptySet();
        }

        List<ProfileDef> result = new LinkedList<>();
        try (DirectoryStream<Path> profileDirContent = Files.newDirectoryStream(profileDir)) {
            int suffixLength = PROFILE_FILE_NAME_SUFFIX.length();
            for (Path file: profileDirContent) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String fileName = file.getFileName().toString();
                String normFileName = fileName.toLowerCase(Locale.ROOT);
                if (!normFileName.endsWith(PROFILE_FILE_NAME_SUFFIX)) {
                    continue;
                }

                // This should hold, but check it just in case I don't know
                // something about weird case issues.
                if (fileName.length() >= suffixLength) {
                    String profileName = fileName.substring(0, fileName.length() - suffixLength);
                    result.add(new ProfileDef(null, fileName, profileName));
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Cannot list profile directory: " + profileDir, ex);
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

    private static Path getSettingsDir(Path rootDir) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");
        return rootDir.resolve(SETTINGS_DIR_NAME);
    }

    private static Path getProfileDirectory(Path rootDir) {
        return getSettingsDir(rootDir).resolve(PROFILE_DIRECTORY);
    }

    public static Path getProfileFile(Path rootDir, ProfileKey profileKey) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");

        if (profileKey != null) {
            Path profileFileDir = getProfileDirectory(rootDir);
            String group = profileKey.getGroupName();
            if (group != null) {
                profileFileDir = profileFileDir.resolve(group);
            }

            return profileFileDir.resolve(profileKey.getFileName());
        }
        else {
            return rootDir.resolve(SETTINGS_FILENAME);
        }
    }

    public static Path[] getFilesForProfile(Path rootDir, ProfileDef profileDef) {
        return getFilesForProfile(rootDir, ProfileKey.fromProfileDef(profileDef));
    }

    public static Path[] getFilesForProfile(Path rootDir, ProfileKey profileKey) {
        ExceptionHelper.checkNotNullArgument(rootDir, "rootDir");

        Path mainFile = rootDir.resolve(SETTINGS_FILENAME);

        if (profileKey == null) {
            return new Path[]{mainFile};
        }
        else {
            Path profileFile = getProfileFile(rootDir, profileKey);
            return new Path[]{profileFile, mainFile};
        }
    }

    public static Path getProfileFile(NbGradleProject project, ProfileKey profileKey) {
        return getProfileFile(getRootDirectory(project), profileKey);
    }

    public static Path getProfileFile(NbGradleProject project, ProfileDef profileDef) {
        return getProfileFile(project, ProfileKey.fromProfileDef(profileDef));
    }

    public static Path[] getFilesForProfile(NbGradleProject project, ProfileKey profileKey) {
        return getFilesForProfile(getRootDirectory(project), profileKey);
    }

    public static Path[] getFilesForProfile(NbGradleProject project, ProfileDef profileDef) {
        return getFilesForProfile(project, ProfileKey.fromProfileDef(profileDef));
    }

    public static Path[] getFilesForProject(NbGradleProject project) {
        return getFilesForProfile(project, project.getCurrentProfile().getProfileDef());
    }

    public static Path getRootDirectory(NbGradleProject project) {
        NbGradleModel model = project.getAvailableModel();
        File settingsFile = model.getSettingsFile();
        File dir = settingsFile != null
                ? settingsFile.getParentFile()
                : project.getProjectDirectoryAsFile();
        if (dir == null) {
            dir = project.getProjectDirectoryAsFile();
        }

        return dir.toPath();
    }

    private SettingsFiles() {
        throw new AssertionError();
    }
}
