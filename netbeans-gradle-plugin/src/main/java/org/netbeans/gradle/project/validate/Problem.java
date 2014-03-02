package org.netbeans.gradle.project.validate;

import org.jtrim.utils.ExceptionHelper;

public final class Problem {
    public enum Level {
        INFO(0),
        WARNING(1),
        SEVERE(2);

        private final int intValue;

        private Level(int intValue) {
            this.intValue = intValue;
        }

        public int getIntValue() {
            return intValue;
        }
    }

    public static Problem severe(String message) {
        return new Problem(Level.SEVERE, message);
    }

    public static Problem warning(String message) {
        return new Problem(Level.WARNING, message);
    }

    public static Problem info(String message) {
        return new Problem(Level.INFO, message);
    }

    private final Level level;
    private final String message;

    public Problem(Level level, String message) {
        ExceptionHelper.checkNotNullArgument(level, "level");
        ExceptionHelper.checkNotNullArgument(message, "message");

        this.level = level;
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
