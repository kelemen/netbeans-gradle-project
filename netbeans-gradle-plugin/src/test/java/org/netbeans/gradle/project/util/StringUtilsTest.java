package org.netbeans.gradle.project.util;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {
    private void testSplitArgs(String cmdLine, String... expected) {
        List<String> args = StringUtils.splitArgs(cmdLine);
        assertEquals(Arrays.asList(expected), args);
    }

    @Test
    public void testSplitArgsEmpty() {
        testSplitArgs("");
    }

    @Test
    public void testSplitArgsSingleArgQuoted() {
        testSplitArgs("\"a\"", "a");
        testSplitArgs("\"myarg\"", "myarg");
        testSplitArgs(" \"myarg\"", "myarg");
        testSplitArgs("\"myarg\" ", "myarg");
    }

    @Test
    public void testSplitArgsSingleArgUnquoted() {
        testSplitArgs("a", "a");
        testSplitArgs("myarg", "myarg");
        testSplitArgs(" myarg", "myarg");
        testSplitArgs("myarg ", "myarg");
    }

    @Test
    public void testSplitArgs2ArgsUnquoted() {
        testSplitArgs("a b", "a", "b");
        testSplitArgs("a   b", "a", "b");
        testSplitArgs(" a b ", "a", "b");
        testSplitArgs("myarg1 myarg2", "myarg1", "myarg2");
        testSplitArgs("myarg1  myarg2", "myarg1", "myarg2");
        testSplitArgs(" myarg1 myarg2 ", "myarg1", "myarg2");
    }

    @Test
    public void testSplitArgs2ArgsQuoted() {
        testSplitArgs("\"a\" \"b\"", "a", "b");
        testSplitArgs("\"a\"   \"b\"", "a", "b");
        testSplitArgs(" \"a\" \"b\" ", "a", "b");
        testSplitArgs("\"myarg1\" \"myarg2\"", "myarg1", "myarg2");
        testSplitArgs("\"myarg1\"  \"myarg2\"", "myarg1", "myarg2");
        testSplitArgs(" \"myarg1\" \"myarg2\" ", "myarg1", "myarg2");
    }

    @Test
    public void testSplitArgsMixed() {
        testSplitArgs("myarg1 myarg2 \"myarg3\" myarg4", "myarg1", "myarg2", "myarg3", "myarg4");
    }

    @Test
    public void testSplitArgsQutedWithSpaces() {
        testSplitArgs("\" my arg1 \" \"my arg2\"", " my arg1 ", "my arg2");
    }

    @Test
    public void testSplitArgsQutedNextToEachOther() {
        testSplitArgs("\" my arg1 \"\"my arg2\"", " my arg1 ", "my arg2");
    }

    @Test
    public void testSplitArgsQutedWithEscapes() {
        testSplitArgs("\" my\\\"arg1 \" \"my\\Xarg2\"", " my\"arg1 ", "myXarg2");
    }

    @Test
    public void testUnescapedIndexOfWithoutEscapeChar() {
        assertEquals(0, StringUtils.unescapedIndexOf(":", 0, ':'));
        assertEquals(0, StringUtils.unescapedIndexOf(":hello", 0, ':'));
        assertEquals(0, StringUtils.unescapedIndexOf(":he:llo", 0, ':'));
        assertEquals(5, StringUtils.unescapedIndexOf("hello:", 0, ':'));
        assertEquals(3, StringUtils.unescapedIndexOf(":he:llo", 1, ':'));
    }

    @Test
    public void testUnescapedIndexOfSkipsEscaped1() {
        assertEquals(8, StringUtils.unescapedIndexOf("0123\\:67:9", 0, ':'));
    }

    @Test
    public void testUnescapedIndexOfSkipsEscaped2() {
        assertEquals(10, StringUtils.unescapedIndexOf("0123\\\\\\:89:X", 0, ':'));
    }

    @Test
    public void testUnescapedIndexOfEscapedSlashBeforeMatch() {
        assertEquals(6, StringUtils.unescapedIndexOf("0123\\\\:78", 0, ':'));
    }

    @Test
    public void testUnescapedIndexOfNotFound() {
        assertEquals(-1, StringUtils.unescapedIndexOf("012378", 0, ':'));
        assertEquals(-1, StringUtils.unescapedIndexOf("", 0, ':'));
    }

    @Test
    public void testUnescapedSplitSimple() {
        assertArrayEquals(
                new String[]{"one", "two"},
                StringUtils.unescapedSplit("one:two", ':'));
    }

    @Test
    public void testUnescapedSplitEmptyEnd() {
        assertArrayEquals(
                new String[]{"one", "two", ""},
                StringUtils.unescapedSplit("one:two:", ':'));
    }

    @Test
    public void testUnescapedSplitEmptyStart() {
        assertArrayEquals(
                new String[]{"", "one", "two"},
                StringUtils.unescapedSplit(":one:two", ':'));
    }

    @Test
    public void testUnescapedSplitNoSplit() {
        assertArrayEquals(
                new String[]{"single"},
                StringUtils.unescapedSplit("single", ':'));
    }

    @Test
    public void testUnescapedSplitEmpty() {
        assertArrayEquals(
                new String[]{""},
                StringUtils.unescapedSplit("", ':'));
    }

    @Test
    public void testUnescapedSplitWithEscaped1() {
        assertArrayEquals(
                new String[]{"one", "two\\:three"},
                StringUtils.unescapedSplit("one:two\\:three", ':'));
    }

    @Test
    public void testUnescapedSplitWithEscaped2() {
        assertArrayEquals(
                new String[]{"one", "two\\\\\\:three"},
                StringUtils.unescapedSplit("one:two\\\\\\:three", ':'));
    }

    @Test
    public void testUnescapedSplitSimpleWithEscapedEscape() {
        assertArrayEquals(
                new String[]{"one\\\\", "two"},
                StringUtils.unescapedSplit("one\\\\:two", ':'));
    }

    @Test
    public void testUnescapedSplitLimitedToOne() {
        assertArrayEquals(
                new String[]{"one:two:three"},
                StringUtils.unescapedSplit("one:two:three", ':', 1));
    }

    @Test
    public void testUnescapedSplitLimitedSimple() {
        assertArrayEquals(
                new String[]{"one", "two:three"},
                StringUtils.unescapedSplit("one:two:three", ':', 2));
    }

    @Test
    public void testUnescapedSplitLimitedEndsWithSeparator1() {
        assertArrayEquals(
                new String[]{"one", ""},
                StringUtils.unescapedSplit("one:", ':', 2));
    }

    @Test
    public void testUnescapedSplitLimitedEndsWithSeparator2() {
        assertArrayEquals(
                new String[]{"one", "two:"},
                StringUtils.unescapedSplit("one:two:", ':', 2));
    }

    @Test
    public void testReplaceLFWithPreferredLineSeparator1() {
        String sep = System.getProperty("line.separator");
        String str = "\nFirst Line\nSecond Line\nThird Line";
        assertEquals(str.replace("\n", sep), StringUtils.replaceLFWithPreferredLineSeparator(str));
    }

    @Test
    public void testReplaceLFWithPreferredLineSeparator2() {
        assertEquals("", StringUtils.replaceLFWithPreferredLineSeparator(""));
    }
}
