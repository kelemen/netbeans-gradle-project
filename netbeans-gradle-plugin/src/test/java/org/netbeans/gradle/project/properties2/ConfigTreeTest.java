package org.netbeans.gradle.project.properties2;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTreeTest {
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
}
