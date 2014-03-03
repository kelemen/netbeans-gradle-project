package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.StringUtils;

public final class MultiFileModelCache implements PersistentModelCache {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to load the MD5 calculator.", ex);
        }
    }

    @Override
    public NbGradleModel tryGetModel(NbGradleProject project) throws IOException {
        NbGradleModel currentModel = project.getAvailableModel();
        File cacheFilePath = getCacheFilePath(currentModel, getMD5());

        if (!cacheFilePath.isFile()) {
            return null;
        }

        SerializedNbGradleModels serializedModel
                = (SerializedNbGradleModels)SerializationUtils.deserializeFile(cacheFilePath);
        return serializedModel != null
                ? serializedModel.deserializeModel(project)
                : null;
    }

    @Override
    public void saveGradleModels(Collection<NbGradleModel> models) throws IOException {
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

    private static String getCacheFileName(NbGradleModel model, MessageDigest hashCalculator) throws IOException {
        String cacheKey = SingleFileModelCache.getCacheKey(model);

        // We do this to limit the key length and make it usable as part of a file name.
        hashCalculator.reset();
        String keyHash = StringUtils.byteArrayToHex(hashCalculator.digest(cacheKey.getBytes(UTF8)));
        return limitLength(model.getProjectDir().getName(), 16) + "-" + keyHash;
    }

    private static File getCacheFilePath(NbGradleModel model, MessageDigest hashCalculator) throws IOException {
        String fileName = getCacheFileName(model, hashCalculator);
        return new File(SettingsFiles.getCacheDir(model.getRootProjectDir()), fileName);
    }

    private void saveGradleModel(
            NbGradleModel sourceModel,
            SerializedNbGradleModels model,
            MessageDigest hashCalculator) throws IOException {

        File cacheFilePath = getCacheFilePath(sourceModel, hashCalculator);

        File cacheDir = cacheFilePath.getParentFile();
        if (cacheDir != null) {
            cacheDir.mkdirs();
        }

        SerializationUtils.serializeToFile(cacheFilePath, model);
    }
}
