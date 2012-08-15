package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceType;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleProjectSources implements Sources, ProjectChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectSources.class.getName());

    private static final SourceGroup[] NO_SOURCE_GROUPS = new SourceGroup[0];

    private final NbGradleProject project;
    private final ChangeSupport changeSupport;

    private volatile Map<String, SourceGroup[]> currentGroups;

    private final AtomicBoolean hasScanned;
    private final AtomicReference<Object> scanRequestId;

    public GradleProjectSources(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.changeSupport = new ChangeSupport(this);
        this.currentGroups = Collections.emptyMap();
        this.hasScanned = new AtomicBoolean(false);
        this.scanRequestId = new AtomicReference<Object>(null);
    }

    private static SourceGroup createSourceGroup(File sourceDir, String caption) {
        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            if (groupRoot != null) {
                return new GradleSourceGroup(groupRoot, caption);
            }
        }
        return null;
    }

    private static SourceGroup[] toSourceGroup(String displayName, List<File> rootDirs) {
        List<SourceGroup> result = new ArrayList<SourceGroup>(rootDirs.size());
        for (File dir: rootDirs) {
            SourceGroup group = createSourceGroup(dir, displayName);
            if (group != null) {
                result.add(group);
            }
        }
        return result.toArray(NO_SOURCE_GROUPS);
    }

    private static SourceGroup[] mergeGroups(SourceGroup[]... groups) {
        int size = 0;
        for (SourceGroup[] group: groups) {
            size += group.length;
        }

        SourceGroup[] result = new SourceGroup[size];
        int offset = 0;
        for (SourceGroup[] group: groups) {
            System.arraycopy(group, 0, result, offset, group.length);
            offset += group.length;
        }
        return result;
    }

    private static Map<String, SourceGroup[]> findSourceGroups(NbGradleProject project) {
        NbGradleModel projectModel = project.getCurrentModel();
        NbGradleModule mainModule = projectModel.getMainModule();

        Map<String, SourceGroup[]> groups = new HashMap<String, SourceGroup[]>();

        String sourceGroupCaption = NbStrings.getSrcPackageCaption();
        String resourceGroupCaption = NbStrings.getResourcesPackageCaption();
        String testGroupCaption = NbStrings.getTestPackageCaption();
        String testResourceGroupCaption = NbStrings.getTestResourcesPackageCaption();

        SourceGroup[] sources = toSourceGroup(
                sourceGroupCaption,
                mainModule.getSources(NbSourceType.SOURCE).getPaths());
        SourceGroup[] resources = toSourceGroup(
                resourceGroupCaption,
                mainModule.getSources(NbSourceType.RESOURCE).getPaths());
        SourceGroup[] testSources = toSourceGroup(
                testGroupCaption,
                mainModule.getSources(NbSourceType.TEST_SOURCE).getPaths());
        SourceGroup[] testResources = toSourceGroup(
                testResourceGroupCaption,
                mainModule.getSources(NbSourceType.TEST_RESOURCE).getPaths());

        if (sources.length > 0) {
            groups.put(JavaProjectConstants.SOURCES_HINT_MAIN, new SourceGroup[]{sources[0]});
        }

        groups.put(JavaProjectConstants.SOURCES_TYPE_JAVA, mergeGroups(sources, testSources));
        groups.put(JavaProjectConstants.SOURCES_HINT_TEST, testSources);
        groups.put(JavaProjectConstants.SOURCES_TYPE_RESOURCES, mergeGroups(resources, testResources));

        groups.put(GradleProjectConstants.SOURCES, sources);
        groups.put(GradleProjectConstants.RESOURCES, resources);
        groups.put(GradleProjectConstants.TEST_SOURCES, testSources);
        groups.put(GradleProjectConstants.TEST_RESOURCES, testResources);

        groups.put(Sources.TYPE_GENERIC, new SourceGroup[] {new GradleSourceGroup(project.getProjectDirectory(), project.getDisplayName())});

        return groups;
    }

    @Override
    public void projectChanged() {
        scanForSources();
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
        NbGradleProject.PROJECT_PROCESSOR.submit(new Runnable() {
            @Override
            public void run() {
                scanRequestId.compareAndSet(requestId, null);

                Map<String, SourceGroup[]> groups = findSourceGroups(project);

                currentGroups = groups;
                LOGGER.log(Level.FINE, "Location of the sources of {0} has been updated.", project.getName());

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        changeSupport.fireChange();
                    }
                });
            }
        });
    }

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        ensureScanForSources();

        SourceGroup[] foundGroup = currentGroups.get(type);
        if (foundGroup == null && Sources.TYPE_GENERIC.equals(type)) {
            foundGroup = new SourceGroup[] {new GradleSourceGroup(project.getProjectDirectory(), project.getDisplayName())};
        }

        return foundGroup != null ? foundGroup.clone() : NO_SOURCE_GROUPS;
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
        private final FileObject location;
        private final PropertyChangeSupport changes;
        private final String displayName;

        public GradleSourceGroup(FileObject location, String displayName) {
            this.location = location;
            this.displayName = displayName;
            this.changes = new PropertyChangeSupport(this);
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

        @Override
        public boolean contains(FileObject file) {
            if (file == location) {
                return true;
            }

            if (FileUtil.getRelativePath(location, file) == null) {
                return false;
            }

            URI f = file.toURI();
            if (f != null && SharabilityQuery.getSharability(f) == SharabilityQuery.Sharability.NOT_SHARABLE) {
                return false;
            } // else MIXED, UNKNOWN, or SHARABLE; or not a disk file
            return true;
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
