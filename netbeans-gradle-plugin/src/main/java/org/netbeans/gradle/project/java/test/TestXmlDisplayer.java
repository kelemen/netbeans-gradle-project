package org.netbeans.gradle.project.java.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.others.test.NbGradleTestManager;
import org.netbeans.gradle.project.others.test.NbGradleTestManagers;
import org.netbeans.gradle.project.others.test.NbGradleTestSession;
import org.netbeans.gradle.project.others.test.NbGradleTestSuite;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class TestXmlDisplayer {
    private static final Logger LOGGER = Logger.getLogger(TestXmlDisplayer.class.getName());
    private static final File[] NO_FILES = new File[0];
    private static final String NEW_LINE_PATTERN = Pattern.quote("\n");
    private static final String[] STACKTRACE_PREFIXES = {"at "};

    private final Project project;
    private final JavaExtension javaExt;
    private final String testName;
    private final NbGradleTestManager testManager;

    public TestXmlDisplayer(Project project, String testName) {
        this(project, testName, NbGradleTestManagers.getTestManager());
    }

    public TestXmlDisplayer(Project project, String testName, NbGradleTestManager testManager) {
        this.project = Objects.requireNonNull(project, "project");
        this.testName = Objects.requireNonNull(testName, "testName");
        this.javaExt = JavaExtension.getJavaExtensionOfProject(project);
        this.testManager = Objects.requireNonNull(testManager, "testManager");
    }

    private String getProjectName() {
        ProjectInformation projectInfo = ProjectUtils.getInformation(project);
        return projectInfo.getDisplayName();
    }

    public String getTestName() {
        return testName;
    }

    public File tryGetReportDirectory() {
        JavaTestTask testTask = javaExt.getCurrentModel().getMainModule().getTestModelByName(testName);
        return testTask.getXmlOutputDir();
    }

    private File[] getTestReportFiles() {
        File reportDir = tryGetReportDirectory();
        if (reportDir == null) {
            return NO_FILES;
        }

        File[] result = reportDir.listFiles((File dir, String name) -> {
            String normName = name.toLowerCase(Locale.ROOT);
            return normName.startsWith("test-") && normName.endsWith(".xml");
        });

        return result != null ? result : NO_FILES;
    }

    private static long tryReadTimeMillis(String timeStr, long defaultValue) {
        if (timeStr == null) {
            return defaultValue;
        }

        try {
            return Math.round(Double.parseDouble(timeStr) * 1000.0);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String[] toLines(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()
                .split(NEW_LINE_PATTERN);
    }

    private static String[] extractStackTrace(String text) {
        String[] lines = toLines(text);

        // The first line is the exception message.
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            for (String prefix: STACKTRACE_PREFIXES) {
                if (line.startsWith(prefix)) {
                    line = line.substring(prefix.length());
                    break;
                }
            }
            lines[i] = line;
        }
        return lines;
    }

    private void displayTestSuite(final File reportFile, SAXParser parser, final NbGradleTestSession testSession) throws Exception {
        parser.reset();

        TestXmlContentHandler testXmlContentHandler = new TestXmlContentHandler(testSession, reportFile);
        parser.parse(reportFile, testXmlContentHandler);

        NbGradleTestSuite testSuite = testXmlContentHandler.testSuite;
        if (testSuite != null) {
            testSuite.setStdErr(testXmlContentHandler.stderr);
            testSuite.setStdOut(testXmlContentHandler.stdout);
            testSuite.endSuite(testXmlContentHandler.suiteTime);
        }
    }

    private SAXParser tryGetSaxParser() {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        try {
            return parserFactory.newSAXParser();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unexpected parser configuration error.", ex);
            return null;
        } catch (SAXException ex) {
            LOGGER.log(Level.WARNING, "Unexpected SAXException.", ex);
            return null;
        }
    }

    private boolean displayTestSession(NbGradleTestSession testSession, File[] reportFiles) {
        SAXParser parser = tryGetSaxParser();
        if (parser == null) {
            return false;
        }

        for (File reportFile: reportFiles) {
            try {
                displayTestSuite(reportFile, parser, testSession);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "Error while parsing " + reportFile, ex);
            }
        }

        return true;
    }

    private boolean displayReport(Lookup runContext, File[] reportFiles) {
        NbGradleTestSession testSession = testManager.startSession(
                getProjectName(),
                project,
                new JavaTestRunnerNodeFactory(javaExt, new TestTaskName(testName)),
                new JavaRerunHandler(runContext));

        try {
            return displayTestSession(testSession, reportFiles);
        } finally {
            testSession.endSession();
        }
    }

    public boolean displayReport(Lookup runContext) {
        Objects.requireNonNull(runContext, "runContext");

        File[] reportFiles = getTestReportFiles();
        if (reportFiles.length == 0) {
            LOGGER.log(Level.WARNING,
                    "Could not find output for test task \"{0}\" in {1}",
                    new Object[]{testName, tryGetReportDirectory()});
            return false;
        }

        return displayReport(runContext, reportFiles);
    }



    public class JavaRerunHandler implements RerunHandler {
        private final Lookup rerunContext;

        private JavaRerunHandler(Lookup rerunContext) {
            this.rerunContext = rerunContext;
        }

        @Override
        public void rerun() {
            String commandStr = GradleActionProvider.getCommandStr(rerunContext, ActionProvider.COMMAND_TEST);
            GradleActionProvider.invokeAction(project, commandStr, rerunContext);
        }

        private List<SpecificTestcase> getSpecificTestcases(Set<Testcase> tests) {
            return tests.stream()
                    .map(TestMethodName::tryConvertToSpecificTestcase)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        @Override
        public void rerun(Set<Testcase> tests) {
            if (tests.isEmpty()) {
                LOGGER.warning("Rerun test requested with an empty test set.");
                return;
            }

            List<Object> contextObjs = new ArrayList<>();
            contextObjs.add(new TestTaskName(testName));
            contextObjs.addAll(getSpecificTestcases(tests));
            Lookup context = Lookups.fixed(contextObjs.toArray());
            GradleActionProvider.invokeAction(project, SingleMethod.COMMAND_RUN_SINGLE_METHOD, context);
        }

        @Override
        public boolean enabled(RerunType type) {
            return true;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
        }
    }

    private static final class TestXmlContentHandler extends DefaultHandler {
        private final NbGradleTestSession session;
        private final File reportFile;

        private int level;
        private NbGradleTestSuite testSuite;
        private final List<Testcase> allTestcases;

        private String stdout;
        private String stderr;
        private long suiteTime;
        private boolean error;
        private Testcase testcase;
        private StringBuilder failureContent;
        private boolean outputBuilderIsStdOut;
        private StringBuilder outputBuilder;

        public TestXmlContentHandler(NbGradleTestSession session, File reportFile) {
            this.session = session;
            this.reportFile = reportFile;
            this.allTestcases = new ArrayList<>(64);

            this.level = 0;
            this.testSuite = null;
            this.suiteTime = 0;
            this.error = false;
            this.testcase = null;
            this.failureContent = null;
            this.outputBuilderIsStdOut = false;
        }

        private void startSuite(Attributes attributes) {
            String name = attributes.getValue("", "name");
            suiteTime = tryReadTimeMillis(attributes.getValue("", "time"), 0);

            String suiteName = name != null ? name : reportFile.getName();
            testSuite = session.startTestSuite(suiteName);
        }

        private Testcase tryGetTestCase(Attributes attributes, Status status) {
            Testcase result = tryGetTestCase(attributes);
            if (result != null) {
                result.setStatus(status);
            }
            return result;
        }

        private Testcase tryGetTestCase(Attributes attributes) {
            if (testSuite == null) {
                LOGGER.warning("test suite has not been started but there is a test case to add.");
                return null;
            }

            String name = attributes.getValue("", "name");
            if (name == null) {
                return null;
            }

            Testcase result = testSuite.addTestcase(name);

            String className = attributes.getValue("", "classname");
            if (className != null) {
                result.setClassName(className);
            }

            long time = tryReadTimeMillis(attributes.getValue("", "time"), 0);
            result.setTimeMillis(time);

            return result;
        }

        private boolean tryAddTestCase(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "testcase":
                    testcase = tryGetTestCase(attributes, Status.PASSED);
                    break;
                case "ignored-testcase":
                    testcase = tryGetTestCase(attributes, Status.SKIPPED);
                    break;
            }

            if (testcase != null) {
                allTestcases.add(testcase);
                return true;
            }
            else {
                return false;
            }
        }

        private void tryUpdateTestCase(String uri, String localName, String qName, Attributes attributes) {
            if (testcase != null) {
                switch (qName) {
                    case "failure":
                        error = false;
                        testcase.setStatus(Status.FAILED);
                        break;
                    case "error":
                        error = true;
                        testcase.setStatus(Status.ERROR);
                        break;
                    case "skipped":
                        error = false;
                        testcase.setStatus(Status.SKIPPED);
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unexpected element in testcase: {0}", qName);
                        error = true;
                        testcase.setStatus(Status.ERROR);
                        break;
                }
                failureContent = new StringBuilder(1024);
            }
        }

        private void tryStartOutput(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "system-out":
                    outputBuilder = new StringBuilder();
                    outputBuilderIsStdOut = true;
                    break;
                case "system-err":
                    outputBuilder = new StringBuilder();
                    outputBuilderIsStdOut = false;
                    break;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (level) {
                case 0:
                    startSuite(attributes);
                    break;
                case 1:
                    if (!tryAddTestCase(uri, localName, qName, attributes)) {
                        tryStartOutput(uri, localName, qName, attributes);
                    }
                    break;
                case 2:
                    tryUpdateTestCase(uri, localName, qName, attributes);
                    break;
            }

            level++;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            level--;

            switch (level) {
                case 1:
                    testcase = null;
                    if (outputBuilder != null) {
                        if (outputBuilderIsStdOut) {
                            stdout = outputBuilder.toString();
                        }
                        else {
                            stderr = outputBuilder.toString();
                        }
                        outputBuilder = null;
                    }
                    break;
                case 2:
                    if (failureContent != null && testcase != null) {
                        Trouble trouble = new Trouble(error);
                        trouble.setStackTrace(extractStackTrace(failureContent.toString()));
                        testcase.setTrouble(trouble);
                    }
                    failureContent = null;
                    break;
            }
        }

        private static void tryAppend(char[] ch, int start, int length, StringBuilder... results) {
            for (StringBuilder result: results) {
                if (result != null) {
                    result.append(ch, start, length);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            tryAppend(ch, start, length, failureContent, outputBuilder);
        }
    }
}
