package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.spi.project.ActionProvider;
import org.openide.modules.SpecificationVersion;

public abstract class AbstractProjectProperties implements ProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(AbstractProjectProperties.class.getName());

    public static final String DEFAULT_SOURCE_LEVEL = "1.7";
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    private static final Set<String> CUSTOMIZABLE_TASKS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            ActionProvider.COMMAND_BUILD,
            ActionProvider.COMMAND_TEST,
            ActionProvider.COMMAND_CLEAN,
            ActionProvider.COMMAND_RUN,
            ActionProvider.COMMAND_DEBUG,
            GradleActionProvider.COMMAND_JAVADOC,
            ActionProvider.COMMAND_REBUILD,
            ActionProvider.COMMAND_TEST_SINGLE,
            ActionProvider.COMMAND_DEBUG_TEST_SINGLE)));

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        SpecificationVersion version = platform.getSpecification().getVersion();
        if (version == null) {
            return DEFAULT_SOURCE_LEVEL;
        }

        String[] versionParts = version.toString().split(Pattern.quote("."));
        if (versionParts.length < 2) {
            return DEFAULT_SOURCE_LEVEL;
        }
        else {
            return versionParts[0] + "." + versionParts[1];
        }
    }

    public static Set<String> getCustomizableCommands() {
        return CUSTOMIZABLE_TASKS;
    }

    @Override
    public final Collection<MutableProperty<?>> getAllProperties() {
        List<MutableProperty<?>> result = new ArrayList<MutableProperty<?>>(4 + CUSTOMIZABLE_TASKS.size());
        result.add(getPlatform());
        result.add(getSourceEncoding());
        result.add(getSourceLevel());
        result.add(getCommonTasks());

        for (String command: CUSTOMIZABLE_TASKS) {
            MutableProperty<PredefinedTask> task = tryGetBuiltInTask(command);
            if (task != null) {
                result.add(task);
            }
            else {
                LOGGER.log(Level.WARNING, "tryGetBuiltInTask returned null for customizable task: {0}", command);
            }
        }

        return result;
    }
}
