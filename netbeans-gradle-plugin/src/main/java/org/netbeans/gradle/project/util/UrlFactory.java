package org.netbeans.gradle.project.util;

import java.io.File;
import java.net.URL;

public interface UrlFactory {
    public URL toUrl(File file);
}
