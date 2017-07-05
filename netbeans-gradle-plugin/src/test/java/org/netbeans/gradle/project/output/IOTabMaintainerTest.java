package org.netbeans.gradle.project.output;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

public class IOTabMaintainerTest {
    private static IOTabMaintainer<Integer, Tab> create() {
        return new IOTabMaintainer<>(Tab::new);
    }

    @Test
    public void testReturnsSameTabAfterClose() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        String caption = "my-tab";

        Tab originalTab;
        try (IOTabRef<Tab> tabRef = maintainer.getTab(1, caption)) {
            originalTab = tabRef.getTab();
        }

        assertTrue("Must be closed.", originalTab.isClosedForNow());
        assertEquals(caption, originalTab.caption);

        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, caption);
        assertSame(originalTab, tabRef2.getTab());
    }

    @Test
    public void testReturnsDifferentTabForDifferentKeys() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        Tab tab1;
        IOTabRef<Tab> tabRef2;
        Tab tab2;
        try (IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1")) {
            tab1 = tabRef1.getTab();
            tabRef2 = maintainer.getTab(2, "tab2");
            tab2 = tabRef2.getTab();
        }
        tabRef2.close();

        Tab tab3;
        try (IOTabRef<Tab> tabRef3 = maintainer.getTab(3, "tab3")) {
            tab3 = tabRef3.getTab();
        }

        assertEquals("tab1", tab1.caption);
        assertEquals("tab2", tab2.caption);
        assertEquals("tab3", tab3.caption);
    }

    @Test
    public void testDoesNotReturnClosedTab() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        Tab tab1;
        try (IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1")) {
            tab1 = tabRef1.getTab();
        }

        tab1.destroy();

        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, "tab2");
        Tab tab2 = tabRef2.getTab();

        assertEquals("tab1", tab1.caption);
        assertEquals("tab2", tab2.caption);
        assertNotSame(tab1, tab2);
    }

    @Test
    public void testMultipleConcurrentTabsAddIndexes() {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef3 = maintainer.getTab(1, "tab1");

        assertEquals("tab1", tabRef1.getTab().caption);
        assertEquals("tab1 #2", tabRef2.getTab().caption);
        assertEquals("tab1 #3", tabRef3.getTab().caption);
    }

    @Test
    public void testLowestIndexIsUsedWhenThereAreMultipleOpen1() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef3 = maintainer.getTab(1, "tab1");

        tabRef1.close();
        tabRef2.close();
        tabRef3.close();

        tabRef1 = maintainer.getTab(1, "XXX");
        assertEquals("tab1", tabRef1.getTab().caption);

        tabRef2 = maintainer.getTab(1, "XXX");
        assertEquals("tab1 #2", tabRef2.getTab().caption);

        tabRef3 = maintainer.getTab(1, "XXX");
        assertEquals("tab1 #3", tabRef3.getTab().caption);
    }

    @Test
    public void testLowestIndexIsUsedWhenThereAreMultipleOpen2() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, "tab1");
        IOTabRef<Tab> tabRef3 = maintainer.getTab(1, "tab1");

        tabRef3.close();
        tabRef2.close();
        tabRef1.close();

        tabRef1 = maintainer.getTab(1, "XXX");
        assertEquals("tab1", tabRef1.getTab().caption);

        tabRef2 = maintainer.getTab(1, "XXX");
        assertEquals("tab1 #2", tabRef2.getTab().caption);

        tabRef3 = maintainer.getTab(1, "XXX");
        assertEquals("tab1 #3", tabRef3.getTab().caption);
    }

    @Test
    public void testCloseIsIdempotent() throws IOException {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");

        for (int i = 0; i < 5; i++) {
            tabRef1.close();
        }

        tabRef1 = maintainer.getTab(1, "tab1");
        assertEquals("tab1", tabRef1.getTab().caption);

        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, "tab1");
        assertEquals("tab1 #2", tabRef2.getTab().caption);
    }

    private static final class Tab implements IOTabDef {
        public final String caption;
        private volatile boolean destroyed;
        private volatile boolean closed;

        public Tab(String caption) {
            this.caption = caption;
            this.destroyed = false;
            this.closed = false;
        }

        void destroy() {
            this.destroyed = true;
        }

        boolean isClosedForNow() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
        }

        @Override
        public boolean isDestroyed() {
            return destroyed;
        }

        @Override
        public String toString() {
            return "Tab{" + "caption=" + caption + ", closed for now: " + closed + ", destroyed=" + destroyed + '}';
        }
    }
}
