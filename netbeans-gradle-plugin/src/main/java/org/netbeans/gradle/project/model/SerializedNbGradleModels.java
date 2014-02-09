package org.netbeans.gradle.project.model;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;

public final class SerializedNbGradleModels implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SerializedNbGradleModels.class.getName());

    private final NbGenericModelInfo genericInfo;
    // Maps extension name to serialized extension model
    private final Map<String, byte[]> extensionModels;

    private SerializedNbGradleModels(
            NbGenericModelInfo genericInfo,
            Map<String, byte[]> extensionModels) {

        assert genericInfo != null;
        assert extensionModels != null;

        this.genericInfo = genericInfo;
        this.extensionModels = extensionModels;
    }

    public static SerializedNbGradleModels tryCreateSerialized(NbGradleModel model) {
        Map<String, Object> extensionModels = model.getExtensionModels();
        Map<String, byte[]> serializedModels = CollectionUtils.newHashMap(extensionModels.size());

        for (Map.Entry<String, Object> entry: extensionModels.entrySet()) {
            String extensionName = entry.getKey();
            Object extensionModel = entry.getValue();
            if (extensionModel == null) {
                serializedModels.put(extensionName, null);
                continue;
            }

            if (!(extensionModel instanceof Serializable)) {
                return null;
            }
            byte[] serializedModel;
            try {
                serializedModel = SerializationUtils.serializeObject(extensionModel);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "There was a problem serializing " + extensionModel, ex);
                return null;
            }

            serializedModels.put(extensionName, serializedModel);
        }

        return new SerializedNbGradleModels(model.getGenericInfo(), serializedModels);
    }

    public NbGradleModel deserializeModel(NbGradleProject ownerProject) {
        Map<String, Object> deserializedModels = CollectionUtils.newHashMap(extensionModels.size());

        for (NbGradleExtensionRef extensionRef: ownerProject.getExtensionRefs()) {
            byte[] serializedModel = extensionModels.get(extensionRef.getName());

            try {
                ClassLoader modelClassLoader = extensionRef.getExtensionDef().getClass().getClassLoader();
                Object model = SerializationUtils.deserializeObject(serializedModel, modelClassLoader);
                deserializedModels.put(extensionRef.getName(), model);
            } catch (Throwable ex) {
                LOGGER.log(Level.INFO,
                        "There was a problem when deserializing model for " + extensionRef.getName(),
                        ex);
            }
        }

        return new NbGradleModel(genericInfo, deserializedModels);
    }
}
