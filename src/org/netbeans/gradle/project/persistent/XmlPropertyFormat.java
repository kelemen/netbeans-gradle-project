package org.netbeans.gradle.project.persistent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.ConstPropertySource;
import org.netbeans.gradle.project.properties.DefaultPropertySources;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.PropertiesSnapshot;
import org.netbeans.gradle.project.properties.PropertySource;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class XmlPropertyFormat {
    private static final Logger LOGGER = Logger.getLogger(XmlPropertyFormat.class.getName());

    private static final String ROOT_NODE = "gradle-project-properties";
    private static final String SOURCE_ENCODING_NODE = "source-encoding";
    private static final String PLATFORM_NAME_NODE = "target-platform-name";
    private static final String PLATFORM_NODE = "target-platform";
    private static final String SOURCE_LEVEL_NODE = "source-level";
    private static final String COMMON_TASKS_NODE = "common-tasks";
    private static final String TASK_DISPLAY_NAME_NODE = "display-name";
    private static final String TASK_NON_BLOCKING_NODE = "non-blocking";
    private static final String TASK_NODE = "task";
    private static final String TASK_NAME_LIST_NODE = "task-names";
    private static final String TASK_NAME_NODE = "name";
    private static final String TASK_MUST_EXIST_ATTR = "must-exist";
    private static final String TASK_ARGS_NODE = "task-args";
    private static final String TASK_JVM_ARGS_NODE = "task-jvm-args";
    private static final String ARG_NODE = "arg";

    private static final String VALUE_YES = "yes";
    private static final String VALUE_NO = "no";

    private static final String DEFAULT_SPECIFICATION_NAME = "j2se";

    private static Element addChild(Node parent, String tagName) {
        Element element = parent.getOwnerDocument().createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    private static Element addSimpleChild(Node parent, String tagName, String value) {
        Element element = addChild(parent, tagName);
        element.setTextContent(value);
        return element;
    }

    private static void saveDocument(File propertyfile, Document document) throws TransformerException, IOException {
        File dir = propertyfile.getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }

        Source source = new DOMSource(document);
        Result result = new StreamResult(propertyfile);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(source, result);
    }

    private static void addSingleTask(Node node, PredefinedTask task) {
        Element taskNode = addChild(node, TASK_NODE);

        addSimpleChild(taskNode, TASK_DISPLAY_NAME_NODE, task.getDisplayName());
        addSimpleChild(taskNode, TASK_NON_BLOCKING_NODE, task.isNonBlocking() ? VALUE_YES : VALUE_NO);

        Element nameListNode = addChild(taskNode, TASK_NAME_LIST_NODE);
        for (PredefinedTask.Name name: task.getTaskNames()) {
            Element nameNode = addSimpleChild(nameListNode, TASK_NAME_NODE, name.getName());
            nameNode.setAttribute(TASK_MUST_EXIST_ATTR, name.isMustExist() ? VALUE_YES : VALUE_NO);
        }

        Element argListNode = addChild(taskNode, TASK_ARGS_NODE);
        for (String arg: task.getArguments()) {
            addSimpleChild(argListNode, ARG_NODE, arg);
        }

        Element jvmArgListNode = addChild(taskNode, TASK_JVM_ARGS_NODE);
        for (String arg: task.getJvmArguments()) {
            addSimpleChild(jvmArgListNode, ARG_NODE, arg);
        }
    }

    private static void addCommonTasks(Node node, List<PredefinedTask> tasks) {
        Element commonTasksNode = addChild(node, COMMON_TASKS_NODE);

        for (PredefinedTask task: tasks) {
            addSingleTask(commonTasksNode, task);
        }
    }

    public static void saveToXml(File propertyfile, PropertiesSnapshot snapshot) {
        if (propertyfile == null) throw new NullPointerException("propertyfile");
        if (snapshot == null) throw new NullPointerException("snapshot");

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create XML builder.", ex);
            return;
        }

        Document document = builder.newDocument();
        Element root = document.createElement(ROOT_NODE);
        document.appendChild(root);

        Comment comment = document.createComment(
                "DO NOT EDIT THIS FILE!"
                + " - Used by the Gradle plugin of NetBeans.");
        root.appendChild(comment);

        if (!snapshot.getSourceEncoding().isDefault()) {
            String sourceEncoding = snapshot.getSourceEncoding().getValue().name();
            addSimpleChild(root, SOURCE_ENCODING_NODE, sourceEncoding);
        }

        if (!snapshot.getPlatform().isDefault()) {
            JavaPlatform platform = snapshot.getPlatform().getValue();
            addSimpleChild(root, PLATFORM_NAME_NODE, platform.getSpecification().getName());
            addSimpleChild(root, PLATFORM_NODE, platform.getSpecification().getVersion().toString());
        }

        if (!snapshot.getSourceLevel().isDefault()) {
            String sourceLevel = snapshot.getSourceLevel().getValue();
            addSimpleChild(root, SOURCE_LEVEL_NODE, sourceLevel);
        }

        if (!snapshot.getCommonTasks().isDefault()) {
            List<PredefinedTask> commonTasks = snapshot.getCommonTasks().getValue();
            if (!commonTasks.isEmpty()) {
                addCommonTasks(root, commonTasks);
            }
        }

        try {
            saveDocument(propertyfile, document);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
        } catch (TransformerException ex) {
            LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
        }
    }

    private static Element getFirstChildByTagName(Element element, String tagName) {
        NodeList children = element.getElementsByTagName(tagName);
        return children.getLength() > 0 ? (Element)children.item(0) : null;
    }

    private static String tryGetValueOfNode(Element node, String tagName) {
        Node child = getFirstChildByTagName(node, tagName);
        String textContent = child != null ? child.getTextContent() : null;
        return textContent != null ? textContent.trim() : null;
    }


    private static Charset parseCharset(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalCharsetNameException ex) {
            LOGGER.log(Level.INFO, "The name of the character set is invalid: " + name, ex);
        } catch (UnsupportedCharsetException ex) {
            LOGGER.log(Level.INFO, "The character set is not supported: " + name, ex);
        }

        return null;
    }

    private static Iterable<Element> getChildElements(Element element, String tagName) {
        return new NodeListAsElementIterable(element.getElementsByTagName(tagName));
    }

    private static PredefinedTask readTask(Element root) {
        Element displayNameNode = getFirstChildByTagName(root, TASK_DISPLAY_NAME_NODE);
        String displayName = null;
        if (displayNameNode != null) {
            displayName = displayNameNode.getTextContent();
        }
        displayName = displayName != null ? displayName.trim() : "?";

        Element nonBlockingNode = getFirstChildByTagName(root, TASK_NON_BLOCKING_NODE);
        boolean nonBlocking = false;
        if (nonBlockingNode != null) {
            String nonBlockingStr = nonBlockingNode.getTextContent();
            if (nonBlockingStr != null) {
                nonBlocking = VALUE_YES.equalsIgnoreCase(nonBlockingStr.trim());
            }
        }

        List<PredefinedTask.Name> names = new LinkedList<PredefinedTask.Name>();
        Element nameListNode = getFirstChildByTagName(root, TASK_NAME_LIST_NODE);
        if (nameListNode != null) {
            for (Element nameNode: getChildElements(nameListNode, TASK_NAME_NODE)) {
                String name = nameNode.getTextContent();
                name = name != null ? name.trim() : "";

                if (!name.isEmpty()) {
                    boolean mustExist = VALUE_YES.equalsIgnoreCase(nameNode.getAttribute(TASK_MUST_EXIST_ATTR));
                    names.add(new PredefinedTask.Name(name, mustExist));
                }
            }
        }

        List<String> args = new LinkedList<String>();
        Element argsNode = getFirstChildByTagName(root, TASK_ARGS_NODE);
        if (argsNode != null) {
            for (Element argNode: getChildElements(argsNode, ARG_NODE)) {
                String arg = argNode.getTextContent();
                arg = arg != null ? arg.trim() : "";
                if (!arg.isEmpty()) {
                    args.add(arg);
                }
            }
        }

        List<String> jvmArgs = new LinkedList<String>();
        Element jvmArgsNode = getFirstChildByTagName(root, TASK_JVM_ARGS_NODE);
        if (jvmArgsNode != null) {
            for (Element jvmArgNode: getChildElements(jvmArgsNode, ARG_NODE)) {
                String arg = jvmArgNode.getTextContent();
                arg = arg != null ? arg.trim() : "";
                if (!arg.isEmpty()) {
                    jvmArgs.add(arg);
                }
            }
        }

        return new PredefinedTask(displayName, names, args, jvmArgs, nonBlocking);
    }

    private static List<PredefinedTask> readTasks(Element root) {
        Element commonTasksNode = getFirstChildByTagName(root, COMMON_TASKS_NODE);
        if (commonTasksNode == null) {
            return Collections.emptyList();
        }

        List<PredefinedTask> result = new LinkedList<PredefinedTask>();
        for (Element taskNode: getChildElements(commonTasksNode, TASK_NODE)) {
            result.add(readTask(taskNode));
        }
        return result;
    }

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    public static PropertiesSnapshot readFromXml(File propertiesFile) {
        PropertiesSnapshot.Builder result = new PropertiesSnapshot.Builder();

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create XML builder.", ex);
            return result.create();
        }

        Document document;
        try {
            if (!propertiesFile.exists()) {
                return result.create();
            }

            document = builder.parse(propertiesFile);
        } catch (SAXException ex) {
            LOGGER.log(Level.INFO, "Failed to parse the property file.", ex);
            return result.create();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to read the property file.", ex);
            return result.create();
        }

        Element root = document.getDocumentElement();

        String sourceLevel = tryGetValueOfNode(root, SOURCE_LEVEL_NODE);
        if (sourceLevel != null) {
            result.setSourceLevel(asConst(sourceLevel, false));
        }

        String sourceEncodingStr = tryGetValueOfNode(root, SOURCE_ENCODING_NODE);
        Charset sourceEncoding = sourceEncodingStr != null
                ? parseCharset(sourceEncodingStr)
                : null;
        if (sourceEncoding != null) {
            result.setSourceEncoding(asConst(sourceEncoding, false));
        }

        String platformName = tryGetValueOfNode(root, PLATFORM_NAME_NODE);
        if (platformName == null) {
            platformName = DEFAULT_SPECIFICATION_NAME;
        }

        String platformStr = tryGetValueOfNode(root, PLATFORM_NODE);
        if (platformStr != null) {
            result.setPlatform(DefaultPropertySources.findPlatformSource(platformName, platformStr, false));
        }

        List<PredefinedTask> commonTasks = Collections.unmodifiableList(readTasks(root));
        result.setCommonTasks(asConst(commonTasks, commonTasks.isEmpty()));

        return result.create();
    }

    private static class NodeListAsElementIterable implements Iterable<Element> {
        private final NodeListAsIterable wrapped;

        public NodeListAsElementIterable(NodeList nodeList) {
            this.wrapped = new NodeListAsIterable(nodeList);
        }

        @Override
        public Iterator<Element> iterator() {
            final Iterator<Node> nodeItr = wrapped.iterator();

            return new Iterator<Element>() {
                @Override
                public boolean hasNext() {
                    return nodeItr.hasNext();
                }

                @Override
                @SuppressWarnings("unchecked")
                public Element next() {
                    return (Element)nodeItr.next();
                }

                @Override
                public void remove() {
                    nodeItr.remove();
                }
            };
        }
    }

    private static class NodeListAsIterable implements Iterable<Node> {
        private final NodeList nodeList;

        public NodeListAsIterable(NodeList nodeList) {
            if (nodeList == null) throw new NullPointerException("nodeList");
            this.nodeList = nodeList;
        }

        @Override
        public Iterator<Node> iterator() {
            final int numberOfNodes = nodeList.getLength();
            return new Iterator<Node>() {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < numberOfNodes;
                }

                @Override
                public Node next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    Node result = nodeList.item(currentIndex);
                    currentIndex++;
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("This Iterable is read-only.");
                }
            };
        }
    }

    private XmlPropertyFormat() {
        throw new AssertionError();
    }
}
