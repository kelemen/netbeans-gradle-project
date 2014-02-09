package org.netbeans.gradle.project.model;

import org.junit.Test;
import org.netbeans.gradle.model.util.SerializationUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.model.NbGradleProjectTreeTest.createTree;

public class NbGradleMultiProjectDefTest {
    @Test
    public void testSerialization() throws ClassNotFoundException {
        NbGradleProjectTree child1 = createTree("child1");
        NbGradleProjectTree child2 = createTree("child1");

        NbGradleProjectTree root = createTree("testapp", child1, child2);

        NbGradleMultiProjectDef source = new NbGradleMultiProjectDef(root, child1);

        byte[] serialized = SerializationUtils.serializeObject(source);
        NbGradleMultiProjectDef deserialized = (NbGradleMultiProjectDef)SerializationUtils.deserializeObject(serialized);

        assertEquals(source.getProjectDir().toString(), deserialized.getProjectDir().toString());
        assertEquals(
                source.getRootProject().getProjectDir().toString(),
                deserialized.getRootProject().getProjectDir().toString());
    }
}
