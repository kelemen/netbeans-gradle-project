package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.JavaSourceGroupID;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.util.ExcludeInclude;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleProjectSources implements Sources, JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectSources.class.getName());

    private static final SourceGroup[] NO_SOURCE_GROUPS = new SourceGroup[0];

    private final JavaExtension javaExt;
    private final ChangeSupport changeSupport;

    private volatile Map<String, SourceGroup[]> currentGroups;

    private final AtomicBoolean hasScanned;
    private final AtomicReference<Object> scanRequestId;

    public GradleProjectSources(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.javaExt = javaExt;
        this.changeSupport = new ChangeSupport(this);
        this.currentGroups = Collections.emptyMap();
        this.hasScanned = new AtomicBoolean(false);
        this.scanRequestId = new AtomicReference<>(null);

        javaExt.getSourceDirsHandler().addDirsCreatedListener(new Runnable() {
            @Override
            public void run() {
                scanForSources();
            }
        });
    }

    public static SourceGroup tryCreateSourceGroup(NamedSourceRoot root) {
        File sourceDir = root.getRoot();

        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            if (groupRoot != null) {
                return new GradleSourceGroup(root, groupRoot);
            }
        }
        return null;
    }

    public static SourceGroup tryCreateSourceGroup(NbListedDir root) {
        File sourceDir = root.getDirectory();

        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            if (groupRoot != null) {
                return new GradleSourceGroup(null, groupRoot, root.getName());
            }
        }
        return null;
    }

    private static <K, V> void addToMultiMap(K key, V value, Map<K, List<V>> map) {
        List<V> sourceGroupList = map.get(key);
        if (sourceGroupList == null) {
            sourceGroupList = new LinkedList<>();
            map.put(key, sourceGroupList);
        }

        sourceGroupList.add(value);
    }

    private static Map<String, List<SourceGroup>> findSourceGroupsOfModule(
            NbJavaModule module) {
        Map<String, List<SourceGroup>> result = new HashMap<>(8);

        for (NamedSourceRoot root: module.getNamedSourceRoots()) {
            SourceGroup newGroup = tryCreateSourceGroup(root);
            if (newGroup == null) {
                continue;
            }

            JavaSourceGroupID groupID = root.getGroupID();

            if (groupID.getGroupName() == JavaSourceGroupName.RESOURCES) {
                addToMultiMap(JavaProjectConstants.SOURCES_TYPE_RESOURCES, newGroup, result);
            }
            else {
                addToMultiMap(JavaProjectConstants.SOURCES_TYPE_JAVA, newGroup, result);
                if (groupID.isTest()) {
                    addToMultiMap(JavaProjectConstants.SOURCES_HINT_TEST, newGroup, result);
                }

                // TODO: Consider "SOURCES_TYPE_GROOVY" and "SOURCES_TYPE_SCALA", "SOURCES_TYPE_ANTLR"
            }
        }

        for (NbListedDir listedDir: module.getListedDirs()) {
            SourceGroup newGroup = tryCreateSourceGroup(listedDir);
            if (newGroup != null) {
                addToMultiMap(JavaProjectConstants.SOURCES_TYPE_RESOURCES, newGroup, result);
            }
        }

        return result;
    }

    private Map<String, SourceGroup[]> findSourceGroups(JavaExtension javaExt) {
        NbJavaModel projectModel = javaExt.getCurrentModel();
        NbJavaModule mainModule = projectModel.getMainModule();

        Map<String, List<SourceGroup>> moduleSources = findSourceGroupsOfModule(mainModule);

        Map<String, SourceGroup[]> result = CollectionUtils.newHashMap(moduleSources.size());
        for (Map.Entry<String, List<SourceGroup>> entry: moduleSources.entrySet()) {
            List<SourceGroup> entryValue = entry.getValue();
            result.put(entry.getKey(), entryValue.toArray(new SourceGroup[entryValue.size()]));
        }

        SourceGroup[] sources = result.get(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (sources != null && sources.length > 0) {
            result.put(JavaProjectConstants.SOURCES_HINT_MAIN, new SourceGroup[]{sources[0]});
        }

        result.put(Sources.TYPE_GENERIC, getGenericGroup());

        return result;
    }

    @Override
    public void onModelChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scanForSources();
            }
        });
    }

    public void scanForSources() {
        scanForSources(false);
    }

    public void ensureScanForSources() {
        scanForSources(true);
    }

    private void scanForSources(boolean initialScan) {
        if (initialScan) {
            if (!hasScanned.compareAndSet(false, true)) {
                return;
            }
        }

        final Object requestId = new Object();
        if (!scanRequestId.compareAndSet(null, requestId)) {
            return;
        }

        hasScanned.set(true);
        NbGradleProject.PROJECT_PROCESSOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                scanRequestId.compareAndSet(requestId, null);

                Map<String, SourceGroup[]> groups = findSourceGroups(javaExt);

                currentGroups = groups;
                LOGGER.log(Level.FINE, "Location of the sources of {0} has been updated.", javaExt.getName());

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        changeSupport.fireChange();
                    }
                });
            }
        }, null);
    }

    private SourceGroup[] getGenericGroup() {
        return new SourceGroup[]{
            new GradleSourceGroup(javaExt.getProjectDirectory())
        };
    }

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        ensureScanForSources();

        SourceGroup[] foundGroup = currentGroups.get(type);
        if (foundGroup == null && Sources.TYPE_GENERIC.equals(type)) {
            return getGenericGroup();
        }
        else {
            return foundGroup != null ? foundGroup.clone() : NO_SOURCE_GROUPS;
        }
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    private static class GradleSourceGroup implements SourceGroup {
        private final NamedSourceRoot parent;
        private final FileObject location;
        private final PropertyChangeSupport changes;
        private final String displayName;

        private final AtomicReference<Path> locationPathRef;

        public GradleSourceGroup(FileObject location) {
            this(null, location, NbStrings.getSrcPackageCaption());
        }

        public GradleSourceGroup(NamedSourceRoot parent, FileObject location) {
            this(parent, location, parent.getDisplayName());
        }

        public GradleSourceGroup(NamedSourceRoot parent, FileObject location, String displayName) {
            this.parent = parent;
            this.location = location;
            this.displayName = displayName;
            this.changes = new PropertyChangeSupport(this);
            this.locationPathRef = new AtomicReference<>(null);
        }

        public Path getRootPath() {
            Path result = locationPathRef.get();
            if (result == null) {
                result = GradleFileUtils.toPath(location);
                if (!locationPathRef.compareAndSet(null, result)) {
                    result = locationPathRef.get();
                }
            }
            return result;
        }

        @Override
        public FileObject getRootFolder() {
            return location;
        }

        @Override
        public String getName() {
            String locationStr = location.getPath();
            return locationStr.length() > 0 ? locationStr : "generic";
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public Icon getIcon(boolean opened) {
            return null;
        }

        private boolean rulesAllow(FileObject file) {
            if (parent == null) {
                return true;
            }

            Path path = GradleFileUtils.toPath(file);
            if (path == null) {
                return true;
            }

            Path rootPath = getRootPath();
            if (rootPath == null) {
                return true;
            }

            return ExcludeInclude.includeFile(
                    path,
                    rootPath,
                    parent.getExcludePatterns(),
                    parent.getIncludePatterns());
        }

        @Override
        public boolean contains(FileObject file) {
            if (file == location) {
                return true;
            }

            if (FileUtil.getRelativePath(location, file) == null) {
                return false;
            }

            if (!rulesAllow(file)) {
                return false;
            }

            URI f = file.toURI();

            // else MIXED, UNKNOWN, or SHARABLE; or not a disk file
            return f == null || SharabilityQuery.getSharability(f) != SharabilityQuery.Sharability.NOT_SHARABLE;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
            changes.addPropertyChangeListener(l);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
            changes.removePropertyChangeListener(l);
        }

        @Override
        public String toString() {
            return "GradleSources.Group[name=" + getName() + ",rootFolder=" + getRootFolder() + "]";
        }
    }
}
