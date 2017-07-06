package org.netbeans.gradle.project.output;

import java.util.Objects;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public final class OutputLinkDef {
    private final int startIndex; // inclusive
    private final int endIndex; // exclusive
    private final Runnable action;

    public OutputLinkDef(int startIndex, int endIndex, Runnable action) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.action = Objects.requireNonNull(action, "action");
    }

    public OutputLinkDef offsetLinkDef(int offset) {
        return new OutputLinkDef(startIndex + offset, endIndex + offset, action);
    }

    public boolean isEmptyLink() {
        return endIndex <= startIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public Runnable getAction() {
        return action;
    }

    public OutputListener toOutputListener() {
        return new OutputListener() {
            @Override
            public void outputLineSelected(OutputEvent ev) {
            }

            @Override
            public void outputLineAction(OutputEvent ev) {
                action.run();
            }

            @Override
            public void outputLineCleared(OutputEvent ev) {
            }
        };
    }
}
