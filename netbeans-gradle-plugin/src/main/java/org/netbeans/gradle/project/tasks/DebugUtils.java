package org.netbeans.gradle.project.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

// This class is mostly a copy-paste from the Maven plugin:
//   - org.netbeans.modules.maven.debug.DebuggerChecker
//   - org.netbeans.modules.maven.execute.DefaultReplaceTokenProvider
public final class DebugUtils {
    public static String getActiveClassName(Project project, Lookup lookup) {
        FileObject[] filesOnLookup = extractFileObjectsfromLookup(lookup);
        SourceGroup group = findGroup(ProjectUtils.getSources(project).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA), filesOnLookup);

        FileObject file = null;
        for (FileObject currentFile : filesOnLookup) {
            if (!currentFile.isFolder()) {
                file = currentFile;
                break;
            }
        }

        StringBuilder className = new StringBuilder();
        if (group != null && file != null) {
            if (filesOnLookup.length < 1) {
                return "";
            }

            String relP = FileUtil.getRelativePath(group.getRootFolder(), file.getParent());
            if (relP == null) {
                return "";
            }

            if (!relP.isEmpty()) {
                className.append(relP.replace('/', '.')).append('.');
            }
            className.append(file.getName());
        }
        return className.toString();
    }

    private static FileObject[] extractFileObjectsfromLookup(Lookup lookup) {
        List<FileObject> files = new ArrayList<>(lookup.lookupAll(FileObject.class));
        if (files.isEmpty()) { // fallback to old nodes
            for (DataObject d : lookup.lookupAll(DataObject.class)) {
                files.add(d.getPrimaryFile());
            }
        }
        Collection<? extends SingleMethod> methods = lookup.lookupAll(SingleMethod.class);
        if (methods.size() == 1) {
            SingleMethod method = methods.iterator().next();
            files.add(method.getFile());
        }

        return files.toArray(new FileObject[files.size()]);
    }

    /**
     * Finds the one source group, if any, which contains all of the listed
     * files.
     */
    private static SourceGroup findGroup(SourceGroup[] groups, FileObject[] files) {
        SourceGroup selected = null;
        for (FileObject file : files) {
            for (SourceGroup group : groups) {
                FileObject root = group.getRootFolder();
                if (file == root || FileUtil.isParentOf(root, file)) { // or group.contains(file)?
                    if (selected == null) {
                        selected = group;
                    }
                    else if (selected != group) {
                        return null;
                    }
                }
            }
        }
        return selected;
    }

    public static void applyChanges(Project project, OutputWriter logger, String classname) {
        // check debugger state
        DebuggerEngine debuggerEngine = DebuggerManager.getDebuggerManager().
                getCurrentEngine();
        if (debuggerEngine == null) {
            logger.println("NetBeans: No debugging sessions was found.");
            return;
        }
        JPDADebugger debugger = debuggerEngine.lookupFirst(null, JPDADebugger.class);
        if (debugger == null) {
            logger.println("NetBeans: Current debugger is not JPDA one.");
            return;
        }
        if (!debugger.canFixClasses()) {
            logger.println("NetBeans: The debugger does not support Fix action.");
            return;
        }
        if (debugger.getState() == JPDADebugger.STATE_DISCONNECTED) {
            logger.println("NetBeans: The debugger is not running");
            return;
        }

        Map<String, byte[]> map = new HashMap<>();
        EditorContext editorContext = DebuggerManager.
                getDebuggerManager().lookupFirst(null, EditorContext.class);

        String clazz = classname.replace('.', '/') + ".class"; //NOI18N
        GradleClassPathProvider prv = project.getLookup().lookup(GradleClassPathProvider.class);
        FileObject fo2 = prv.getBuildOutputClassPaths().findResource(clazz);

        if (fo2 != null) {
            try {
                String basename = fo2.getName();
                for (FileObject classfile : fo2.getParent().getChildren()) {
                    String basename2 = classfile.getName();
                    if (/*#220338*/!"class".equals(classfile.getExt()) || (!basename2.equals(basename) && !basename2.startsWith(basename + '$'))) {
                        continue;
                    }
                    String url = classToSourceURL(classfile, logger);
                    if (url != null) {
                        editorContext.updateTimeStamp(debugger, url);
                    }
                    map.put(classname + basename2.substring(basename.length()), classfile.asBytes());
                }
            } catch (IOException ex) {
                NbGradleProject gradleProject = NbGradleProjectFactory.tryGetGradleProject(project);
                if (gradleProject != null) {
                    gradleProject.displayError("Unexpected error.", ex);
                }
                else {
                    throw new IllegalStateException("Unexpected error in an unexpected project type.", ex);
                }
            }
        }

        logger.println("NetBeans: classes to reload: " + map.keySet());
        if (map.isEmpty()) {
            logger.println("NetBeans: No class to reload");
            return;
        }
        String error = null;
        try {
            debugger.fixClasses(map);
        } catch (UnsupportedOperationException uoex) {
            error = "The virtual machine does not support this operation: " + uoex.getLocalizedMessage();
        } catch (NoClassDefFoundError ncdfex) {
            error = "The bytes don't correspond to the class type (the names don't match): " + ncdfex.getLocalizedMessage();
        } catch (VerifyError ver) {
            error = "A \"verifier\" detects that a class, though well formed, contains an internal inconsistency or security problem: " + ver.getLocalizedMessage();
        } catch (UnsupportedClassVersionError ucver) {
            error = "The major and minor version numbers in bytes are not supported by the VM. " + ucver.getLocalizedMessage();
        } catch (ClassFormatError cfer) {
            error = "The bytes do not represent a valid class. " + cfer.getLocalizedMessage();
        } catch (ClassCircularityError ccer) {
            error = "A circularity has been detected while initializing a class: " + ccer.getLocalizedMessage();
        }
        if (error != null) {
            logger.println("NetBeans:" + error);
        }
    }

    private static String classToSourceURL(FileObject fo, OutputWriter logger) {
        ClassPath cp = ClassPath.getClassPath(fo, ClassPath.EXECUTE);
        if (cp == null) {
            return null;
        }
        FileObject root = cp.findOwnerRoot(fo);
        String resourceName = cp.getResourceName(fo, '/', false);
        if (resourceName == null || root == null) {
            logger.println("Can not find classpath resource for " + fo + ", skipping...");
            return null;
        }
        int i = resourceName.indexOf('$');
        if (i > 0) {
            resourceName = resourceName.substring(0, i);
        }
        FileObject[] sRoots = SourceForBinaryQuery.findSourceRoots(root.toURL()).getRoots();
        ClassPath sourcePath = ClassPathSupport.createClassPath(sRoots);
        FileObject rfo = sourcePath.findResource(resourceName + ".java");
        if (rfo == null) {
            return null;
        }
        return rfo.toURL().toExternalForm();
    }

    private DebugUtils() {
        throw new AssertionError();
    }
}
