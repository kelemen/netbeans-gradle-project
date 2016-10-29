package org.netbeans.gradle.project.model;

import org.junit.Test;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.model.NbGradleProjectTreeTest.*;

public class NbGradleMultiProjectDefTest {
    public static NbGradleMultiProjectDef createTestMultiProject() {
        NbGradleProjectTree child1 = createTree("child1");
        NbGradleProjectTree child2 = createTree("child1");

        NbGradleProjectTree root = createTree("testapp", child1, child2);

        return new NbGradleMultiProjectDef(root, child1);
    }

    @Test
    public void testSerialization() throws ClassNotFoundException {
        NbGradleMultiProjectDef source = createTestMultiProject();

        byte[] serialized = SerializationUtils.serializeObject(source);
        NbGradleMultiProjectDef deserialized = (NbGradleMultiProjectDef)SerializationUtils.deserializeObject(serialized, SerializationCache.NO_CACHE);

        assertEquals(source.getProjectDir().toString(), deserialized.getProjectDir().toString());
        assertEquals(
                source.getRootProject().getProjectDir().toString(),
                deserialized.getRootProject().getProjectDir().toString());
    }
}
