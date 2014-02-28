package org.netbeans.gradle.model.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class BasicFileUtilsTest {
    private static final String LATIN2_STR = "\u00E1rv\u00EDzt\u0171r\u0151t\u00FCk\u00F6rf\u00FAr\u00F3g\u00E9p";
    private static final String LATIN2_STR_ESCAPED = "\\" + "u00E1rv\\" + "u00EDzt\\" + "u0171r\\" + "u0151t\\" + "u00FCk\\" + "u00F6rf\\" + "u00FAr\\" + "u00F3g\\" + "u00E9p";

    @Test
    public void testToSafelyPastableToJavaCode0Arg() {
        assertEquals("", BasicFileUtils.toSafelyPastableToJavaCode(""));
    }

    @Test
    public void testToSafelyPastableToJavaCode1ArgLatin2() {
        assertEquals(LATIN2_STR_ESCAPED, BasicFileUtils.toSafelyPastableToJavaCode(LATIN2_STR));
    }

    @Test
    public void testToSafelyPastableToJavaCode2ArgsLatin2() {
        String[] testStrs = {LATIN2_STR.substring(0, 3), LATIN2_STR.substring(3)};

        assertEquals(LATIN2_STR_ESCAPED, BasicFileUtils.toSafelyPastableToJavaCode(testStrs));
    }

    @Test
    public void testToSafelyPastableToJavaCode2ArgsLatin3() {
        String[] testStrs = {LATIN2_STR.substring(0, 3), LATIN2_STR.substring(3, 7), LATIN2_STR.substring(7)};

        assertEquals(LATIN2_STR_ESCAPED, BasicFileUtils.toSafelyPastableToJavaCode(testStrs));
    }

    @Test
    public void testEscapeBackSlashU1() {
        String str = "Begin\\" + "u";
        assertEquals("Begin\\\\" + "u0075", BasicFileUtils.toSafelyPastableToJavaCode(str));
    }

    @Test
    public void testEscapeBackSlashU2() {
        String str = "Begin\\" + "uEnd";
        assertEquals("Begin\\\\" + "u0075End", BasicFileUtils.toSafelyPastableToJavaCode(str));
    }

    @Test
    public void testEscapeBackSlashU3() {
        String str = "\\" + "uEnd";
        assertEquals("\\\\" + "u0075End", BasicFileUtils.toSafelyPastableToJavaCode(str));
    }

    @Test
    public void testEscapeBackSlashU4() {
        String[] strs = {"Begin\\", "uEnd"};
        assertEquals("Begin\\\\" + "u0075End", BasicFileUtils.toSafelyPastableToJavaCode(strs));
    }

    @Test
    public void testEscapeBackSlash() {
        String str = "Begin\\End";
        assertEquals(str, BasicFileUtils.toSafelyPastableToJavaCode(str));
    }

    @Test
    public void testToSafelyPastableToJavaCode1ArgAscii() {
        String str = "BasicString'\"";
        assertEquals(str, BasicFileUtils.toSafelyPastableToJavaCode(str));
    }

    @Test
    public void testToSafelyPastableToJavaCode2ArgsAscii() {
        String str = "BasicString'\"";
        String[] testStrs = {str.substring(0, 3), str.substring(3)};

        assertEquals(str, BasicFileUtils.toSafelyPastableToJavaCode(testStrs));
    }

    @Test
    public void testToSafelyPastableToJavaCodeEmptyStrings() {
        assertEquals("", BasicFileUtils.toSafelyPastableToJavaCode("", "", ""));
    }
}
