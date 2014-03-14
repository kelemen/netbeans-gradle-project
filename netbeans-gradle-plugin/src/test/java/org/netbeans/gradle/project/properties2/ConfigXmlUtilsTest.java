package org.netbeans.gradle.project.properties2;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

public class ConfigXmlUtilsTest {
    private static Document readFromResources(String relPath) throws Exception {
        try (InputStream input = TestResourceUtils.openResource(relPath)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        }
    }

    private void addTaskNames(ConfigTree.Builder parent, boolean mustExist, String... names) {
        ConfigTree.Builder taskNamesNode = parent.addChildBuilder("task-names");
        for (String name: names) {
            addTaskNameNode(taskNamesNode, name, mustExist);
        }
    }

    private void addTaskNameNode(ConfigTree.Builder parent, String taskName, boolean mustExist) {
        ConfigTree.Builder nameNode = parent.addChildBuilder("name");
        nameNode.setValue(taskName);
        nameNode.addChildBuilder("#attr-must-exist").setValue(mustExist ? "yes" : "no");
    }

    private ConfigTree getExpectedSettings1Content() {
        ConfigTree.Builder result = new ConfigTree.Builder();
        result.addChildBuilder("source-encoding").setValue("UTF-8");
        result.addChildBuilder("target-platform-name").setValue("j2se");
        result.addChildBuilder("target-platform").setValue("1.7");
        result.addChildBuilder("source-level").setValue("1.7");

        ConfigTree.Builder commonTasks = result.addChildBuilder("common-tasks");
        ConfigTree.Builder customTask1 = commonTasks.addChildBuilder("task");
        customTask1.addChildBuilder("display-name").setValue("List tasks");
        customTask1.addChildBuilder("non-blocking").setValue("yes");
        customTask1.addChildBuilder("task-args").setValue("");
        customTask1.addChildBuilder("task-jvm-args").setValue("");
        addTaskNames(customTask1, false, "tasks");

        ConfigTree.Builder scriptPlatform = result.addChildBuilder("script-platform");
        scriptPlatform.addChildBuilder("spec-name").setValue("j2se");
        scriptPlatform.addChildBuilder("spec-version").setValue("1.7");

        result.addChildBuilder("gradle-home").setValue("?VER=1.11");

        ConfigTree.Builder licenseHeader = result.addChildBuilder("license-header");
        licenseHeader.addChildBuilder("name").setValue("my-license");
        licenseHeader.addChildBuilder("template").setValue("license2.txt");

        ConfigTree.Builder licenseProperty = licenseHeader.addChildBuilder("property");
        licenseProperty.addChildBuilder("#attr-name").setValue("organization");
        licenseProperty.setValue("MyCompany");

        ConfigTree.Builder builtInTasks = result.addChildBuilder("built-in-tasks");

        ConfigTree.Builder builtInTask1 = builtInTasks.addChildBuilder("task");
        builtInTask1.addChildBuilder("display-name").setValue("build");
        builtInTask1.addChildBuilder("non-blocking").setValue("yes");
        addTaskNames(builtInTask1, false, "build");
        builtInTask1.addChildBuilder("task-args").setValue("");
        builtInTask1.addChildBuilder("task-jvm-args").setValue("");

        ConfigTree.Builder builtInTask2 = builtInTasks.addChildBuilder("task");
        builtInTask2.addChildBuilder("display-name").setValue("test");
        builtInTask2.addChildBuilder("non-blocking").setValue("yes");
        addTaskNames(builtInTask2, false, "cleanTest", "test");
        builtInTask2.addChildBuilder("task-args").setValue("");
        builtInTask2.addChildBuilder("task-jvm-args").setValue("");

        ConfigTree.Builder auxiliary = result.addChildBuilder("auxiliary");
        auxiliary.addChildBuilder("com-junichi11-netbeans-changelf.enable").setValue("true");
        auxiliary.addChildBuilder("com-junichi11-netbeans-changelf.lf-kind").setValue("LF");
        auxiliary.addChildBuilder("com-junichi11-netbeans-changelf.use-global").setValue("true");
        auxiliary.addChildBuilder("com-junichi11-netbeans-changelf.use-project").setValue("false");

        return result.create();
    }

    @Test
    public void testSettings1() throws Exception {
        ConfigTree parsedTree = ConfigXmlUtils.parseDocument(readFromResources("settings1.xml")).create();
        assertEquals(getExpectedSettings1Content(), parsedTree);
    }

}
