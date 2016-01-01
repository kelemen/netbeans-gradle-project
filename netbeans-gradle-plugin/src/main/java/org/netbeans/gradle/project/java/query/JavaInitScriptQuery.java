package org.netbeans.gradle.project.java.query;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.gradle.project.api.config.InitScriptQueryEx;
import org.netbeans.gradle.project.util.StringUtils;

public final class JavaInitScriptQuery implements InitScriptQueryEx {
    @StaticResource
    private static final String INIT_SCRIPT_PATH = "org/netbeans/gradle/project/resources/nb-init-script.gradle";

    private final AtomicReference<String> initScriptCache;

    public JavaInitScriptQuery() {
        this.initScriptCache = new AtomicReference<>(null);
    }

    @Override
    public String getBaseFileName() {
        return "nb-init-java";
    }

    @Override
    public String getInitScript() throws IOException {
        String result = initScriptCache.get();
        if (result == null) {
            result = StringUtils.getResourceAsString(INIT_SCRIPT_PATH, StringUtils.UTF8);
            initScriptCache.compareAndSet(null, result);
            result = initScriptCache.get();
        }
        return result;
    }
}
