package org.netbeans.gradle.project.properties2;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTreeTest {
    private final AtomicReference<Boolean> basicBuilderWorks = new AtomicReference<>(null);

    private void assumeBasicBuilderWorks() {
        Boolean works = basicBuilderWorks.get();
        if (works == null) {
            works = false;
            try {
                testBasicBuilder();
                works = true;
            } catch (Throwable ex) {
            }

            basicBuilderWorks.compareAndSet(null, works);
        }
        Assume.assumeTrue(works);
    }

    private void verifyTrivial(ConfigTree tree) {
        assertSame(tree, tree.getDeepSubTree(ConfigPath.ROOT));
        assertSame(tree, tree.getDeepSubTree(new String[0]));
        assertSame(tree, tree.getDeepSubTree(new ConfigKey[0]));
    }

    private void verifyNoChildren(ConfigTree tree) {
        assertTrue("tree.subTrees.isEmpty", tree.getSubTrees().isEmpty());
        assertSame(ConfigTree.EMPTY, tree.getSubTree(new ConfigKey("key1", null)));
        assertSame(ConfigTree.EMPTY, tree.getSubTree("key2"));
        assertSame(ConfigTree.EMPTY, tree.getDeepSubTree("key1"));
        assertSame(ConfigTree.EMPTY, tree.getDeepSubTree("key1", "key2"));
        assertSame(ConfigTree.EMPTY, tree.getDeepSubTree(new ConfigKey("key1", null), new ConfigKey("key2", null)));
    }

    private void verifyNoValue(ConfigTree tree) {
        assertNull(tree.getValue(null));
        assertEquals("my-default", tree.getValue("my-default"));
    }

    private void verifyValue(ConfigTree tree, String value) {
        if (value == null) {
            verifyNoValue(tree);
        }
        else {
            assertEquals(value, tree.getValue(null));
            assertEquals(value, tree.getValue("WRONG-" + value));
        }
    }

    private void verifyEmpty(ConfigTree tree) {
        verifyNoValue(tree);
        assertFalse("ConfigTree.hasValues", tree.hasValues());

        verifyNoChildren(tree);
        verifyTrivial(tree);
    }

    @Test
    public void testEmptyConstant() {
        verifyEmpty(ConfigTree.EMPTY);
    }

    @Test
    public void testSingleValueByBuilder() {
        final String value = "my-value";

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.setValue(value);

        ConfigTree tree = builder.create();
        assertTrue(tree.hasValues());

        verifyValue(tree, value);
        verifyNoChildren(tree);
    }

    @Test
    public void testSingleValueByFactoryNull() {
        assertSame(ConfigTree.EMPTY, ConfigTree.singleValue(null));
    }

    @Test
    public void testSingleValueByFactory() {
        final String value = "my-value";

        ConfigTree tree = ConfigTree.singleValue(value);
        assertTrue(tree.hasValues());

        verifyValue(tree, value);
        verifyNoChildren(tree);
    }

    @Test
    public void testBasicBuilder() {
        ConfigTree.Builder builder = new ConfigTree.Builder();

        builder.setValue("my-value");
        ConfigTree.Builder subBuilder = builder.getSubBuilder("sub-key0");
        subBuilder.setValue("deep-value");

        ConfigTree.Builder subSubBuilder = builder.getDeepSubBuilder("sub-key1", "sub-key2");
        subSubBuilder.setValue("deeper-value");

        ConfigTree tree = builder.create();
        verifyTrivial(tree);
        assertTrue(tree.hasValues());
        verifyValue(tree, "my-value");

        Map<ConfigKey, ConfigTree> subTrees = tree.getSubTrees();
        assertEquals(2, subTrees.size());

        ConfigTree subTree1 = subTrees.get(new ConfigKey("sub-key0", null));
        assertTrue(subTree1.hasValues());
        verifyTrivial(subTree1);
        verifyValue(subTree1, "deep-value");
        verifyNoChildren(subTree1);

        ConfigTree subTree2 = subTrees.get(new ConfigKey("sub-key1", null));
        assertTrue(subTree2.hasValues());
        verifyTrivial(subTree2);
        verifyNoValue(subTree2);

        Map<ConfigKey, ConfigTree> subSubTrees = subTree2.getSubTrees();
        assertEquals(1, subSubTrees.size());

        ConfigTree subTree3 = subSubTrees.get(new ConfigKey("sub-key2", null));
        assertTrue(subTree3.hasValues());
        verifyTrivial(subTree3);
        verifyValue(subTree3, "deeper-value");
        verifyNoChildren(subTree3);
    }

    private static void assertTreesEqual(ConfigTree expected, ConfigTree actual) {
        assertEquals(expected.getValue(null), actual.getValue(null));

        Map<ConfigKey, ConfigTree> expectedChildren = expected.getSubTrees();
        Map<ConfigKey, ConfigTree> actualChildren = actual.getSubTrees();
        assertEquals("Children count", expectedChildren.size(), actualChildren.size());

        for (Map.Entry<ConfigKey, ConfigTree> expectedEntry: expectedChildren.entrySet()) {
            ConfigTree actualChild = actualChildren.get(expectedEntry.getKey());
            assertTreesEqual(expectedEntry.getValue(), actualChild);
        }
    }

    private ConfigTree getSinglePath(String value, String... path) {
        assumeBasicBuilderWorks();

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepSubBuilder(path).setValue(value);
        return builder.create();
    }

    @Test
    public void testDeepBuilderWithPath() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigPath path = ConfigPath.fromKeys(new ConfigKey("key1", null), new ConfigKey("key2", null));
        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepSubBuilder(path).setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testDeepBuilderWithKeys() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepSubBuilder(new ConfigKey("key1", null), new ConfigKey("key2", null)).setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach1() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachSubTreeBuilders();

        builder.getDeepSubBuilder("key1", "key2").setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach2() {
        ConfigTree expected = getSinglePath("my-value", "key1");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachSubTreeBuilders();

        builder.getSubBuilder("key1").setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach3() {
        ConfigTree expected = getSinglePath("my-value", "key1");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachSubTreeBuilders();

        builder.setChildTree(new ConfigKey("key1", null), expected.getSubTree("key1"));
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyValueAfterDetach() {
        ConfigTree expected = getSinglePath("my-value");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachSubTreeBuilders();

        builder.setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testChildModificationIsIgnoredAfterDetach() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        ConfigTree.Builder subBuilder = builder.getDeepSubBuilder("key1", "key2");
        subBuilder.setValue("my-value");

        builder.detachSubTreeBuilders();

        subBuilder.setValue("ignored-value1");
        subBuilder.getSubBuilder("ignored-key").setValue("ignored-value2");

        assertTreesEqual(expected, builder.create());
    }
}
