package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.utils.LazyValues;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.MultiMapUtils;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.JavaSourceGroupID;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;

public final class GradleProjectSources implements Sources, JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectSources.class.getName());

    private static final SourceGroup[] NO_SOURCE_GROUPS = new SourceGroup[0];

    private final JavaExtension javaExt;
    private final ChangeSupport changeSupport;

    private volatile Map<String, SourceGroup[]> currentGroups;

    private final AtomicBoolean hasScanned;
    private final UpdateTaskExecutor scanSourcesExecutor;

    public GradleProjectSources(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.changeSupport = new ChangeSupport(this);
        this.currentGroups = Collections.emptyMap();
        this.hasScanned = new AtomicBoolean(false);
        this.scanSourcesExecutor = NbTaskExecutors.newDefaultUpdateExecutor();

        javaExt.getSourceDirsHandler().addDirsCreatedListener(this::scanForSources);
    }

    public static SingleNodeFactory tryCreateSourceGroupNodeFactory(NamedSourceRoot root) {
        SourceGroup group = tryCreateSourceGroup(root);
        return group != null ? createSourceGroupNodeFactory(root, group) : null;
    }

    public static SingleNodeFactory createSourceGroupNodeFactory(Object sourceGroupKey, SourceGroup group) {
        return new SourceRootNodeFactory(sourceGroupKey, group);
    }

    public static SourceGroup tryCreateSourceGroup(NamedSourceRoot root) {
        File sourceDir = root.getRoot();

        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            if (groupRoot != null) {
                return new GradleSourceGroup(groupRoot, root.getDisplayName(), root.getIncludeRules());
            }
        }
        return null;
    }

    public static SourceGroup tryCreateSourceGroup(NbListedDir root) {
        File sourceDir = root.getDirectory();

        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            if (groupRoot != null) {
                return new GradleSourceGroup(groupRoot, root.getName());
            }
        }
        return null;
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
                MultiMapUtils.addToMultiMap(JavaProjectConstants.SOURCES_TYPE_RESOURCES, newGroup, result);
            }
            else {
                MultiMapUtils.addToMultiMap(JavaProjectConstants.SOURCES_TYPE_JAVA, newGroup, result);
                if (groupID.isTest()) {
                    MultiMapUtils.addToMultiMap(JavaProjectConstants.SOURCES_HINT_TEST, newGroup, result);
                }

                // TODO: Consider "SOURCES_TYPE_GROOVY" and "SOURCES_TYPE_SCALA", "SOURCES_TYPE_ANTLR"
            }
        }

        for (NbListedDir listedDir: module.getListedDirs()) {
            SourceGroup newGroup = tryCreateSourceGroup(listedDir);
            if (newGroup != null) {
                MultiMapUtils.addToMultiMap(JavaProjectConstants.SOURCES_TYPE_RESOURCES, newGroup, result);
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
        SwingUtilities.invokeLater(this::scanForSources);
    }

    public void scanForSources() {
        scanForSources(false);
    }

    public void ensureScanForSources() {
        scanForSources(true);
    }

    private void scanForSources(boolean initialScan) {
        if (!hasScanned.compareAndSet(false, true)) {
            if (initialScan) {
                return;
            }
        }

        scanSourcesExecutor.execute(() -> {
            Map<String, SourceGroup[]> groups = findSourceGroups(javaExt);

            currentGroups = groups;
            LOGGER.log(Level.FINE, "Location of the sources of {0} has been updated.", javaExt.getName());

            SwingUtilities.invokeLater(changeSupport::fireChange);
        });
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

    private static class SourceRootNodeFactory implements SingleNodeFactory {
        private final Object sourceGroupKey;
        private final SourceGroup group;

        public SourceRootNodeFactory(Object sourceGroupKey, SourceGroup group) {
            this.sourceGroupKey = sourceGroupKey;
            this.group = Objects.requireNonNull(group, "group");
        }

        @Override
        public Node createNode() {
            return PackageView.createPackageView(group);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.sourceGroupKey);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)return false;
            if (getClass() != obj.getClass()) return false;

            final SourceRootNodeFactory other = (SourceRootNodeFactory)obj;
            return Objects.equals(this.sourceGroupKey, other.sourceGroupKey);
        }
    }

    private static class GradleSourceGroup implements SourceGroup {
        private final ExcludeIncludeRules includeRules;
        private final FileObject location;
        private final PropertyChangeSupport changes;
        private final String displayName;

        private final Supplier<Path> locationPathRef;

        public GradleSourceGroup(FileObject location) {
            this(location, NbStrings.getSrcPackageCaption());
        }

        public GradleSourceGroup(FileObject location, String displayName) {
            this(location, displayName, ExcludeIncludeRules.ALLOW_ALL);
        }

        public GradleSourceGroup(FileObject location, String displayName, ExcludeIncludeRules includeRules) {
            this.includeRules = includeRules;
            this.location = location;
            this.displayName = displayName;
            this.changes = new PropertyChangeSupport(this);
            this.locationPathRef = LazyValues.lazyValue(() -> GradleFileUtils.toPath(location));
        }

        public Path getRootPath() {
            return locationPathRef.get();
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
            Path rootPath = getRootPath();
            if (rootPath == null) {
                return true;
            }

            boolean result = includeRules.isIncluded(rootPath, file);
            // Directories are always allowed because otherwise
            // package view might skip the entire directory, regardless that
            // it might contain included sub directories.
            return result || file.isFolder();
        }

        @Override
        public boolean contains(FileObject file) {
            if (file == location) {
                return true;
            }

            if (FileUtil.getRelativePath(location, file) == null) {
                return false;
            }

            return rulesAllow(file);
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
