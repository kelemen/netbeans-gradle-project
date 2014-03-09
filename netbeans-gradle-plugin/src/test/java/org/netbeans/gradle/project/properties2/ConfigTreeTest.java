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
        assertSame(tree, tree.getDeepChildTree(ConfigPath.ROOT));
        assertSame(tree, tree.getDeepChildTree(new String[0]));
        assertSame(tree, tree.getDeepChildTree(new ConfigKey[0]));
    }

    private void verifyNoChildren(ConfigTree tree) {
        assertTrue("tree.childTrees.isEmpty", tree.getChildTrees().isEmpty());
        assertSame(ConfigTree.EMPTY, tree.getChildTree(new ConfigKey("key1", null)));
        assertSame(ConfigTree.EMPTY, tree.getChildTree("key2"));
        assertSame(ConfigTree.EMPTY, tree.getDeepChildTree("key1"));
        assertSame(ConfigTree.EMPTY, tree.getDeepChildTree("key1", "key2"));
        assertSame(ConfigTree.EMPTY, tree.getDeepChildTree(new ConfigKey("key1", null), new ConfigKey("key2", null)));
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
        ConfigTree.Builder childBuilder = builder.getChildBuilder("child-key0");
        childBuilder.setValue("deep-value");

        ConfigTree.Builder childChildBuilder = builder.getDeepChildBuilder("child-key1", "child-key2");
        childChildBuilder.setValue("deeper-value");

        ConfigTree tree = builder.create();
        verifyTrivial(tree);
        assertTrue(tree.hasValues());
        verifyValue(tree, "my-value");

        Map<ConfigKey, ConfigTree> childTrees = tree.getChildTrees();
        assertEquals(2, childTrees.size());

        ConfigTree childTree1 = childTrees.get(new ConfigKey("child-key0", null));
        assertTrue(childTree1.hasValues());
        verifyTrivial(childTree1);
        verifyValue(childTree1, "deep-value");
        verifyNoChildren(childTree1);

        ConfigTree childTree2 = childTrees.get(new ConfigKey("child-key1", null));
        assertTrue(childTree2.hasValues());
        verifyTrivial(childTree2);
        verifyNoValue(childTree2);

        Map<ConfigKey, ConfigTree> childChildTrees = childTree2.getChildTrees();
        assertEquals(1, childChildTrees.size());

        ConfigTree childTree3 = childChildTrees.get(new ConfigKey("child-key2", null));
        assertTrue(childTree3.hasValues());
        verifyTrivial(childTree3);
        verifyValue(childTree3, "deeper-value");
        verifyNoChildren(childTree3);
    }

    private static void assertTreesEqual(ConfigTree expected, ConfigTree actual) {
        assertEquals(expected.getValue(null), actual.getValue(null));

        Map<ConfigKey, ConfigTree> expectedChildren = expected.getChildTrees();
        Map<ConfigKey, ConfigTree> actualChildren = actual.getChildTrees();
        assertEquals("Children count", expectedChildren.size(), actualChildren.size());

        for (Map.Entry<ConfigKey, ConfigTree> expectedEntry: expectedChildren.entrySet()) {
            ConfigTree actualChild = actualChildren.get(expectedEntry.getKey());
            assertTreesEqual(expectedEntry.getValue(), actualChild);
        }
    }

    private ConfigTree getSinglePath(String value, String... path) {
        assumeBasicBuilderWorks();

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepChildBuilder(path).setValue(value);
        return builder.create();
    }

    @Test
    public void testDeepBuilderWithPath() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigPath path = ConfigPath.fromKeys(new ConfigKey("key1", null), new ConfigKey("key2", null));
        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepChildBuilder(path).setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testDeepBuilderWithKeys() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.getDeepChildBuilder(new ConfigKey("key1", null), new ConfigKey("key2", null)).setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach1() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachChildTreeBuilders();

        builder.getDeepChildBuilder("key1", "key2").setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach2() {
        ConfigTree expected = getSinglePath("my-value", "key1");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachChildTreeBuilders();

        builder.getChildBuilder("key1").setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyAfterDetach3() {
        ConfigTree expected = getSinglePath("my-value", "key1");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachChildTreeBuilders();

        builder.setChildTree(new ConfigKey("key1", null), expected.getChildTree("key1"));
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testModifyValueAfterDetach() {
        ConfigTree expected = getSinglePath("my-value");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.detachChildTreeBuilders();

        builder.setValue("my-value");
        assertTreesEqual(expected, builder.create());
    }

    @Test
    public void testChildModificationIsIgnoredAfterDetach() {
        ConfigTree expected = getSinglePath("my-value", "key1", "key2");

        ConfigTree.Builder builder = new ConfigTree.Builder();
        ConfigTree.Builder childBuilder = builder.getDeepChildBuilder("key1", "key2");
        childBuilder.setValue("my-value");

        builder.detachChildTreeBuilders();

        childBuilder.setValue("ignored-value1");
        childBuilder.getChildBuilder("ignored-key").setValue("ignored-value2");

        assertTreesEqual(expected, builder.create());
    }
}
