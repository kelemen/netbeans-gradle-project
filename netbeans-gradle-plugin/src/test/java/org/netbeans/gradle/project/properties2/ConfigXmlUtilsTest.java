package org.netbeans.gradle.project.properties2;

import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

public class ConfigXmlUtilsTest {
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private static Document readFromResources(String relPath) throws Exception {
        try (InputStream input = TestResourceUtils.openResource(relPath)) {
            return newDocumentBuilder().parse(input);
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

    private String saveXmlToString(Document document) throws Exception {
        StringWriter output = new StringWriter(8 * 1024);
        Result result = new StreamResult(output);

        Source source = new DOMSource(document);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(source, result);
        return output.toString();
    }

    @Test
    public void testSaveAndParseForSettings1() throws Exception {
        ConfigTree settings1 = getExpectedSettings1Content();

        Document document = newDocumentBuilder().newDocument();
        Element root = document.createElement("root");
        document.appendChild(root);
        ConfigXmlUtils.addTree(root, settings1, NaturalConfigNodeSorter.INSTANCE);

        ConfigTree parsedTree = ConfigXmlUtils.parseDocument(document).create();

        try {
            assertEquals(settings1, parsedTree);
        } catch (Throwable ex) {
            System.err.println("Built XML: ");
            System.err.println(saveXmlToString(document));

            throw ex;
        }
    }
}
