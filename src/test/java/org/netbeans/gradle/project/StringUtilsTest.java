package org.netbeans.gradle.project;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
}
