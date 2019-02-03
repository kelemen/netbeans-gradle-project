package org.netbeans.gradle.project.java.query;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.Tree;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.event.ChangeListener;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.SimpleListenerRegistry;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingPropertySource;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.FileSystemWatcher;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.modules.java.preprocessorbridge.api.ModuleUtilities;
import org.netbeans.spi.java.queries.CompilerOptionsQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleCompilerOptionsQuery implements CompilerOptionsQueryImplementation, JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleCompilerOptionsQuery.class.getName());

    private static final FileSystemWatcher WATCHER = FileSystemWatcher.getDefault();
    private static final String MODULE_INFO_JAVA = "module-info.java";

    private final GradleProperty.SourceLevel sourceLevelRef;
    private final JavaExtension javaExt;
    private final ListenerRegistrations pathRegs;
    private final UpdateTaskExecutor moduleInfoLoadExecutor;
    private final MutableProperty<Map<String, List<String>>> argumentsBySourceSetRef;
    private final SwingPropertySource<Map<String, List<String>>, ChangeListener> argumentsBySourceSetOldRef;

    public GradleCompilerOptionsQuery(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.pathRegs = new ListenerRegistrations();
        this.moduleInfoLoadExecutor = NbTaskExecutors.newDefaultUpdateExecutor();
        this.argumentsBySourceSetRef = PropertyFactory.memProperty(Collections.emptyMap());
        this.argumentsBySourceSetOldRef = NbProperties.toOldProperty(argumentsBySourceSetRef);
        this.sourceLevelRef = javaExt.getOwnerProjectLookup().lookup(GradleProperty.SourceLevel.class);
    }

    @Override
    public Result getOptions(FileObject file) {
        if (!javaExt.getProjectProperties().allowModules().getActiveValue()) {
            return null;
        }

        JavaSourceSet sourceSet = GradleClassPathProvider.findAssociatedSourceSet(javaExt.getCurrentModel(), file);
        if (sourceSet == null || JavaSourceSet.NAME_MAIN.equals(sourceSet.getName())) {
            return null;
        }

        return new ResultImpl(sourceSet.getName(), argumentsBySourceSetOldRef);
    }

    @Override
    public void onModelChange() {
        updateModuleInfosAsync();
    }

    private void updateModuleInfosAsync() {
        moduleInfoLoadExecutor.execute(this::updateModuleInfos);
    }

    private void updateModuleInfos() {
        NbJavaModel model = javaExt.currentModel().getValue();
        updateModuleInfos(model.getMainModule());
    }

    private void registerChangeListener(PropertySource<?> property) {
        registerChangeListener(property::addChangeListener);
    }

    private void registerChangeListener(SimpleListenerRegistry<Runnable> changeListener) {
        pathRegs.add(NbProperties.weakListenerRegistry(changeListener).registerListener(this::updateModuleInfosAsync));
    }

    private static Stream<File> sourceRoots(JavaSourceSet sourceSet) {
        return sourceSet.getSourceGroups()
                .stream()
                .flatMap(group -> group.getSourceRoots().stream());
    }

    private ModuleInfo tryGetModuleInfo(JavaSourceSet sourceSet) {
        return sourceRoots(sourceSet)
                .map(root -> root.toPath().resolve(MODULE_INFO_JAVA))
                .filter(moduleInfoPath -> Files.isRegularFile(moduleInfoPath))
                .map(GradleCompilerOptionsQuery::tryGetModuleInfo)
                .filter(moduleInfo -> moduleInfo != null)
                .findFirst()
                .orElse(null);
    }

    private void updateModuleInfos(NbJavaModule module) {
        pathRegs.unregisterAll();

        int sourceLevel = GradleSourceLevelQueryImplementation.getNumJavaVersion(sourceLevelRef.getValue());
        registerChangeListener(sourceLevelRef);

        Map<String, List<String>> argumentsBySourceSet = sourceLevel >= 9
                ? getJigsawCompilerArgsAndUpdateListeners(module)
                : Collections.emptyMap();

        if (!argumentsBySourceSet.equals(argumentsBySourceSetRef.getValue())) {
            argumentsBySourceSetRef.setValue(argumentsBySourceSet);
        }
    }

    private Map<String, List<String>> getJigsawCompilerArgsAndUpdateListeners(NbJavaModule module) {
        List<JavaSourceSet> sourceSets = module.getSources();

        Map<String, List<String>> result = CollectionsEx.newHashMap(sourceSets.size());

        Map<String, ModuleInfo> moduleInfos = CollectionsEx.newHashMap(sourceSets.size());
        sourceSets.forEach(sourceSet -> {
            sourceRoots(sourceSet).forEach(root -> {
                File moduleInfoFile = new File(root, MODULE_INFO_JAVA);
                registerChangeListener(listener -> WATCHER.watchPath(moduleInfoFile.toPath(), listener));
            });

            ModuleInfo moduleInfo = tryGetModuleInfo(sourceSet);
            if (moduleInfo != null) {
                moduleInfos.put(sourceSet.getName(), moduleInfo);
            }
        });

        Map<String, Set<String>> sourceSetDependencyGraph = module.sourceSetDependencyGraph();

        sourceSets.forEach(sourceSet -> {
            String sourceSetName = sourceSet.getName();
            ModuleInfo moduleInfo = moduleInfos.get(sourceSetName);

            List<String> arguments;
            if (moduleInfo != null) {
                arguments = Collections.emptyList();
            }
            else {
                arguments = sourceSetDependencyGraph
                        .getOrDefault(sourceSetName, Collections.emptySet())
                        .stream()
                        .map(moduleInfos::get)
                        .filter(depInfo -> depInfo != null)
                        .map(GradleCompilerOptionsQuery::getMergedSourceSetArgs)
                        .findFirst()
                        .orElse(Collections.emptyList());
            }

            result.put(sourceSetName, Collections.unmodifiableList(arguments));
        });

        return result;
    }

    private static List<String> getMergedSourceSetArgs(ModuleInfo moduleInfo) {
        String moduleName = moduleInfo.getModuleName();
        return Arrays.asList(
                "-XD-Xmodule:" + moduleName,
                "--add-reads", moduleName + "=ALL-UNNAMED");
    }

    private static ModuleInfo tryGetModuleInfo(Path moduleInfoPath) {
        String moduleName = tryGetModuleName(moduleInfoPath.toFile());
        if (moduleName == null) {
            return null;
        }

        return new ModuleInfo(moduleInfoPath, moduleName);
    }

    private static String tryGetModuleName(File moduleInfoFile) {
        ModuleUtilities moduleUtilities = tryGetModuleUtilties(moduleInfoFile);
        if (moduleUtilities == null) {
            return null;
        }

        try {
            return moduleUtilities.parseModuleName();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to parse module name from " + moduleInfoFile, ex);
            return null;
        }
    }

    private static ModuleUtilities tryGetModuleUtilties(File moduleInfoFile) {
        File normModuleInfoFile = FileUtil.normalizeFile(moduleInfoFile);

        FileObject moduleInfoObj = FileUtil.toFileObject(normModuleInfoFile);
        if (moduleInfoObj == null) {
            return null;
        }

        JavaSource src = JavaSource.forFileObject(moduleInfoObj);
        if (src == null) {
            return null;
        }

        return ModuleUtilities.get(src);
    }

    private static final class ModuleInfo {
        private final Path moduleInfoPath;
        private final String moduleName;

        public ModuleInfo(Path moduleInfoPath, String moduleName) {
            this.moduleInfoPath = Objects.requireNonNull(moduleInfoPath, "moduleInfoPath");
            this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        }

        public Path getModuleInfoPath() {
            return moduleInfoPath;
        }

        public String getModuleName() {
            return moduleName;
        }
    }

    private static final class ResultImpl extends Result {
        private final String sourceSetName;
        private final SwingPropertySource<Map<String, List<String>>, ChangeListener> argumentsBySourceSetRef;

        public ResultImpl(
                String sourceSetName,
                SwingPropertySource<Map<String, List<String>>, ChangeListener> argumentsBySourceSetRef) {

            this.sourceSetName = Objects.requireNonNull(sourceSetName, "sourceSetName");
            this.argumentsBySourceSetRef = Objects.requireNonNull(argumentsBySourceSetRef, "argumentsBySourceSetRef");
        }

        @Override
        public List<? extends String> getArguments() {
            return argumentsBySourceSetRef.getValue().getOrDefault(sourceSetName, Collections.emptyList());
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            argumentsBySourceSetRef.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            argumentsBySourceSetRef.removeChangeListener(listener);
        }
    }
}
