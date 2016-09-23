package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.gradle.project.util.StringUtils;

public final class MultiFileModelCache<T> implements PersistentModelCache<T> {
    private final ModelPersister<T> modelPersister;
    private final NbFunction<? super T, ? extends PersistentModelKey> modelKeyFactory;

    public MultiFileModelCache(
            ModelPersister<T> modelPersister,
            NbFunction<? super T, ? extends PersistentModelKey> modelKeyFactory) {
        ExceptionHelper.checkNotNullArgument(modelPersister, "modelPersister");
        ExceptionHelper.checkNotNullArgument(modelKeyFactory, "modelKeyFactory");

        this.modelPersister = modelPersister;
        this.modelKeyFactory = modelKeyFactory;
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

            Path cacheDir = cacheFilePath.getParent();
            if (cacheDir != null) {
                Files.createDirectories(cacheDir);
            }

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
        return limitLength(modelKey.getProjectDir().getFileName().toString(), 16) + "-" + keyHash;
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
