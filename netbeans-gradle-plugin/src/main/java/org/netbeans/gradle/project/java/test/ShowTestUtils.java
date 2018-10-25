package org.netbeans.gradle.project.java.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationToken;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;

public final class ShowTestUtils {
    private static final Logger LOGGER = Logger.getLogger(ShowTestUtils.class.getName());

    // This is mostly copied from the Maven plugin.
    public static boolean openTestMethod(
            final CancellationToken cancelToken,
            JavaExtension javaExt,
            final SpecificTestcase specificTestcase) {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(javaExt, "javaExt");
        Objects.requireNonNull(specificTestcase, "specificTestcase");

        FileObject testFile = JavaTestsuiteNode.tryGetTestFile(
                javaExt,
                StringUtils.getTopMostClassName(specificTestcase.getTestClassName()));
        if (testFile == null) {
            return false;
        }

        final FileObject[] fo2open = new FileObject[]{testFile};
        final long[] line = new long[]{-1};

        JavaSource javaSource = JavaSource.forFileObject(fo2open[0]);
        if (javaSource == null) {
            return false;
        }

        cancelToken.checkCanceled();

        try {
            String testMethodName = specificTestcase.getTestMethodName().getRawMethodName();

            javaSource.runUserActionTask((CompilationController compilationController) -> {
                compilationController.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                Trees trees = compilationController.getTrees();
                CompilationUnitTree compilationUnitTree = compilationController.getCompilationUnit();
                List<? extends Tree> typeDecls = compilationUnitTree.getTypeDecls();
                for (Tree tree: typeDecls) {
                    Element element = trees.getElement(trees.getPath(compilationUnitTree, tree));
                    if (element != null && element.getKind() == ElementKind.CLASS && element.getSimpleName().contentEquals(fo2open[0].getName())) {
                        List<? extends ExecutableElement> methodElements = ElementFilter.methodsIn(element.getEnclosedElements());
                        for (Element child: methodElements) {
                            if (cancelToken.isCanceled()) {
                                return;
                            }

                            if (child.getSimpleName().contentEquals(testMethodName)) {
                                long pos = trees.getSourcePositions().getStartPosition(compilationUnitTree, trees.getTree(child));
                                line[0] = compilationUnitTree.getLineMap().getLineNumber(pos);
                                break;
                            }
                        }
                        break;
                    }
                }
            }, true);
        } catch (IOException ioe) {
            LOGGER.log(Level.INFO, "Error while looking for test method.", ioe);
            return false;
        }

        cancelToken.checkCanceled();

        SwingUtilities.invokeLater(() -> {
            if (!cancelToken.isCanceled()) {
                OpenEditorOutputListener.tryOpenFile(fo2open[0], (int)line[0]);
            }
        });

        return true;
    }

    private ShowTestUtils() {
        throw new AssertionError();
    }
}
