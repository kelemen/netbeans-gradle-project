package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.gradle.project.util.StringUtils;

public final class NamedSourceRoot {
    private final JavaSourceGroupID groupID;
    private final String displayName;
    private final File root;

    private final ExcludeIncludeRules includeRules;

    public NamedSourceRoot(
            JavaSourceGroupID groupID,
            String displayName,
            File root,
            ExcludeIncludeRules includeRules) {

        this.groupID = Objects.requireNonNull(groupID, "groupID");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.root = Objects.requireNonNull(root, "root");
        this.includeRules = Objects.requireNonNull(includeRules, "includeRules");
    }

    public JavaSourceGroupID getGroupID() {
        return groupID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public File getRoot() {
        return root;
    }

    public ExcludeIncludeRules getIncludeRules() {
        return includeRules;
    }

    private static String displayNameOfSourceSet(String sourceSetName) {
        if (sourceSetName.isEmpty()) {
            return "?";
        }
        else {
            return StringUtils.capitalizeFirstCharacter(sourceSetName);
        }
    }

    private static int countNonResource(Collection<JavaSourceGroup> sourceGroups) {
        int result = 0;
        for (JavaSourceGroup sourceGroup: sourceGroups) {
            if (sourceGroup.getGroupName() != JavaSourceGroupName.RESOURCES) {
                result++;
            }
        }
        return result;
    }

    public static String getSourceGroupDisplayName(JavaSourceGroup sourceGroup) {
        JavaSourceGroupName groupName = sourceGroup.getGroupName();
        if (groupName == JavaSourceGroupName.RESOURCES) {
            return NbStrings.getResourcesPackageCaption();
        }

        return StringUtils.capitalizeFirstCharacter(groupName.toString().toLowerCase(Locale.ROOT));
    }

    public static List<NamedSourceRoot> getAllSourceRoots(NbJavaModule module) {
        List<NamedSourceRoot> result = new ArrayList<>();

        for (JavaSourceSet sourceSet: module.getSources()) {
            String sourceSetName = sourceSet.getName();
            String displaySourceSetName = displayNameOfSourceSet(sourceSetName);

            String mainName;
            switch (sourceSetName) {
                case JavaSourceSet.NAME_MAIN:
                    mainName = NbStrings.getSrcPackageCaption();
                    break;
                case JavaSourceSet.NAME_TEST:
                    mainName = NbStrings.getTestPackageCaption();
                    break;
                default:
                    mainName = null;
                    break;
            }

            Collection<JavaSourceGroup> sourceGroups = sourceSet.getSourceGroups();
            int nonResourceCount = countNonResource(sourceGroups);

            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                JavaSourceGroupName groupName = sourceGroup.getGroupName();
                JavaSourceGroupID groupID = new JavaSourceGroupID(sourceSetName, groupName);

                Set<File> sourceRoots = sourceGroup.getSourceRoots();

                String groupNamePrefix;
                if (groupName == JavaSourceGroupName.RESOURCES) {
                    groupNamePrefix = NbStrings.getResourcesPackageCaption() + " [" + displaySourceSetName + "]";
                }
                else if (nonResourceCount == 1) {
                    groupNamePrefix = mainName != null
                            ? mainName
                            : NbStrings.getOtherPackageCaption(displaySourceSetName);
                }
                else {
                    String groupDisplayName = getSourceGroupDisplayName(sourceGroup);
                    groupNamePrefix = mainName != null
                            ? mainName + " [" + groupDisplayName + "]"
                            :  NbStrings.getOtherPackageCaption(displaySourceSetName + "/" + groupDisplayName);
                }

                ExcludeIncludeRules includeRules = ExcludeIncludeRules.create(sourceGroup);

                if (sourceRoots.size() == 1) {
                    result.add(new NamedSourceRoot(
                            groupID,
                            groupNamePrefix,
                            sourceRoots.iterator().next(),
                            includeRules));
                }
                else {
                    for (NamedFile root: nameSourceRoots(sourceRoots)) {
                        String rootName = NbStrings.getMultiRootSourceGroups(groupNamePrefix, root.getName());
                        result.add(new NamedSourceRoot(
                                groupID,
                                rootName,
                                root.getPath(),
                                includeRules));
                    }
                }
            }
        }

        return sortNamedSourceRoots(result);
    }

