package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import org.junit.Test;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationUtils;

import static org.junit.Assert.*;

public class ConstrProjectInfoBuilderRefTest {
    public <T> ProjectInfoBuilder2<T> create(Class<? extends T> modelType, String wrappedTypeName, Object... arguments) {
        return new ConstrProjectInfoBuilderRef<T>(modelType, wrappedTypeName, arguments);
    }

    @Test
    public void testNoArgumentWithoutPackage() {
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.REL_NAME);

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", TestInfoBuilder.DEFAULT_ARG, returned);
    }

    @Test
    public void test1ArgumentWithoutPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.REL_NAME, model);

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", model, returned);
    }

    @Test
    public void test2ArgumentWithoutPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.REL_NAME, model, "MyName");

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", model, returned);
        assertEquals("name", "MyName", builderRef.getName());
    }

    @Test
    public void testNoArgumentWithPackage() {
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.ABS_NAME);

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", TestInfoBuilder.DEFAULT_ARG, returned);
    }

    @Test
    public void test1ArgumentWithPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.ABS_NAME, model);

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", model, returned);
    }

    @Test
    public void test2ArgumentWithPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.ABS_NAME, model, "MyName");

        TestObj returned = builderRef.getProjectInfo(new Object());

        assertSame("model", model, returned);
        assertEquals("name", "MyName", builderRef.getName());
    }

    @Test
    public void testNameWithoutPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.REL_NAME, model, "MyName");

        assertEquals("name", "MyName", builderRef.getName());
    }

    @Test
    public void testNameWithPackage() {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilder.ABS_NAME, model, "MyName");

        assertEquals("name", "MyName", builderRef.getName());
    }

    @Test
    public void testProjectArgWithoutPackage() {
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilderBasic.REL_NAME);

        TestObj project = new TestObj("MyTestProject");
        Object returned = builderRef.getProjectInfo(project);

        assertSame("project", project, returned);
    }

    @Test
    public void testProjectArgWithPackage() {
        ProjectInfoBuilder2<TestObj> builderRef = create(TestObj.class, TestInfoBuilderBasic.ABS_NAME);

        TestObj project = new TestObj("MyTestProject");
        Object returned = builderRef.getProjectInfo(project);

        assertSame("project", project, returned);
    }

    @Test
    public void testSerialization() throws Exception {
        TestObj model = new TestObj("MyTestObj");
        ProjectInfoBuilder2<?> builderRef = create(TestObj.class, TestInfoBuilder.ABS_NAME, model, "MyName");

        byte[] serialized = SerializationUtils.serializeObject(builderRef);
        builderRef = (ProjectInfoBuilder2<?>)SerializationUtils.deserializeObject(serialized, SerializationCache.NO_CACHE);

        Object returned = builderRef.getProjectInfo(new Object());

        assertEquals("model", model.toString(), returned.toString());
        assertEquals("name", "MyName", builderRef.getName());
    }

    public static final class TestObj implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String value;

        public TestObj(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final class TestInfoBuilder implements ProjectInfoBuilder2<TestObj> {
        private static final long serialVersionUID = 1L;

        public static final String REL_NAME = "ConstrProjectInfoBuilderRefTest$TestInfoBuilder";
        public static final String ABS_NAME = TestInfoBuilder.class.getName();

        public static final TestObj DEFAULT_ARG = new TestObj("TestDefaultArg");
        public static final String DEFAULT_NAME = "TestDefaultName";

        private final TestObj arg;
        private final String name;

        public TestInfoBuilder() {
            this(DEFAULT_ARG);
        }

        public TestInfoBuilder(TestObj arg) {
            this(arg, DEFAULT_NAME);
        }

        public TestInfoBuilder(TestObj arg, String name) {
            this.arg = arg;
            this.name = name;
        }

        @Override
        public TestObj getProjectInfo(Object project) {
            return arg;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static final class TestInfoBuilderBasic implements ProjectInfoBuilder2<Object> {
        private static final long serialVersionUID = 1L;

        public static final String REL_NAME = "ConstrProjectInfoBuilderRefTest$TestInfoBuilderBasic";
        public static final String ABS_NAME = TestInfoBuilderBasic.class.getName();

        public TestInfoBuilderBasic() {
        }

        @Override
        public Object getProjectInfo(Object project) {
            return project;
        }

        @Override
        public String getName() {
            return "TestInfoBuilderBasic";
        }
    }
}
