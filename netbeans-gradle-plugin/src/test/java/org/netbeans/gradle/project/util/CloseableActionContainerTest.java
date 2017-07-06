package org.netbeans.gradle.project.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class CloseableActionContainerTest {
    @Test
    public void test1Action1Open() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        action0.assertOpenCount(0);

        container.open();
        action0.assertAllClosedButLast(1);

        container.close();
        action0.assertAllClosedOnce(1);
    }

    @Test
    public void test1Action2Open() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        container.open();
        container.close();

        container.open();
        action0.assertAllClosedButLast(2);

        container.close();
        action0.assertAllClosedOnce(2);
    }

    @Test
    public void test3Action1Open() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        TestCloseableAction action1 = new TestCloseableAction();

        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        action0.assertOpenCount(0);

        container.open();
        action0.assertAllClosedButLast(1);

        actionRef.setValue(action1);
        action0.assertAllClosedOnce(1);
        action1.assertAllClosedButLast(1);

        container.close();
        action0.assertAllClosedOnce(1);
        action1.assertAllClosedOnce(1);

        TestCloseableAction action2 = new TestCloseableAction();
        actionRef.setValue(action2);
        action0.assertAllClosedOnce(1);
        action1.assertAllClosedOnce(1);
        action2.assertOpenCount(0);
    }

    @Test
    public void testActionDefinedAfterOpen() {
        CloseableActionContainer container = new CloseableActionContainer();

        container.open();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);
        container.defineAction(actionRef);

        action0.assertAllClosedButLast(1);

        container.close();
        action0.assertAllClosedOnce(1);
    }

    @Test
    public void testActionDefinedAfterClose() {
        CloseableActionContainer container = new CloseableActionContainer();

        container.open();
        container.close();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);
        container.defineAction(actionRef);

        action0.assertOpenCount(0);
    }

    @Test
    public void test1Action1DuplicateOpen() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        action0.assertOpenCount(0);

        container.open();
        container.open();
        action0.assertAllClosedButLast(1);

        container.close();
        action0.assertAllClosedOnce(1);
    }

    @Test
    public void test1Action1OpenDuplicateClose() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        action0.assertOpenCount(0);

        container.open();
        action0.assertAllClosedButLast(1);

        container.close();
        container.close();
        action0.assertAllClosedOnce(1);
    }

    @Test
    public void test1ActionDuplicateCloseWithoutOpen() {
        CloseableActionContainer container = new CloseableActionContainer();

        TestCloseableAction action0 = new TestCloseableAction();
        MutableProperty<TestCloseableAction> actionRef = PropertyFactory.memProperty(action0);

        container.defineAction(actionRef);

        container.close();
        action0.assertOpenCount(0);

        container.close();

        action0.assertOpenCount(0);
    }

    private static final class TestCloseableAction implements CloseableAction {
        private final Lock mainLock;
        private final List<TestActionRef> openedRefs;

        public TestCloseableAction() {
            this.mainLock = new ReentrantLock();
            this.openedRefs = new ArrayList<>();
        }

        public TestActionRef getOpenedRef(int openIndex) {
            mainLock.lock();
            try {
                return openedRefs.get(openIndex);
            } finally {
                mainLock.unlock();
            }
        }

        public void assertAllClosedButLast(int expectedOpenCount) {
            assert expectedOpenCount > 0;

            mainLock.lock();
            try {
                assertEquals("Open count", expectedOpenCount, openedRefs.size());

                for (int i = 0; i < expectedOpenCount - 1; i++) {
                    TestActionRef actionRef = openedRefs.get(i);
                    actionRef.assertCloseCount(1);
                }
                openedRefs.get(expectedOpenCount - 1);
            } finally {
                mainLock.unlock();
            }
        }

        public void assertAllClosedOnce(int numberOfExpectedOpens) {
            mainLock.lock();
            try {
                assertEquals("Open count", numberOfExpectedOpens, openedRefs.size());

                for (TestActionRef actionRef: openedRefs) {
                    actionRef.assertCloseCount(1);
                }
            } finally {
                mainLock.unlock();
            }
        }

        public void assertOpenCount(int expectedOpenCount) {
            mainLock.lock();
            try {
                assertEquals("Open count", expectedOpenCount, openedRefs.size());
            } finally {
                mainLock.unlock();
            }
        }

        public TestActionRef[] getOpenedRefs() {
            mainLock.lock();
            try {
                return openedRefs.toArray(new TestActionRef[openedRefs.size()]);
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public TestActionRef open() {
            TestActionRef result = new TestActionRef();
            mainLock.lock();
            try {
                openedRefs.add(result);
            } finally {
                mainLock.unlock();
            }
            return result;
        }
    }

    private static final class TestActionRef implements CloseableAction.Ref {
        private final AtomicInteger closeCount;

        public TestActionRef() {
            this.closeCount = new AtomicInteger(0);
        }

        public void assertCloseCount(int expectedCloseCount) {
            assertEquals("closeCount", expectedCloseCount, closeCount.get());
        }

        public int getCloseCount() {
            return closeCount.get();
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }
}
