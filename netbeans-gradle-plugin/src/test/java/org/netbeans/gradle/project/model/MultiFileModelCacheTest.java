package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultiFileModelCacheTest {
    private static MultiFileModelCache<TestModel> getTestCache(PersistentModelStore<TestModel> persister) {
        return new MultiFileModelCache<>(persister, TestModel::getKey);
    }

    private static PersistentModelKey getKey(String rootName, String subName) {
        return new TestModel(rootName, subName).getKey();
    }

    @Test
    public void testGetNonExistant() throws Exception {
        MemPersistentModelStore<TestModel> persister = new MemPersistentModelStore<>();
        MultiFileModelCache<TestModel> cache = getTestCache(persister);

        TestModel model = cache.tryGetModel(getKey("TestRoot", "TestSub"));
        assertNull(model);
    }

    @Test
    public void testSave1Get1() throws Exception {
        MemPersistentModelStore<TestModel> persister = new MemPersistentModelStore<>();
        MultiFileModelCache<TestModel> cache = getTestCache(persister);

        TestModel model = new TestModel("TestRoot", "TestSub");
        PersistentModelKey key = model.getKey();

        assertEquals("saved model count", 0, persister.getSavedModels().size());
        cache.saveGradleModels(Collections.singleton(model));
        assertEquals("saved model count", 1, persister.getSavedModels().size());

        assertSame(model, cache.tryGetModel(key));
    }

    @Test
    public void testSaveMultipleGet1() throws Exception {
        MemPersistentModelStore<TestModel> persister = new MemPersistentModelStore<>();
        MultiFileModelCache<TestModel> cache = getTestCache(persister);

        TestModel model1 = new TestModel("TestRoot1", "TestSub1");
        PersistentModelKey key1 = model1.getKey();

        TestModel model2 = new TestModel("TestRoot2", "TestSub2");
        PersistentModelKey key2 = model2.getKey();

        assertEquals("saved model count", 0, persister.getSavedModels().size());
        cache.saveGradleModels(Arrays.asList(model1, model2));
        assertEquals("saved model count", 2, persister.getSavedModels().size());

        assertSame(model1, cache.tryGetModel(key1));
        assertSame(model2, cache.tryGetModel(key2));
    }

    @Test
    public void testOverwrite() throws Exception {
        MemPersistentModelStore<TestModel> persister = new MemPersistentModelStore<>();
        MultiFileModelCache<TestModel> cache = getTestCache(persister);

        TestModel model1 = new TestModel("TestRoot", "TestSub");
        TestModel model2 = new TestModel("TestRoot", "TestSub");
        PersistentModelKey key = model1.getKey();

        cache.saveGradleModels(Collections.singleton(model1));
        cache.saveGradleModels(Collections.singleton(model2));
        assertEquals("saved model count", 1, persister.getSavedModels().size());

        assertSame(model2, cache.tryGetModel(key));
    }

    private static final class TestModel {
        private final String rootName;
        private final String subName;

        public TestModel(String rootName, String subName) {
            this.rootName = rootName;
            this.subName = subName;
        }

        public PersistentModelKey getKey() {
            Path root = Paths.get(rootName);
            return new PersistentModelKey(root, root.resolve(subName));
        }

        @Override
        public String toString() {
            return "TestModel{" + "rootName=" + rootName + ", subName=" + subName + '}';
        }
    }
}
