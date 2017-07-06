package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class SettingsFiles {
    private static final Logger LOGGER = Logger.getLogger(SettingsFiles.class.getName());

    private static final String DEFAULT_PROPERTIES_FILENAME = ".nb-gradle-properties";
    private static final String PROFILE_FILE_NAME_SUFFIX = ".profile";
    private static final String SETTINGS_DIR_NAME = ".nb-gradle";
    private static final String PROFILE_DIRECTORY = "profiles";
    private static final String CACHE_DIR = "cache";

    public static Path getPrivateSettingsDir(Path rootDir) {
        return getSettingsDir(rootDir).resolve(ProfileKey.PRIVATE_PROFILE.getGroupName());
    }

    public static Path getCacheDir(Path rootDir) {
        return getPrivateSettingsDir(rootDir).resolve(CACHE_DIR);
    }

    public static Collection<ProfileDef> getAvailableProfiles(Path rootDir) {
        Path profileDir = getProfileDirectory(rootDir);
        if (!Files.isDirectory(profileDir)) {
            return Collections.emptySet();
        }

        List<ProfileDef> result = new ArrayList<>();
        try (DirectoryStream<Path> profileDirContent = Files.newDirectoryStream(profileDir)) {
            int suffixLength = PROFILE_FILE_NAME_SUFFIX.length();
            for (Path file: profileDirContent) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String fileName = NbFileUtils.getFileNameStr(file);
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

    public static Path getSettingsDir(Path rootDir) {
        Objects.requireNonNull(rootDir, "rootDir");
        return rootDir.resolve(SETTINGS_DIR_NAME);
    }

    private static Path getProfileDirectory(Path rootDir) {
        return getSettingsDir(rootDir).resolve(PROFILE_DIRECTORY);
    }

    public static Path getProfileFile(Path rootDir, ProfileKey profileKey) {
        Objects.requireNonNull(rootDir, "rootDir");

        if (profileKey != null) {
            Path profileFileDir = getProfileDirectory(rootDir);
            String group = profileKey.getGroupName();
            if (group != null) {
                profileFileDir = profileFileDir.resolve(group);
            }

            return profileFileDir.resolve(profileKey.getFileName());
        }
        else {
            return rootDir.resolve(DEFAULT_PROPERTIES_FILENAME);
        }
    }

    public static Path[] getFilesForProfile(Path rootDir, ProfileDef profileDef) {
        return getFilesForProfile(rootDir, ProfileKey.fromProfileDef(profileDef));
    }

    public static Path[] getFilesForProfile(Path rootDir, ProfileKey profileKey) {
        Objects.requireNonNull(rootDir, "rootDir");

        Path mainFile = rootDir.resolve(DEFAULT_PROPERTIES_FILENAME);

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
        return getFilesForProfile(project, project.getConfigProvider().getActiveConfiguration().getProfileDef());
    }

    public static Path getRootDirectory(NbGradleProject project) {
        NbGradleModel model = project.currentModel().getValue();
        return model.getSettingsDir();
    }

    private SettingsFiles() {
        throw new AssertionError();
    }
}
