package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.SerializationUtils;

import static org.junit.Assert.*;

public class NbGradleProjectTreeTest {
    private static NbGradleProjectTree createTree(String name, NbGradleProjectTree... children) {
        String fullName = ":app:" + name;
        GenericProjectProperties properties = new GenericProjectProperties(
                name,
                fullName,
                new File(name),
                new File("build.gradle"));

        List<GradleTaskID> tasks = Arrays.asList(new GradleTaskID("run", fullName + ":run"));
        return new NbGradleProjectTree(properties, tasks, Arrays.asList(children));
    }

    @Test
    public void testSerialization() throws ClassNotFoundException {
        NbGradleProjectTree child1 = createTree("child1");
        NbGradleProjectTree child2 = createTree("child1");

        NbGradleProjectTree source = createTree("testapp", child1, child2);

        byte[] serialized = SerializationUtils.serializeObject(source);
        NbGradleProjectTree deserialized = (NbGradleProjectTree)SerializationUtils.deserializeObject(serialized);

        assertEquals(source.getProjectDir().toString(), deserialized.getProjectDir().toString());
    }
}
