package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingPropertySource;
import org.junit.Test;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.SwingTest;
import org.netbeans.gradle.project.util.SwingTestAware;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AnnotationChildNodesTest extends SwingTestAware {
    private static Project mockProject() {
        return new Project() {
            @Override
            public FileObject getProjectDirectory() {
                throw new UnsupportedOperationException("This is a mock project.");
            }

            @Override
            public Lookup getLookup() {
                return Lookup.EMPTY;
            }
        };
    }

    private AnnotationChildNodes testNodes(Object... factories) {
        final Lookup lookup = Lookups.fixed(factories);
        Project project = mockProject();
        return new AnnotationChildNodes(project, () -> lookup);
    }

    private static NodeFactory testNodeFactory(final PropertySource<? extends TestNodeListSnapshot> nodeList) {
        return project -> new TestNodeList(nodeList);
    }

    @Test
    @SwingTest
    public void testGetNodesAfterRecreate() {
        TestNodeListSnapshot initList = new TestNodeListSnapshot(0);
        TestNodeListSnapshot testList = new TestNodeListSnapshot(1);

        MutableProperty<TestNodeListSnapshot> nodeList = PropertyFactory.memProperty(initList);
        AnnotationChildNodes childNodes = testNodes(testNodeFactory(nodeList));

        childNodes.addNotify();
        childNodes.removeNotify();

        PropertySource<Collection<SingleNodeFactory>> singleNodeFactoriesRef = childNodes.nodeFactories();
        TestListener changeListener = new TestListener();
        singleNodeFactoriesRef.addChangeListener(changeListener);

        assertEquals("node factory count", 0, singleNodeFactoriesRef.getValue().size());

        childNodes.addNotify();

        int preSetNotifyCount = changeListener.getCallCount();
        nodeList.setValue(testList);
        changeListener.verifyCalledMore(preSetNotifyCount);

        testList.verifyNodes(singleNodeFactoriesRef.getValue());

        childNodes.removeNotify();

        assertEquals("node factory count", 0, singleNodeFactoriesRef.getValue().size());
    }

    @Test
    @SwingTest
    public void testGetNodesAfterNotify() {
        TestNodeListSnapshot initList = new TestNodeListSnapshot(0);
        TestNodeListSnapshot testList = new TestNodeListSnapshot(1);

        MutableProperty<TestNodeListSnapshot> nodeList = PropertyFactory.memProperty(initList);
        AnnotationChildNodes childNodes = testNodes(testNodeFactory(nodeList));

        PropertySource<Collection<SingleNodeFactory>> singleNodeFactoriesRef = childNodes.nodeFactories();
        TestListener changeListener = new TestListener();
        singleNodeFactoriesRef.addChangeListener(changeListener);

        assertEquals("node factory count", 0, singleNodeFactoriesRef.getValue().size());

        childNodes.addNotify();

        int preSetNotifyCount = changeListener.getCallCount();
        nodeList.setValue(testList);
        changeListener.verifyCalledMore(preSetNotifyCount);

        testList.verifyNodes(singleNodeFactoriesRef.getValue());

        childNodes.removeNotify();

        assertEquals("node factory count", 0, singleNodeFactoriesRef.getValue().size());
    }

    @Test
    @SwingTest
    public void testGetNodesBeforeNotify() {
        TestNodeListSnapshot initList = new TestNodeListSnapshot(0);
        TestNodeListSnapshot testList = new TestNodeListSnapshot(1);

        MutableProperty<TestNodeListSnapshot> nodeList = PropertyFactory.memProperty(initList);
        AnnotationChildNodes childNodes = testNodes(testNodeFactory(nodeList));

        PropertySource<Collection<SingleNodeFactory>> singleNodeFactoriesRef = childNodes.nodeFactories();
        TestListener changeListener = new TestListener();
        singleNodeFactoriesRef.addChangeListener(changeListener);

        assertEquals("node factory count", 0, singleNodeFactoriesRef.getValue().size());

        int preSetNotifyCount = changeListener.getCallCount();

        nodeList.setValue(testList);
        childNodes.addNotify();

        changeListener.verifyCalledMore(preSetNotifyCount);
        testList.verifyNodes(singleNodeFactoriesRef.getValue());

        childNodes.removeNotify();
    }

    private static final class TestNodeListSnapshot {
        private final TestNodeSnapshot[] nodes;

        public TestNodeListSnapshot(int count) {
            this.nodes = new TestNodeSnapshot[count];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = new TestNodeSnapshot("Node" + i);
            }
        }

        public void verifyNodes(Collection<? extends SingleNodeFactory> actualNodeFactories) {
            int i = 0;
            for (SingleNodeFactory nodeFactory: actualNodeFactories) {
                assertSame(nodeFactory.createNode(), nodes[i].getNode());
                i++;
            }
            assertEquals("Node count must match", i, nodes.length);
        }

        public TestNodeListSnapshot(TestNodeSnapshot... nodes) {
            this.nodes = nodes.clone();
        }

        public TestNodeSnapshot[] getNodes() {
            return nodes.clone();
        }

        public TestNodeSnapshot tryGetByKey(String key) {
            for (TestNodeSnapshot node: nodes) {
                if (key.equals(node.getKey())) {
                    return node;
                }
            }
            return null;
        }

        public Node tryGetNodeByKey(String key) {
            TestNodeSnapshot result = tryGetByKey(key);
            return result != null ? result.getNode() : null;
        }
    }

    private static final class TestNodeSnapshot {
        private final String key;
        private final Node node;

        public TestNodeSnapshot(String key) {
            this.key = key;
            this.node = mock(Node.class);
            doReturn(key)
                    .when(node)
                    .toString();
        }

        public String getKey() {
            return key;
        }

        public Node getNode() {
            return node;
        }
    }

    private static final class TestNodeList implements NodeList<String> {
        private final SwingPropertySource<TestNodeListSnapshot, ChangeListener> nodeListRef;
        private final AtomicInteger notifyCount;

        public TestNodeList(PropertySource<? extends TestNodeListSnapshot> nodeList) {
            this(NbProperties.toOldProperty(nodeList, new Object()));
        }

        public TestNodeList(SwingPropertySource<TestNodeListSnapshot, ChangeListener> nodeList) {
            this.nodeListRef = nodeList;
            this.notifyCount = new AtomicInteger();
        }

        public void verifyNotifyCount(int expected) {
            assertEquals("notify count", expected, notifyCount.get());
        }

        @Override
        public List<String> keys() {
            TestNodeSnapshot[] nodeList = nodeListRef.getValue().getNodes();
            List<String> result = new ArrayList<>(nodeList.length);
            for (TestNodeSnapshot node: nodeList) {
                result.add(node.getKey());
            }
            return result;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            nodeListRef.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            nodeListRef.removeChangeListener(l);
        }

        @Override
        public Node node(String key) {
            return nodeListRef.getValue().tryGetNodeByKey(key);
        }

        @Override
        public void addNotify() {
            notifyCount.incrementAndGet();
        }

        @Override
        public void removeNotify() {
            notifyCount.decrementAndGet();
        }
    }

    private static final class TestListener implements Runnable {
        private final AtomicInteger callCount;

        public TestListener() {
            this.callCount = new AtomicInteger(0);
        }

        public int getCallCount() {
            return callCount.get();
        }

        public void verifyCalledMore(int prevCallCount) {
            int currentCallCount = getCallCount();
            if (prevCallCount >= currentCallCount) {
                fail("Expected to be called more than " + prevCallCount + " times but was called " + currentCallCount + " times.");
            }
        }

        @Override
        public void run() {
            callCount.incrementAndGet();
        }
    }
}
