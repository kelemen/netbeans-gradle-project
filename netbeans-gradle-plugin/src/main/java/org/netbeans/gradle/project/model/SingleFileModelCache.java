package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.SettingsFiles;

public final class SingleFileModelCache implements PersistentModelCache {
    private static final Logger LOGGER = Logger.getLogger(SingleFileModelCache.class.getName());

    @Override
    public NbGradleModel tryGetModel(NbGradleProject project) throws IOException {
        NbGradleModel model = project.getAvailableModel();
        File rootDir = model.getRootProjectDir();

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

        File rootDir = models.iterator().next().getRootProjectDir();

        Map<String, SerializedNbGradleModels> cachedModels = readFromCache(rootDir);
        Map<String, SerializedNbGradleModels> updatedModels = CollectionUtils.newHashMap(cachedModels.size() + models.size());
        updatedModels.putAll(cachedModels);

        List<NbGradleModel> remainingModels = new LinkedList<NbGradleModel>();
        for (NbGradleModel model: models) {
            if (!rootDir.equals(model.getRootProjectDir())) {
                remainingModels.add(model);
                continue;
            }

            SerializedNbGradleModels serializedModel = SerializedNbGradleModels.tryCreateSerialized(model);
            if (serializedModel != null) {
                updatedModels.put(getCacheKey(model), serializedModel);
            }
            else {
                LOGGER.log(Level.WARNING,
                        "The model of a Gradle project cannot be serialized {0}",
                        model.getProjectDir());
            }
        }

        File cacheFile = getCacheFile(rootDir);
        File cacheDir = cacheFile.getParentFile();
        if (cacheDir != null) {
            cacheDir.mkdirs();
        }
        SerializationUtils.serializeToFile(cacheFile, updatedModels);

        saveGradleModels(remainingModels);
    }

    private static Map<String, SerializedNbGradleModels> tryReadFromCache(File rootDir) throws IOException {
        File cacheFile = getCacheFile(rootDir);
        if (!cacheFile.isFile()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, SerializedNbGradleModels> result
                = (Map<String, SerializedNbGradleModels>)SerializationUtils.deserializeFile(cacheFile);
        return result;
    }

    private static Map<String, SerializedNbGradleModels> readFromCache(File rootDir) throws IOException {
        Map<String, SerializedNbGradleModels> result = tryReadFromCache(rootDir);
        return result != null ? result : Collections.<String, SerializedNbGradleModels>emptyMap();
    }

    private static File getCacheFile(File rootDir) {
        return new File(SettingsFiles.getCacheDir(rootDir), "project-cache");
    }

    private static String getCacheKey(NbGradleModel model) throws IOException {
        File rootDir = model.getRootProjectDir().getCanonicalFile();

        String rootDirStr = rootDir.getPath();
        String projectDirStr = model.getProjectDir().getCanonicalFile().getPath();
        if (projectDirStr.startsWith(rootDirStr)) {
            projectDirStr = projectDirStr.substring(rootDirStr.length());
        }
        return projectDirStr;
    }
}
