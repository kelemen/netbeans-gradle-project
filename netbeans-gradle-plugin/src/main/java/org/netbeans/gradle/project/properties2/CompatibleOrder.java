package org.netbeans.gradle.project.properties2;

final class CompatibleOrder extends AbstractFixedOrderNodeSorter {
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
    public static final ConfigNodeSorter INSTANCE = new CompatibleOrder();

    private CompatibleOrder() {
        super(ROOT_KEYS);
    }

    @Override
    public ConfigNodeSorter getChildSorter(String keyName) {
        switch (keyName) {
            case "common-tasks":
                return TaskListOrder.INSTANCE;
            case "license-header":
                return LicenseOrder.INSTANCE;
            case "built-in-tasks":
                return TaskListOrder.INSTANCE;
            default:
                return NaturalConfigNodeSorter.INSTANCE;
        }
    }

    private static final class TaskOrder extends AbstractFixedOrderNodeSorter {
        private static final String[] TASK_KEYS = {
            "display-name",
            "non-blocking",
            "task-args",
            "task-jvm-args"
        };
        public static final ConfigNodeSorter INSTANCE = new TaskOrder();

        public TaskOrder() {
            super(TASK_KEYS);
        }

        @Override
        public ConfigNodeSorter getChildSorter(String keyName) {
            return NaturalConfigNodeSorter.INSTANCE;
        }
    }

    private static final class LicenseOrder extends AbstractFixedOrderNodeSorter {
        private static final String[] LICENSE_KEYS = {
            "name",
            "template",
            "property"
        };
        public static final ConfigNodeSorter INSTANCE = new LicenseOrder();

        public LicenseOrder() {
            super(LICENSE_KEYS);
        }

        @Override
        public ConfigNodeSorter getChildSorter(String keyName) {
            return NaturalConfigNodeSorter.INSTANCE;
        }
    }

    private static final class TaskListOrder extends AbstractFixedOrderNodeSorter {
        public static final ConfigNodeSorter INSTANCE = new TaskListOrder();

        public TaskListOrder() {
            super();
        }

        @Override
        public ConfigNodeSorter getChildSorter(String keyName) {
            if ("task".equals(keyName)) {
                return TaskOrder.INSTANCE;
            }
            else {
                return NaturalConfigNodeSorter.INSTANCE;
            }
        }
    }
}
