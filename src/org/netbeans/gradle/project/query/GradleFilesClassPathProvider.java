package org.netbeans.gradle.project.query;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = ClassPathProvider.class)})
public final class GradleFilesClassPathProvider implements ClassPathProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleFilesClassPathProvider.class.getName());

    private final ConcurrentMap<ClassPathType, ClassPath> classpaths;

    @SuppressWarnings("MapReplaceableByEnumMap") // no, it's not.
    public GradleFilesClassPathProvider() {
        this.classpaths = new ConcurrentHashMap<ClassPathType, ClassPath>();
    }

    public static boolean isGradleFile(FileObject file) {
        // case-insensitive check, so that there is no surprise on Windows.
        return "gradle".equals(file.getExt().toLowerCase(Locale.US));
    }

    private void setupClassPaths() {
        JavaPlatform defaultJdk = JavaPlatform.getDefault();

        if (defaultJdk != null) {
            classpaths.putIfAbsent(ClassPathType.BOOT, defaultJdk.getBootstrapLibraries());
        }
        else {
            LOGGER.warning("There is no default JDK.");
        }

        // TODO: add class paths for COMPILE and RUNTIME
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        LOGGER.log(Level.INFO, "findClassPath({0}, {1})", new Object[]{file, type});
        // case-insensitive check, so that there is no surprise on Windows.
        if (!isGradleFile(file)) {
            return null;
        }

        ClassPathType classPathType = getClassPathType(type);
        if (classPathType == null) {
            return null;
        }

        ClassPath classpath = classpaths.get(classPathType);
        if (classpath != null) {
            return classpath;
        }

        setupClassPaths();
        return classpaths.get(classPathType);
    }

    private static ClassPathType getClassPathType(String type) {
        if (ClassPath.SOURCE.equals(type)) {
            return null;
        }
        else if (ClassPath.BOOT.equals(type)) {
            return ClassPathType.BOOT;
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return ClassPathType.COMPILE;
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return ClassPathType.RUNTIME;
        }
        else if (JavaClassPathConstants.PROCESSOR_PATH.equals(type)) {
            return ClassPathType.COMPILE;
        }

        return null;
    }

    private enum ClassPathType {
        BOOT,
        COMPILE,
        RUNTIME
    }
}
