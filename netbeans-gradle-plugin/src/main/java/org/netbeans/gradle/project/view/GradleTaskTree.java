package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.MultiMapUtils;

public final class GradleTaskTree {
    private static final Logger LOGGER = Logger.getLogger(GradleTaskTree.class.getName());

    private static final int DEFAULT_TASK_LIMIT = 20;

    private final String caption;
    private final GradleTaskID taskID;
    private final List<GradleTaskTree> children;

    public GradleTaskTree(GradleTaskID taskID) {
        this.taskID = Objects.requireNonNull(taskID, "taskID");
        this.caption = taskID.getName();
        this.children = Collections.emptyList();
    }

    public GradleTaskTree(String caption, List<GradleTaskTree> children) {
        this(caption, null, children);
    }

    private GradleTaskTree(
            String caption,
            GradleTaskID taskID,
            List<GradleTaskTree> children) {
        this.caption = Objects.requireNonNull(caption, "caption");
        this.taskID = taskID;
        this.children = CollectionUtils.copyNullSafeList(children);
    }

    public String getCaption() {
        return caption;
    }

    @Nullable
    public GradleTaskID getTaskID() {
        return taskID;
    }

    public List<GradleTaskTree> getChildren() {
        return children;
    }

    public static List<GradleTaskTree> createTaskTree(Collection<GradleTaskID> tasks) {
        return createTaskTree(DEFAULT_TASK_LIMIT, tasks);
    }

    public static List<GradleTaskTree> createTaskTree(
            int taskLimit,
            Collection<GradleTaskID> taskIDs) {
        return createTaskTree(taskLimit, 0, taskIDs);
    }

    private static List<GradleTaskTree> createTaskTree(
            int taskLimit,
            int skipCharCount,
            Collection<GradleTaskID> taskIDs) {
        int taskCount = taskIDs.size();
        if (taskCount <= taskLimit || taskCount <= 1) {
            return toLeafs(taskIDs);
        }

        Map<String, List<GradleTaskID>> splitTasks = new LinkedHashMap<>();
        for (GradleTaskID taskID: taskIDs) {
            String name = taskID.getName();
            int nextPos = getNextWordStartPos(skipCharCount, name);
            if (nextPos < 0) {
                MultiMapUtils.addToMultiMap(name, taskID, splitTasks);
            }
            else {
                String prefix = name.substring(0, nextPos);
                MultiMapUtils.addToMultiMap(prefix, taskID, splitTasks);
            }
        }

        int splitTasksSize = splitTasks.size();

        if (splitTasksSize == 1) {
            Map.Entry<String, List<GradleTaskID>> task
                    = splitTasks.entrySet().iterator().next();

            int nextSkipCharCount = task.getKey().length();
            return createTaskTree(taskLimit, nextSkipCharCount, taskIDs);
        }
        else {
            List<GradleTaskTree> result = new ArrayList<>(splitTasksSize);
            for (Map.Entry<String, List<GradleTaskID>> entry: splitTasks.entrySet()) {
                GradleTaskTree subTree = createTaskTree(taskLimit, entry);
                if (subTree != null) {
                    result.add(subTree);
                }
            }

            int resultSize = result.size();
            if (resultSize > taskLimit) {
                LOGGER.log(Level.INFO,
                        "Number of tasks exceed the needed limit. Task count: {0}. Requested limit: {1}.",
                        new Object[]{resultSize, taskLimit});
            }

            return result;
        }
    }

    private static GradleTaskTree createTaskTree(
            int taskLimit,
            Map.Entry<String, List<GradleTaskID>> entry) {

        List<GradleTaskID> childTasks = entry.getValue();
        int childrenCount = childTasks.size();
        if (childrenCount > 1) {
            String key = entry.getKey();

            List<GradleTaskTree> subTrees
                    = createTaskTree(taskLimit, key.length(), childTasks);
            return new GradleTaskTree(key, subTrees);
        }
        else if (childrenCount == 1) {
            return new GradleTaskTree(childTasks.get(0));
        }
        else {
            return null;
        }
    }

    private static List<GradleTaskTree> toLeafs(Collection<GradleTaskID> taskIDs) {
        List<GradleTaskTree> result = new ArrayList<>(taskIDs.size());
        for (GradleTaskID taskID: taskIDs) {
            result.add(new GradleTaskTree(taskID));
        }
        return result;
    }

    private static CharacterType getCharacterType(char ch) {
        switch (Character.getType(ch)) {
            case Character.UPPERCASE_LETTER:
                return CharacterType.UPPERCASE_LETTER;
            case Character.LOWERCASE_LETTER:
                return CharacterType.LOWERCASE_LETTER;
            case Character.DECIMAL_DIGIT_NUMBER:
                return CharacterType.NUMBER;
            default:
                return CharacterType.OTHER;
        }
    }

    private static int getNextWordStartPos(int startPos, String str) {
        int length = str.length();
        if (length <= startPos + 1) {
            // We need at least two characters to have a chance for a next word.
            return -1;
        }

        CharacterType firstCh = getCharacterType(str.charAt(startPos));
        CharacterType secondCh = getCharacterType(str.charAt(startPos + 1));

        int startOffset;
        CharacterType typeToAvoid;
        if (firstCh == CharacterType.UPPERCASE_LETTER) {
            if (!secondCh.letter) {
                return startPos + 1;
            }
            typeToAvoid = secondCh;
            startOffset = startPos + 2;
        }
        else {
            typeToAvoid = firstCh;
            startOffset = startPos + 1;
        }

        if (typeToAvoid == CharacterType.OTHER) {
            for (int i = startPos + 1; i < length; i++) {
                typeToAvoid = getCharacterType(str.charAt(i));
                if (typeToAvoid != CharacterType.OTHER) {
                    startOffset = i + 1;
                    break;
                }
            }
            if (typeToAvoid == CharacterType.OTHER) {
                return -1;
            }
        }

        for (int i = startOffset; i < length; i++) {
            if (typeToAvoid != getCharacterType(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private enum CharacterType {
        LOWERCASE_LETTER(true),
        UPPERCASE_LETTER(true),
        NUMBER(false),
        OTHER(false);

        public final boolean letter;

        private CharacterType(boolean letter) {
            this.letter = letter;
        }
    }
}
