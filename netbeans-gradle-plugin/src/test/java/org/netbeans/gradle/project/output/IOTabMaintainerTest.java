package org.netbeans.gradle.project.output;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class IOTabMaintainerTest {
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

    private static IOTabMaintainer<Integer, Tab> create() {
        return new IOTabMaintainer<Integer, Tab>(new IOTabFactory<Tab>() {
            @Override
            public Tab create(String caption) {
                return new Tab(caption);
            }
        });
    }

    @Test
    public void testReturnsSameTabAfterClose() {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        String caption = "my-tab";

        IOTabRef<Tab> tabRef = maintainer.getTab(1, caption);
        Tab originalTab = tabRef.getTab();
        tabRef.close();

        assertEquals(caption, originalTab.caption);

        IOTabRef<Tab> tabRef2 = maintainer.getTab(1, caption);
        assertSame(originalTab, tabRef2.getTab());
    }

    @Test
    public void testReturnsDifferentTabForDifferentKeys() {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");
        Tab tab1 = tabRef1.getTab();

        IOTabRef<Tab> tabRef2 = maintainer.getTab(2, "tab2");
        Tab tab2 = tabRef2.getTab();

        tabRef1.close();
        tabRef2.close();

        IOTabRef<Tab> tabRef3 = maintainer.getTab(3, "tab3");
        Tab tab3 = tabRef3.getTab();
        tabRef3.close();

        assertEquals("tab1", tab1.caption);
        assertEquals("tab2", tab2.caption);
        assertEquals("tab3", tab3.caption);
    }

    @Test
    public void testDoesNotReturnClosedTab() {
        IOTabMaintainer<Integer, Tab> maintainer = create();

        IOTabRef<Tab> tabRef1 = maintainer.getTab(1, "tab1");
        Tab tab1 = tabRef1.getTab();
        tabRef1.close();

        tab1.close();

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
    public void testLowestIndexIsUsedWhenThereAreMultipleOpen1() {
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
    public void testLowestIndexIsUsedWhenThereAreMultipleOpen2() {
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
    public void testCloseIsIdempotent() {
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
        private volatile boolean closed;

        public Tab(String caption) {
            this.caption = caption;
            this.closed = false;
        }

        public void close() {
            this.closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public String toString() {
            return "Tab{" + "caption=" + caption + ", closed=" + closed + '}';
        }
    }
}
