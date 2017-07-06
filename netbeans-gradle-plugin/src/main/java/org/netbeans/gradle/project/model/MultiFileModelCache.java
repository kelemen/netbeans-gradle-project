package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;

public final class MultiFileModelCache<T> implements PersistentModelCache<T> {
    private final PersistentModelStore<T> modelPersister;
    private final Function<? super T, ? extends PersistentModelKey> modelKeyFactory;

    public MultiFileModelCache(
            PersistentModelStore<T> modelPersister,
            Function<? super T, ? extends PersistentModelKey> modelKeyFactory) {
        this.modelPersister = Objects.requireNonNull(modelPersister, "modelPersister");
        this.modelKeyFactory = Objects.requireNonNull(modelKeyFactory, "modelKeyFactory");
    }

    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to load the MD5 calculator.", ex);
        }
    }

    @Override
    public T tryGetModel(PersistentModelKey modelKey) throws IOException {
        Path cacheFilePath = getCacheFilePath(modelKey, getMD5());
        return modelPersister.tryLoadModel(cacheFilePath);
    }

    @Override
    public void saveGradleModels(Collection<? extends T> models) throws IOException {
        MessageDigest hashCalculator = getMD5();

        for (T model: models) {
            Path cacheFilePath = getCacheFilePath(model, hashCalculator);
            modelPersister.persistModel(model, cacheFilePath);
        }
    }

    private static String limitLength(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private static String getCacheKey(PersistentModelKey modelKey) throws IOException {
        Path rootDir = modelKey.getRootPath();

        String rootDirStr = rootDir.toString();
        String projectDirStr = modelKey.getProjectDir().toString();
        if (projectDirStr.startsWith(rootDirStr)) {
            projectDirStr = projectDirStr.substring(rootDirStr.length());
        }
        return projectDirStr;
    }

    private static String getCacheFileName(
            PersistentModelKey modelKey,
            MessageDigest hashCalculator) throws IOException {

        String cacheKey = getCacheKey(modelKey);

        // We do this to limit the key length and make it usable as part of a file name.
        hashCalculator.reset();
        String keyHash = StringUtils.byteArrayToHex(hashCalculator.digest(cacheKey.getBytes(StringUtils.UTF8)));
        return limitLength(NbFileUtils.getFileNameStr(modelKey.getProjectDir()), 16) + "-" + keyHash;
    }

    private Path getCacheFilePath(T model, MessageDigest hashCalculator) throws IOException {
        PersistentModelKey modelKey = modelKeyFactory.apply(model);
        return getCacheFilePath(modelKey, hashCalculator);
    }

    private static Path getCacheFilePath(
            PersistentModelKey modelKey,
            MessageDigest hashCalculator) throws IOException {
        String fileName = getCacheFileName(modelKey, hashCalculator);
        return SettingsFiles.getCacheDir(modelKey.getRootPath()).resolve(fileName);
    }
}
