package org.netbeans.gradle.project.properties;

import java.util.List;
import java.util.Map;
import org.netbeans.gradle.project.api.config.ConfigTree;

final class CompatibleRootNodeProperty extends AbstractFixedOrderNodeProperty {
    private static final String[] ROOT_KEYS = {
        "source-encoding",
        "target-platform-name",
        "target-platform",
        "source-level",
        "common-tasks",
        "script-platform",
        "gradle-home",
        "license-header",
        "built-in-tasks"
    };
    public static final ConfigNodeProperty INSTANCE = new CompatibleRootNodeProperty();

    private CompatibleRootNodeProperty() {
        super(ROOT_KEYS);
    }

    @Override
    public boolean ignoreValue() {
        return true;
    }

    @Override
    public ConfigNodeProperty getChildSorter(String keyName) {
        switch (keyName) {
            case "common-tasks":
                return TaskListOrder.INSTANCE;
            case "license-header":
                return LicenseOrder.INSTANCE;
            case "built-in-tasks":
                return TaskListOrder.INSTANCE;
            default:
                return DefaultConfigNodeProperty.INSTANCE;
        }
    }

    private static final class IgnoreValueProperty extends AbstractConfigNodeProperty {
        public static final ConfigNodeProperty INSTANCE = new IgnoreValueProperty();

        @Override
        public boolean ignoreValue() {
            return true;
        }
    }

    private static abstract class AbstractFixedOrderNodePropertyWithoutValue
    extends
            AbstractFixedOrderNodeProperty {

        public AbstractFixedOrderNodePropertyWithoutValue(String... order) {
            super(order);
        }

        @Override
        public boolean ignoreValue() {
            return true;
        }
    }

    private static final class TaskOrder extends AbstractFixedOrderNodePropertyWithoutValue {
        private static final String TASK_ARGS = "task-args";
        private static final String TASK_JVM_ARGS = "task-jvm-args";

        private static final String[] TASK_KEYS = {
            "display-name",
            "non-blocking",
            "task-names",
            TASK_ARGS,
            TASK_JVM_ARGS
        };
        public static final ConfigNodeProperty INSTANCE = new TaskOrder();

        public TaskOrder() {
            super(TASK_KEYS);
        }

        @Override
        public ConfigNodeProperty getChildSorter(String keyName) {
            switch (keyName) {
                case TASK_ARGS:
                    return IgnoreValueProperty.INSTANCE;
                case TASK_JVM_ARGS:
                    return IgnoreValueProperty.INSTANCE;
                default:
                    return DefaultConfigNodeProperty.INSTANCE;
            }
        }

        @Override
        public ConfigTree adjustNodes(ConfigTree actualTree) {
            Map<String, List<ConfigTree>> children = actualTree.getChildTrees();
            boolean hasTaskArgs = children.containsKey(TASK_ARGS);
            boolean hasTasJvmArgs = actualTree.getChildTrees().containsKey(TASK_JVM_ARGS);

            if (hasTaskArgs && hasTasJvmArgs) {
                return actualTree;
            }

            // We set their values, so that they will remain in the tree
            ConfigTree.Builder result = new ConfigTree.Builder(actualTree);
            result.getChildBuilder(TASK_ARGS).setValue("");
            result.getChildBuilder(TASK_JVM_ARGS).setValue("");
            return result.create();
        }
    }

    private static final class LicenseOrder extends AbstractFixedOrderNodePropertyWithoutValue {
        private static final String[] LICENSE_KEYS = {
            "name",
            "template",
            "property"
        };
        public static final ConfigNodeProperty INSTANCE = new LicenseOrder();

        public LicenseOrder() {
            super(LICENSE_KEYS);
        }

        @Override
        public ConfigNodeProperty getChildSorter(String keyName) {
            return DefaultConfigNodeProperty.INSTANCE;
        }
    }

    private static final class TaskListOrder extends AbstractFixedOrderNodePropertyWithoutValue {
        public static final ConfigNodeProperty INSTANCE = new TaskListOrder();

        public TaskListOrder() {
            super();
        }

        @Override
        public ConfigNodeProperty getChildSorter(String keyName) {
            if ("task".equals(keyName)) {
                return TaskOrder.INSTANCE;
            }
            else {
                return DefaultConfigNodeProperty.INSTANCE;
            }
        }
    }
}
