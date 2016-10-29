package org.netbeans.gradle.project.model;

import java.nio.file.Paths;
import org.junit.Test;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.model.NbGradleMultiProjectDefTest.*;

public class NbGenericModelInfoTest {
    @Test
    public void testSerialization() throws ClassNotFoundException {
        NbGradleMultiProjectDef projectDef = createTestMultiProject();
        NbGenericModelInfo source = new NbGenericModelInfo(projectDef, Paths.get("settings.gradle"));

        byte[] serialized = SerializationUtils.serializeObject(source);
        NbGenericModelInfo deserialized = (NbGenericModelInfo)SerializationUtils.deserializeObject(serialized, SerializationCache.NO_CACHE);

        assertEquals(source.getProjectDir().toString(), deserialized.getProjectDir().toString());
        assertEquals(
                source.getSettingsFile().toString(),
                deserialized.getSettingsFile().toString());
    }
}
