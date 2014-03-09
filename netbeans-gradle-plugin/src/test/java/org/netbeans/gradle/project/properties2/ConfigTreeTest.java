package org.netbeans.gradle.project.properties2;

import java.util.Iterator;
import java.util.List;
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

    private void verifyValueWithoutChildren(ConfigTree tree, String value) {
        assertEquals("tree.hasValues", value != null, tree.hasValues());
        verifyValue(tree, value);
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

    private static <K, E> E getSingle(Map<K, List<E>> map, K key) {
        List<E> values = map.get(key);
        if (values == null) {
            throw new AssertionError("Expected to have key: " + key);
        }

        if (values.size() != 1) {
            fail("Expected to have exactly one value for key " + key + " instead of " + values.size());
        }

        return values.get(0);
    }

    @Test
    public void testBasicBuilder() {
        ConfigTree.Builder builder = new ConfigTree.Builder();

        builder.setValue("my-value");
        ConfigTree.Builder childBuilder = builder.getChildBuilder("child-key0");
        childBuilder.setValue("deep-value");

        ConfigTree.Builder childChildBuilder = builder.getDeepChildBuilder("child-key1", "child-key2");
        childChildBuilder.setValue("deeper-value");

        builder.addChildBuilder("list-key").setValue("list-value0");
        builder.addChildBuilder("list-key").setValue("list-value1");

        ConfigTree tree = builder.create();
        verifyTrivial(tree);
        assertTrue(tree.hasValues());
        verifyValue(tree, "my-value");

        Map<ConfigKey, List<ConfigTree>> childTrees = tree.getChildTrees();
        assertEquals(3, childTrees.size());

        List<ConfigTree> childList = childTrees.get(new ConfigKey("list-key", null));
        assertEquals(2, childList.size());
        verifyValueWithoutChildren(childList.get(0), "list-value0");
        verifyValueWithoutChildren(childList.get(1), "list-value1");

        ConfigTree childTree1 = getSingle(childTrees, new ConfigKey("child-key0", null));
        verifyValueWithoutChildren(childTree1, "deep-value");

        ConfigTree childTree2 = getSingle(childTrees, new ConfigKey("child-key1", null));
        assertTrue(childTree2.hasValues());
        verifyTrivial(childTree2);
        verifyNoValue(childTree2);

        Map<ConfigKey, List<ConfigTree>> childChildTrees = childTree2.getChildTrees();
        assertEquals(1, childChildTrees.size());

        ConfigTree childTree3 = getSingle(childChildTrees, new ConfigKey("child-key2", null));
        verifyValueWithoutChildren(childTree3, "deeper-value");
    }

    private static void assertTreesEqual(ConfigTree expected, ConfigTree actual) {
        assertEquals(expected.getValue(null), actual.getValue(null));

        Map<ConfigKey, List<ConfigTree>> expectedChildren = expected.getChildTrees();
        Map<ConfigKey, List<ConfigTree>> actualChildren = actual.getChildTrees();
        assertEquals("Children count", expectedChildren.size(), actualChildren.size());

        for (Map.Entry<ConfigKey, List<ConfigTree>> expectedEntry: expectedChildren.entrySet()) {
            ConfigKey key = expectedEntry.getKey();

            List<ConfigTree> expectedValues = expectedEntry.getValue();
            List<ConfigTree> actualChild = actualChildren.get(key);
            assertEquals("Children count for " + key, expectedValues.size(), actualChild.size());

            Iterator<ConfigTree> expectedValuesItr = expectedValues.iterator();
            Iterator<ConfigTree> actualChildItr = actualChild.iterator();
            while (expectedValuesItr.hasNext()) {
                assertTreesEqual(expectedValuesItr.next(), actualChildItr.next());
            }
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

    @Test
    public void testDoesntHaveValueWithDeepTree() {
        ConfigTree tree = getSinglePath(null, "key1", "key2");
        if (tree.hasValues()) {
            fail("Not expected to have value: " + tree);
        }
    }

    private void verifyEquals(ConfigTree tree1, ConfigTree tree2) {
        if (!tree1.equals(tree2)) {
            fail("tree1.equals(tree2) must be true. Tree1 = " + tree1 + ", Tree2 = " + tree2);
        }

        if (!tree2.equals(tree1)) {
            fail("tree2.equals(tree1) must be true. Tree1 = " + tree1 + ", Tree2 = " + tree2);
        }

        if (tree1.hashCode() != tree2.hashCode()) {
            fail("The hash code for equivalent trees must match. Tree1 = " + tree1 + ", Tree2 = " + tree2);
        }
    }

    @Test
    public void testEqualsForSame() {
        ConfigTree.Builder builder = new ConfigTree.Builder();
        builder.setValue("value-testEqualsForSame");
        ConfigTree tree = builder.create();

        verifyEquals(tree, tree);
    }

    @Test
    public void testEqualsWithValueOnly() {
        assumeBasicBuilderWorks();

        String value = "value-testEqualsWithOnlyValues";
        verifyEquals(getSinglePath(value), getSinglePath(value));
    }

    @Test
    public void testEqualsWithDeepValue() {
        assumeBasicBuilderWorks();

        String value = "value-testEqualsWithOnlyValues";
        String[] path = {"key1", "key2"};
        verifyEquals(getSinglePath(value, path), getSinglePath(value, path));
    }
}
