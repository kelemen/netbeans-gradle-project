package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
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
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.SharabilityQuery;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Kelemen Attila
 */
public final class GradleProjectSources implements Sources {
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
        sourceDir.mkdirs();
        if (sourceDir.isDirectory()) {
            FileObject groupRoot = FileUtil.toFileObject(sourceDir);
            return new GradleSourceGroup(groupRoot, caption);
        }
        else {
            return null;
        }
    }

    private static Map<String, SourceGroup[]> findSourceGroups(NbGradleProject project) {
        NbProjectModel projectModel = project.loadProject();

        Map<String, SourceGroup[]> groups = new HashMap<String, SourceGroup[]>();

        String sourceGroupCaption = NbBundle.getMessage(GradleProjectSources.class, "LBL_SrcJava");
        String resourceGroupCaption = NbBundle.getMessage(GradleProjectSources.class, "LBL_Resources");
        String testGroupCaption = NbBundle.getMessage(GradleProjectSources.class, "LBL_TestJava");
        String testResourceGroupCaption = NbBundle.getMessage(GradleProjectSources.class, "LBL_TestResources");

        List<SourceGroup> sources = new LinkedList<SourceGroup>();
        List<SourceGroup> resources = new LinkedList<SourceGroup>();
        List<SourceGroup> testSources = new LinkedList<SourceGroup>();
        List<SourceGroup> testResources = new LinkedList<SourceGroup>();

        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);

        if (mainModule != null) {
            for (IdeaContentRoot contentRoot: mainModule.getContentRoots()) {
                for (IdeaSourceDirectory sourceDir: contentRoot.getSourceDirectories()) {
                    File dir = sourceDir.getDirectory();

                    if (NbProjectModelUtils.isResourcePath(sourceDir)) {
                        SourceGroup group = createSourceGroup(dir, resourceGroupCaption);
                        if (group != null) resources.add(group);
                    }
                    else {
                        SourceGroup group = createSourceGroup(dir, sourceGroupCaption);
                        if (group != null) sources.add(group);
                    }
                }
                for (IdeaSourceDirectory sourceDir: contentRoot.getTestDirectories()) {
                    File dir = sourceDir.getDirectory();

                    if (NbProjectModelUtils.isResourcePath(sourceDir)) {
                        SourceGroup group = createSourceGroup(dir, testResourceGroupCaption);
                        if (group != null) testResources.add(group);
                    }
                    else {
                        SourceGroup group = createSourceGroup(dir, testGroupCaption);
                        if (group != null) testSources.add(group);
                    }
                }
            }
        }

        // TODO: add settings.gradle to TYPE_GENERIC
        groups.put(Sources.TYPE_GENERIC, new SourceGroup[] {new GradleSourceGroup(project.getProjectDirectory(), project.getDisplayName())});
        groups.put(GradleProjectConstants.SOURCES, sources.toArray(NO_SOURCE_GROUPS));
        groups.put(GradleProjectConstants.RESOURCES, resources.toArray(NO_SOURCE_GROUPS));
        groups.put(GradleProjectConstants.TEST_SOURCES, testSources.toArray(NO_SOURCE_GROUPS));
        groups.put(GradleProjectConstants.TEST_RESOURCES, testResources.toArray(NO_SOURCE_GROUPS));

        return groups;
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

                ProgressHandle progress = null;
                try {
                    progress = ProgressHandleFactory.createHandle(
                            NbBundle.getMessage(GradleProjectSources.class, "LBL_ScanningForSource"));
                    progress.start();

                    Map<String, SourceGroup[]> groups = findSourceGroups(project);

                    currentGroups = groups;
                    LOGGER.log(Level.FINE, "Location of the sources of {0} has been updated.", project.getName());

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            changeSupport.fireChange();
                        }
                    });
                } finally {
                    progress.finish();
                }
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
