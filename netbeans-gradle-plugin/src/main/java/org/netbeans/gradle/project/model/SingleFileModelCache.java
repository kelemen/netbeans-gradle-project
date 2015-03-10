package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.SerializationUtils2;

public final class SingleFileModelCache implements PersistentModelCache {
    @Override
    public NbGradleModel tryGetModel(NbGradleProject project) throws IOException {
        NbGradleModel model = project.getAvailableModel();
        Path rootDir = model.getRootProjectDir().toPath();

        Map<String, SerializedNbGradleModels> allModels = tryReadFromCache(rootDir);
        if (allModels == null) {
            return null;
        }

        String cacheKey = getCacheKey(model);
        SerializedNbGradleModels serializedModels = allModels.get(cacheKey);
        return serializedModels != null
                ? serializedModels.deserializeModel(project)
                : null;
    }

    @Override
    public void saveGradleModels(Collection<NbGradleModel> models) throws IOException {
        if (models.isEmpty()) {
            return;
        }

        Path rootDir = models.iterator().next().getRootProjectDir().toPath();

        Map<String, SerializedNbGradleModels> cachedModels = readFromCache(rootDir);
        Map<String, SerializedNbGradleModels> updatedModels = CollectionUtils.newHashMap(cachedModels.size() + models.size());
        updatedModels.putAll(cachedModels);

        List<NbGradleModel> remainingModels = new LinkedList<>();
        for (NbGradleModel model: models) {
            if (!rootDir.equals(model.getRootProjectDir().toPath())) {
                remainingModels.add(model);
                continue;
            }

            SerializedNbGradleModels serializedModel = SerializedNbGradleModels.createSerialized(model);
            updatedModels.put(getCacheKey(model), serializedModel);
        }

        Path cacheFile = getCacheFile(rootDir);
        Path cacheDir = cacheFile.getParent();
        if (cacheDir != null) {
            Files.createDirectories(cacheDir);
        }
        SerializationUtils2.serializeToFile(cacheFile, updatedModels);

        saveGradleModels(remainingModels);
    }

    private static Map<String, SerializedNbGradleModels> tryReadFromCache(Path rootDir) throws IOException {
        Path cacheFile = getCacheFile(rootDir);
        if (!Files.isRegularFile(cacheFile)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, SerializedNbGradleModels> result
                = (Map<String, SerializedNbGradleModels>)SerializationUtils2.deserializeFile(cacheFile);
        return result;
    }

    private static Map<String, SerializedNbGradleModels> readFromCache(Path rootDir) throws IOException {
        Map<String, SerializedNbGradleModels> result = tryReadFromCache(rootDir);
        return result != null ? result : Collections.<String, SerializedNbGradleModels>emptyMap();
    }

    private static Path getCacheFile(Path rootDir) {
        return SettingsFiles.getCacheDir(rootDir).resolve("project-cache");
    }

    public static String getCacheKey(NbGradleModel model) throws IOException {
        File rootDir = model.getRootProjectDir().getCanonicalFile();

        String rootDirStr = rootDir.getPath();
        String projectDirStr = model.getProjectDir().getCanonicalFile().getPath();
        if (projectDirStr.startsWith(rootDirStr)) {
            projectDirStr = projectDirStr.substring(rootDirStr.length());
        }
        return projectDirStr;
    }
}
