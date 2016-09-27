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
}
