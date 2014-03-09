package org.netbeans.gradle.project.properties2;

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
}