    private static List<NamedSourceRoot> sortNamedSourceRoots(Collection<NamedSourceRoot> namedRoots) {
        NamedSourceRoot[] orderedRoots = namedRoots.toArray(new NamedSourceRoot[namedRoots.size()]);
        Arrays.sort(orderedRoots, (NamedSourceRoot root1, NamedSourceRoot root2) -> {
            JavaSourceGroupID groupID1 = root1.getGroupID();
            JavaSourceGroupID groupID2 = root2.getGroupID();

            String sourceSetName1 = groupID1.getSourceSetName();
            String sourceSetName2 = groupID2.getSourceSetName();

            if (sourceSetName1.equals(sourceSetName2)) {
                return compareGroups(groupID1.getGroupName(), groupID2.getGroupName());
            }

            return compareSourceSetNames(sourceSetName1, sourceSetName2);
        });

        return Arrays.asList(orderedRoots);
    }

    private static int getOrderOfGroup(JavaSourceGroupName groupName) {
        // If we have Groovy or Scala sources, Groovy and Scala source roots
        // are preferred over Java because joint compilation is more convenient.
        switch (groupName) {
            case GROOVY:
                return 0;
            case SCALA:
                return 1;
            case JAVA:
                return 2;
            case OTHER:
                return 3;
            case RESOURCES:
                return 4;
            default:
                throw new AssertionError(groupName.name());
        }
    }

    private static int compareGroups(JavaSourceGroupName name1, JavaSourceGroupName name2) {
        int order1 = getOrderOfGroup(name1);
        int order2 = getOrderOfGroup(name2);

        return order1 - order2;
    }

    public static int compareSourceSetNames(String name1, String name2) {
        if (JavaSourceSet.NAME_MAIN.equals(name1)) {
            return -1;
        }

        if (JavaSourceSet.NAME_MAIN.equals(name2)) {
            return 1;
        }

        if (JavaSourceSet.NAME_TEST.equals(name1)) {
            return -1;
        }

        if (JavaSourceSet.NAME_TEST.equals(name2)) {
            return 1;
        }

        return StringUtils.STR_CMP.compare(name1, name2);
    }

    private static <K, V> void addToMap(Map<K, List<V>> map, K key, V value) {
        List<V> valueList = map.get(key);
        if (valueList == null) {
            valueList = new ArrayList<>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static List<NamedFile> nameSourceRoots(Collection<File> files) {
        // The common case
        if (files.size() == 1) {
            File file = files.iterator().next();
            return Collections.singletonList(new NamedFile(file, file.getName()));
        }

        Map<String, List<FileWithBase>> nameToFile = CollectionUtils.newHashMap(files.size());

        int fileIndex = 0;
        for (File file: files) {
            String name = file.getName();
            File parent = file.getParentFile();
            addToMap(nameToFile, name, new FileWithBase(fileIndex, parent, file));
            fileIndex++;
        }

        boolean didSomething;
        do {
            didSomething = false;

            List<Map.Entry<String, List<FileWithBase>>> currentEntries
                    = new ArrayList<>(nameToFile.entrySet());
            for (Map.Entry<String, List<FileWithBase>> entry: currentEntries) {
                String entryName = entry.getKey();
                List<FileWithBase> entryFiles = entry.getValue();

                int renameableCount = 0;
                for (FileWithBase file: entryFiles) {
                    if (file.base != null) renameableCount++;
                }

                if (renameableCount > 1) {
                    nameToFile.remove(entryName);
                    for (FileWithBase file: entryFiles) {
                        if (file.base != null) {
                            String newName = file.base.getName() + '/' + entryName;
                            File newParent = file.base.getParentFile();
                            addToMap(nameToFile,
                                    newName,
                                    new FileWithBase(file.index, newParent, file.file));
                        }
                        else {
                            addToMap(nameToFile, entryName, file);
                        }
                    }
                    didSomething = true;
                }
            }
        } while (didSomething);

        NamedFile[] result = new NamedFile[fileIndex];
        for (Map.Entry<String, List<FileWithBase>> entry: nameToFile.entrySet()) {
            String entryName = entry.getKey();
            for (FileWithBase file: entry.getValue()) {
                result[file.index] = new NamedFile(file.file, entryName);
            }
        }

        return Arrays.asList(result);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + groupID.hashCode();
        hash = 23 * hash + displayName.hashCode();
        hash = 23 * hash + root.hashCode();
        hash = 23 * hash + includeRules.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final NamedSourceRoot other = (NamedSourceRoot)obj;
        if (!groupID.equals(other.groupID)) {
            return false;
        }
        if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (!includeRules.equals(other.includeRules)) {
            return false;
        }
        return root.equals(other.root);
    }

    private static final class FileWithBase {
        public final int index;
        public final File base;
        public final File file;

        public FileWithBase(int index, File base, File file) {
            assert file != null;

            this.index = index;
            this.base = base;
            this.file = file;
        }
    }
}
