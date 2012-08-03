package org.netbeans.gradle.project;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.gradle.api.GradleException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.idea.IdeaModule;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class GradleProjectLogicalViewProvider implements LogicalViewProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        GradleProjectNode result = new GradleProjectNode(projectFolder.getNodeDelegate());
        result.scanProject();
        return result;
    }

    private Children createChildren() {
        return Children.create(new GradleProjectChildFactory(project), true);
    }

    private Lookup createLookup(Node rootNode) {
        return new ProxyLookup(
                project.getLookup(),
                rootNode.getLookup());
    }

    private final class GradleProjectNode extends FilterNode {
        private final TasksActionMenu tasksAction;
        private final Action[] actions;

        public GradleProjectNode(Node node) {
            super(node, createChildren(), createLookup(node));

            this.tasksAction = new TasksActionMenu(project);
            this.actions = new Action[] {
                //ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_RUN, "Run", null),
                CommonProjectActions.newFileAction(),
                this.tasksAction,
                CommonProjectActions.closeProjectAction(),
            };
        }

        public void scanProject() {
            tasksAction.scanForTasks();
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        @Override
        public Image getIcon(int type) {
            return NbIcons.getGradleIcon();
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getDisplayName();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        //leave unimplemented for now
        return null;
    }

    private static File getJavaHome() {
        Collection<FileObject> installFolders = JavaPlatform.getDefault().getInstallFolders();
        if (installFolders.size() != 1) {
            return null;
        }
        else {
            return FileUtil.toFile(installFolders.iterator().next());
        }
    }

    private static void doGradleTasks(NbGradleProject project, String... taskNames) {
        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbBundle.getMessage(GradleClassPathProvider.class, "LBL_ExecutingGradleTasks"));
        try {
            progress.start();
            doGradleTasksWithProgress(progress, project, taskNames);
        } finally {
            progress.finish();
        }
    }

    private static void doGradleTasksWithProgress(
            final ProgressHandle progress,
            NbGradleProject project,
            String... taskNames) {
        if (taskNames.length < 1) {
            throw new IllegalArgumentException("At least one task is required.");
        }
        String printableName = taskNames.length == 1
                ? taskNames[0]
                : Arrays.toString(taskNames);

        StringBuilder commandBuilder = new StringBuilder(128);
        commandBuilder.append("gradle");
        for (String task: taskNames) {
            commandBuilder.append(' ');
            commandBuilder.append(task);
        }

        String command = commandBuilder.toString();

        LOGGER.log(Level.INFO, "Executing: {0}", command);

        FileObject projectDir = project.getProjectDirectory();

        GradleConnector gradleConnector = GradleConnector.newConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();
            BuildLauncher buildLauncher = projectConnection.newBuild();
            buildLauncher.setJavaHome(getJavaHome());

            buildLauncher.addProgressListener(new ProgressListener() {
                @Override
                public void statusChanged(ProgressEvent pe) {
                    progress.progress(pe.getDescription());
                }
            });

           buildLauncher.forTasks(taskNames);

           InputOutput io = IOProvider.getDefault().getIO("Gradle: " + printableName, false);
           OutputWriter buildOutput = io.getOut();
           try {
               buildOutput.println(NbBundle.getMessage(
                       GradleProjectLogicalViewProvider.class, "MSG_ExecutingTask", command));
               buildOutput.println();

               OutputWriter buildErrOutput = io.getErr();
               try {
                   buildLauncher.setStandardOutput(new WriterOutputStream(buildOutput));
                   buildLauncher.setStandardError(new WriterOutputStream(buildErrOutput));

                   io.select();
                   buildLauncher.run();
               } finally {
                   buildErrOutput.close();
               }
           } finally {
               buildOutput.close();
           }
        } catch (GradleException ex) {
            // Gradle should have printed this one to stderr.
            LOGGER.log(Level.WARNING, "Gradle build failure: " + command, ex);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }
    }

    private static void submitGradleTask(final NbGradleProject project, String... taskNames) {
        final String[] taskNamesCopy = taskNames.clone();
        NbGradleProject.TASK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                doGradleTasks(project, taskNamesCopy);
            }
        });
    }

    @SuppressWarnings("serial") // don't care about serialization
    private static class TasksActionMenu extends AbstractAction implements Presenter.Popup {
        private final NbGradleProject project;
        private final JMenu tasksMenu;

        public TasksActionMenu(NbGradleProject project) {
            this.project = project;
            this.tasksMenu = new JMenu("Tasks");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Do nothing it's just a menu.
        }


        public void scanForTasks() {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    NbProjectModel projectModel = project.loadProject();
                    IdeaModule module = NbProjectModelUtils.getMainIdeaModule(projectModel);
                    Set<String> tasks = new HashSet<String>();
                    for (GradleTask task: module.getGradleProject().getTasks()) {
                        tasks.add(task.getName());
                    }

                    final List<String> taskList = new ArrayList<String>(tasks);
                    Collections.sort(taskList);

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tasksMenu.removeAll();
                            for (final String task: taskList) {
                                tasksMenu.add(task).addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        submitGradleTask(project, task);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }

        @Override
        public JMenuItem getPopupPresenter() {
            return tasksMenu;
        }
    }

    private static class WriterOutputStream extends OutputStream {
        private final Writer writer;
        private final Charset encoding;

        public WriterOutputStream(Writer writer, Charset encoding) {
            this.writer = writer;
            this.encoding = encoding != null
                    ? encoding
                    : Charset.defaultCharset();
        }

        public WriterOutputStream(Writer writer) {
            this(writer, null);
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            writer.write(new String(b, encoding));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            writer.write(new String(b, off, len, encoding));
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte)b});
        }
    }
}
