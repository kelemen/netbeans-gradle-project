package org.netbeans.gradle.project.java.nodes;

import java.awt.Image;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.JavaSourceGroupID;
import org.netbeans.gradle.project.java.model.NamedFile;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.query.GradleProjectSources;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.BadgeAwareNode;
import org.netbeans.gradle.project.view.NodeUtils;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class JavaSourceSetNode extends AbstractNode {
    private final String displayName;

    private JavaSourceSetNode(JavaExtension javaExt, String sourceSetName) {
        this(sourceSetName, new JavaSourceSetNodeChildFactory(javaExt, sourceSetName));
    }

    private JavaSourceSetNode(String sourceSetName, JavaSourceSetNodeChildFactory childFactory) {
        this(sourceSetName, childFactory, createChildren(childFactory));
    }

    private JavaSourceSetNode(
            String sourceSetName,
            JavaSourceSetNodeChildFactory childFactory,
            Children children) {
        super(children, createLookup(childFactory, children));

        this.displayName = StringUtils.capitalizeFirstCharacter(sourceSetName);

        setName("java.sourceset." + sourceSetName);
    }

    private static Lookup createLookup(JavaSourceSetNodeChildFactory childFactory, Children children) {
        return Lookups.fixed(
                NodeUtils.askChildrenPackageViewsFinder(),
                NodeUtils.defaultNodeRefresher(children, childFactory));
    }

    private static Children createChildren(JavaSourceSetNodeChildFactory childFactory) {
        return Children.create(childFactory, true);
    }

    public static SingleNodeFactory createFactory(JavaExtension javaExt, String sourceSetName) {
        return new JavaSourceSetNodeFactory(javaExt, sourceSetName);
    }

    @Override
    public Image getIcon(int type) {
        return NbIcons.getPackageIcon();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return NbIcons.getOpenPackageIcon();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    private static class JavaSourceSetNodeChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            RefreshableChildren {

        private final JavaExtension javaExt;
        private final String sourceSetName;

        private final ListenerRegistrations listenerRegs;

        public JavaSourceSetNodeChildFactory(JavaExtension javaExt, String sourceSetName) {
            this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
            this.sourceSetName = Objects.requireNonNull(sourceSetName, "sourceSetName");
            this.listenerRegs = new ListenerRegistrations();
        }

        @Override
        public void refreshChildren() {
            refresh(false);
        }

        @Override
        protected void addNotify() {
            listenerRegs.unregisterAll();
            listenerRegs.add(javaExt.addModelChangeListener(() -> refresh(false)));
        }

        @Override
        protected void removeNotify() {
            listenerRegs.unregisterAll();
        }

        private JavaSourceSet tryGetSourceSet() {
            return javaExt.getCurrentModel().getMainModule().tryGetSourceSetByName(sourceSetName);
        }

        private static int compareGroupNames(JavaSourceGroupName groupName1, JavaSourceGroupName groupName2) {
            if (groupName1 == groupName2) {
                return 0;
            }
            if (groupName1 == JavaSourceGroupName.OTHER) {
                return 1;
            }
            if (groupName2 == JavaSourceGroupName.OTHER) {
                return -1;
            }
            if (groupName1 == JavaSourceGroupName.RESOURCES) {
                return 1;
            }
            if (groupName2 == JavaSourceGroupName.RESOURCES) {
                return -1;
            }

            return StringUtils.STR_CMP.compare(groupName1.name(), groupName2.name());
        }

        private static JavaSourceGroup[] sortGroups(Collection<JavaSourceGroup> sourceGroups) {
            JavaSourceGroup[] result = sourceGroups.toArray(new JavaSourceGroup[sourceGroups.size()]);
            Arrays.sort(result, Comparator.comparing(JavaSourceGroup::getGroupName, JavaSourceSetNodeChildFactory::compareGroupNames));
            return result;
        }

        private void addSourceRootNode(List<SingleNodeFactory> toPopulate, NamedSourceRoot root) {
            SingleNodeFactory nodeFactory = GradleProjectSources.tryCreateSourceGroupNodeFactory(root);
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }

        @Override
        protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
            JavaSourceSet sourceSet = tryGetSourceSet();
            if (sourceSet != null) {
                for (JavaSourceGroup sourceGroup: sortGroups(sourceSet.getSourceGroups())) {
                    String groupName = NamedSourceRoot.getSourceGroupDisplayName(sourceGroup);
                    Set<File> roots = sourceGroup.getSourceRoots();
                    ExcludeIncludeRules includeRules = ExcludeIncludeRules.create(sourceGroup);
                    JavaSourceGroupID groupID = new JavaSourceGroupID(sourceSetName, sourceGroup.getGroupName());

                    if (roots.size() == 1) {
                        addSourceRootNode(toPopulate, new NamedSourceRoot(
                                groupID,
                                groupName,
                                roots.iterator().next(),
                                includeRules));
                    }
                    else {
                        for (NamedFile namedRoot: NamedSourceRoot.nameSourceRoots(roots)) {
                            addSourceRootNode(toPopulate, new NamedSourceRoot(
                                    groupID,
                                    NbStrings.getMultiRootSourceGroups(groupName, namedRoot.getName()),
                                    namedRoot.getPath(),
                                    includeRules));
                        }
                    }
                }
            }
            return true;
        }
    }

    private static class SourceSetFilesProperty implements PropertySource<Set<File>> {
        private final JavaExtension javaExt;
        private final String sourceSetName;

        public SourceSetFilesProperty(JavaExtension javaExt, String sourceSetName) {
            this.javaExt = javaExt;
            this.sourceSetName = sourceSetName;
        }

        @Override
        public Set<File> getValue() {
            JavaSourceSet sourceSet = javaExt.getCurrentModel().getMainModule().tryGetSourceSetByName(sourceSetName);
            if (sourceSet == null) {
                return Collections.emptySet();
            }

            Set<File> roots = new HashSet<>();
            for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
                for (File root: group.getSourceRoots()) {
                    roots.add(root);
                }
            }
            return roots;
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return javaExt.addModelChangeListener(listener);
        }

    }

    private static class JavaSourceSetNodeFactory implements SingleNodeFactory {
        private final JavaExtension javaExt;
        private final String sourceSetName;
        private final PropertySource<Set<File>> sourceSetFiles;

        public JavaSourceSetNodeFactory(JavaExtension javaExt, String sourceSetName) {
            this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
            this.sourceSetName = Objects.requireNonNull(sourceSetName, "sourceSetName");
            this.sourceSetFiles = new SourceSetFilesProperty(javaExt, sourceSetName);
        }

        @Override
        public Node createNode() {
            return BadgeAwareNode.makeBadgeAware(new JavaSourceSetNode(javaExt, sourceSetName), sourceSetFiles);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + System.identityHashCode(javaExt);
            hash = 71 * hash + sourceSetName.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final JavaSourceSetNodeFactory other = (JavaSourceSetNodeFactory)obj;
            return this.javaExt == other.javaExt
                    && Objects.equals(this.sourceSetName, other.sourceSetName);
        }
    }
}
