package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.SerializationUtils2;
import org.netbeans.gradle.project.util.StringUtils;

public final class MultiFileModelCache implements PersistentModelCache<NbGradleModel> {
    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to load the MD5 calculator.", ex);
        }
    }

    @Override
    public NbGradleModel tryGetModel(NbGradleProject project, Path rootProjectDir) throws IOException {
        Path cacheFilePath = getCacheFilePath(
                rootProjectDir,
                project.getProjectDirectoryAsFile(),
                getMD5());

        if (!Files.isRegularFile(cacheFilePath)) {
            return null;
        }

        SerializedNbGradleModels serializedModel
                = (SerializedNbGradleModels)SerializationUtils2.deserializeFile(cacheFilePath);
        return serializedModel != null
                ? serializedModel.deserializeModel(project)
                : null;
    }

    @Override
    public void saveGradleModels(Collection<? extends NbGradleModel> models) throws IOException {
        MessageDigest hashCalculator = getMD5();

        for (NbGradleModel model: models) {
            saveGradleModel(model, hashCalculator);
        }
    }

    private void saveGradleModel(NbGradleModel model, MessageDigest hashCalculator) throws IOException {
        SerializedNbGradleModels toSave = SerializedNbGradleModels.createSerialized(model);
        saveGradleModel(model, toSave, hashCalculator);
    }

    private static String limitLength(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private static String getCacheKey(Path rootProjectDir, File projectDir) throws IOException {
        Path rootDir = rootProjectDir.normalize();

        String rootDirStr = rootDir.toString();
        String projectDirStr = projectDir.getCanonicalFile().getPath();
        if (projectDirStr.startsWith(rootDirStr)) {
            projectDirStr = projectDirStr.substring(rootDirStr.length());
        }
        return projectDirStr;
    }

    private static String getCacheFileName(
            Path rootProjectDir,
            File projectDir,
            MessageDigest hashCalculator) throws IOException {

        String cacheKey = getCacheKey(rootProjectDir, projectDir);

        // We do this to limit the key length and make it usable as part of a file name.
        hashCalculator.reset();
        String keyHash = StringUtils.byteArrayToHex(hashCalculator.digest(cacheKey.getBytes(StringUtils.UTF8)));
        return limitLength(projectDir.getName(), 16) + "-" + keyHash;
    }

    private static Path getCacheFilePath(NbGradleModel model, MessageDigest hashCalculator) throws IOException {
        return getCacheFilePath(model.getSettingsDir(), model.getProjectDir(), hashCalculator);
    }

    private static Path getCacheFilePath(
            Path rootProjectDir,
            File projectDir,
            MessageDigest hashCalculator) throws IOException {

        String fileName = getCacheFileName(rootProjectDir, projectDir, hashCalculator);

        return SettingsFiles.getCacheDir(rootProjectDir).resolve(fileName);
    }

    private void saveGradleModel(
            NbGradleModel sourceModel,
            SerializedNbGradleModels model,
            MessageDigest hashCalculator) throws IOException {

        Path cacheFilePath = getCacheFilePath(sourceModel, hashCalculator);

        Path cacheDir = cacheFilePath.getParent();
        if (cacheDir != null) {
            Files.createDirectories(cacheDir);
        }

        SerializationUtils2.serializeToFile(cacheFilePath, model);
    }
}
