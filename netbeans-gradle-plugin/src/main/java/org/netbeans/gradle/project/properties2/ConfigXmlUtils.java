package org.netbeans.gradle.project.properties2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigXmlUtils {
    private static final Logger LOGGER = Logger.getLogger(ConfigXmlUtils.class.getName());

    private static final char ESCAPE_START_CHAR = '_';
    private static final char ESCAPE_END_CHAR = '.';
    private static final String EMPTY_ESCAPE = ESCAPE_START_CHAR + "" + ESCAPE_END_CHAR;

    private static final String KEYWORD_PREFIX = "__";
    private static final String KEYWORD_VALUE = KEYWORD_PREFIX + "value";
    private static final String KEYWORD_HAS_VALUE = KEYWORD_PREFIX + "has-value";

    private static final String STR_NO = "no";

    private static final String ATTR_PREFIX = "#attr-";

    private static String asAttributeName(String keyName) {
        return ATTR_PREFIX + keyName;
    }

    private static boolean containsChar(String str, char ch) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                return true;
            }
        }
        return false;
    }

    private static int addEscaped(String str, int offset, StringBuilder result) {
        int length = str.length();
        if (length <= offset) {
            return 0;
        }

        if (str.charAt(offset) == ESCAPE_START_CHAR) {
            result.append(ESCAPE_START_CHAR);
            return 1;
        }

        int charValue = 0;
        int skippedCount = 0;
        boolean hasNumber = false;
        for (int i = offset; i < length; i++) {
            char ch = str.charAt(i);
            skippedCount++;

            if (ch >= '0' && ch <= '9') {
                hasNumber = true;
                charValue = 10 * charValue + (ch - '0');
            }
            else {
                break;
            }
        }

        if (hasNumber) {
            result.append((char)charValue);
        }
        return skippedCount;
    }

    private static String fromElementName(String elementName) {
        if (!containsChar(elementName, ESCAPE_START_CHAR)) {
            return elementName;
        }

        int nameLength = elementName.length();
        StringBuilder result = new StringBuilder(nameLength);

        int index = 0;
        while (index < nameLength) {
            char ch = elementName.charAt(index);
            if (ch == ESCAPE_START_CHAR) {
                index++;
                index += addEscaped(elementName, index, result);
            }
            else {
                result.append(ch);
                index++;
            }
        }

        return result.toString();
    }

    private static boolean isLowerCaseLetter(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    private static boolean isUpperCaseLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    private static boolean isLetter(char ch) {
        return isLowerCaseLetter(ch) || isUpperCaseLetter(ch);
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isValidFirstElementChar(char ch) {
        return isLetter(ch);
    }

    private static boolean isValidElementChar(char ch) {
        if (isValidFirstElementChar(ch)) {
            return true;
        }
        if (isDigit(ch)) {
            return true;
        }

        return ch == '.' || ch == '-';
    }

    private static boolean startsWithXmlReserved(String name) {
        String reserved = "xml";
        if (name.length() < reserved.length()) {
            return false;
        }

        for (int i = 0; i < reserved.length(); i++) {
            char ch = Character.toLowerCase(name.charAt(i));
            if (ch != reserved.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidTagNameStart(String name) {
        if (name.isEmpty()) {
            return false;
        }

        if (startsWithXmlReserved(name)) {
            return false;
        }

        return isValidFirstElementChar(name.charAt(0));
    }


    private static String toElementName(String rawName) {
        int nameLength = rawName.length();

        if (nameLength == 0) {
            return EMPTY_ESCAPE;
        }

        StringBuilder result = new StringBuilder(nameLength);

        if (!isValidTagNameStart(rawName)) {
            result.append(EMPTY_ESCAPE);
        }

        for (int i = 0; i < nameLength; i++) {
            char ch = rawName.charAt(i);

            if (ch == ESCAPE_START_CHAR) {
                result.append(ESCAPE_START_CHAR);
                result.append(ESCAPE_START_CHAR);
            }
            else if (isValidElementChar(ch)) {
                result.append(ch);
            }
            else {
                result.append(ESCAPE_START_CHAR);
                result.append((int)ch);
                result.append(ESCAPE_END_CHAR);
            }
        }

        return result.toString();
    }

    private static boolean addAttributes(Element element, ConfigTree.Builder result) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return false;
        }

        boolean setValue = false;

        int attributeCount = attributes.getLength();
        for (int i = 0; i < attributeCount; i++) {
            Node attribute = attributes.item(i);

            String xmlAttrName = attribute.getNodeName();
            String attrValue = attribute.getNodeValue();

            if (xmlAttrName.startsWith(KEYWORD_PREFIX)) {
                switch (xmlAttrName) {
                    case KEYWORD_VALUE:
                        result.setValue(attrValue);
                        setValue = true;
                        break;
                    case KEYWORD_HAS_VALUE:
                        if (STR_NO.equals(attrValue)) {
                            result.setValue(null);
                            setValue = true;
                        }
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unknown keyword in properties file: {0}", xmlAttrName);
                        break;
                }
            }
            else {
                String attrName = fromElementName(xmlAttrName);
                result.addChildBuilder(asAttributeName(attrName)).setValue(attrValue);
            }
        }
        return setValue;
    }

    private static int addChildren(Element element, ConfigTree.Builder result) {
        NodeList childNodes = element.getChildNodes();

        int addedChildren = 0;
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element elementChild = (Element)child;

                String elementKey = fromElementName(elementChild.getNodeName());

                ConfigTree.Builder childBuilder = result.addChildBuilder(elementKey);
                String nodeValue = parseNode(elementChild, childBuilder);
                if (nodeValue != null) {
                    childBuilder.setValue(nodeValue);
                }

                addedChildren++;
            }
        }

        return addedChildren;
    }

    private static String parseNode(Element root, ConfigTree.Builder result) {
        ExceptionHelper.checkNotNullArgument(root, "root");
        ExceptionHelper.checkNotNullArgument(result, "result");

        boolean setValue = addAttributes(root, result);

        int addedChildCount = addChildren(root, result);
        if (!setValue && addedChildCount == 0) {
            return root.getTextContent();
        }
        else {
            return null;
        }
    }

    public static ConfigTree.Builder parseDocument(Document document) {
        ConfigTree.Builder result = new ConfigTree.Builder();

        Element root = document.getDocumentElement();
        if (root != null) {
            parseNode(root, result);
        }

        return result;
    }

    private static List<KeyValuePair> tryGetAttributeList(ConfigTree tree) {
        List<KeyValuePair> attributes = null;
        for (Map.Entry<String, List<ConfigTree>> entry: tree.getChildTrees().entrySet()) {
            List<ConfigTree> childList = entry.getValue();
            if (childList.size() != 1) {
                continue;
            }

            ConfigTree child = childList.get(0);
            if (!child.getChildTrees().isEmpty()) {
                continue;
            }

            String key = entry.getKey();
            if (!key.startsWith(ATTR_PREFIX)) {
                continue;
            }

            if (attributes == null) {
                attributes = new ArrayList<>(tree.getChildTrees().size());
            }

            // A childless ConfigTree should always have a value, this is
            // something guaranteed by ConfigTree.
            String value = child.getValue("");
            attributes.add(new KeyValuePair(key, value));
        }
        return attributes;
    }

    private static Set<String> addAttributeChildrenToXml(
            Element parent,
            ConfigTree tree,
            final ConfigNodeSorter nodeSorter) {

        List<KeyValuePair> attributes = tryGetAttributeList(tree);
        if (attributes == null) {
            return Collections.emptySet();
        }

        // Despite that attributes are unordered, add them in a deterministic
        // order, so that any sensible implementation will save them in the
        // same order every time (avoiding unnecessary differences in the
        // properties file).
        Collections.sort(attributes, new Comparator<KeyValuePair>() {
            @Override
            public int compare(KeyValuePair o1, KeyValuePair o2) {
                return nodeSorter.compare(o1.key, o1.key);
            }
        });

        Set<String> result = CollectionsEx.newHashSet(attributes.size());
        for (KeyValuePair keyValue: attributes) {
            String key = keyValue.key;
            result.add(key);

            assert key.startsWith(ATTR_PREFIX);
            String xmlKey = toElementName(key.substring(ATTR_PREFIX.length()));
            parent.setAttribute(xmlKey, keyValue.value);
        }
        return result;
    }

    private static void addTreeToXml(
            Document document,
            Element parent,
            ConfigTree tree,
            final ConfigNodeSorter nodeSorter) {

        Map<String, List<ConfigTree>> children = tree.getChildTrees();

        String value = tree.getValue(null);
        if (value != null) {
            if (children.isEmpty()) {
                parent.setTextContent(value);
                return;
            }
        }

        Set<String> attributeKeys = addAttributeChildrenToXml(parent, tree, nodeSorter);

        List<NamedNode> childEntries = new ArrayList<>(children.size());
        for (Map.Entry<String, List<ConfigTree>> entry: children.entrySet()) {
            String key = entry.getKey();
            if (!attributeKeys.contains(key)) {
                childEntries.add(new NamedNode(key, entry.getValue()));
            }
        }

        if (childEntries.isEmpty()) {
            if (value != null) {
                parent.setTextContent(value);
            }
            else {
                parent.setAttribute(KEYWORD_HAS_VALUE, STR_NO);
            }
            return;
        }

        if (value != null) {
            parent.setAttribute(KEYWORD_VALUE, value);
        }

        Collections.sort(childEntries, new Comparator<NamedNode>() {
            @Override
            public int compare(NamedNode o1, NamedNode o2) {
                return nodeSorter.compare(o1.name, o2.name);
            }
        });

        for (NamedNode child: childEntries) {
            String xmlKey = toElementName(child.name);
            Element childElement = document.createElement(xmlKey);
            parent.appendChild(childElement);

            ConfigNodeSorter childSorter = nodeSorter.getChildSorter(child.name);
            for (ConfigTree childTree: child.trees) {
                addTreeToXml(document, childElement, childTree, childSorter);
            }
        }
    }

    public static void addTree(Element parent, ConfigTree tree, ConfigNodeSorter nodeSorter) {
        ExceptionHelper.checkNotNullArgument(parent, "parent");
        ExceptionHelper.checkNotNullArgument(tree, "tree");
        ExceptionHelper.checkNotNullArgument(nodeSorter, "nodeSorter");

        Document document = parent.getOwnerDocument();
        Objects.requireNonNull(document, "parent.getOwnerDocument()");
        addTreeToXml(document, parent, tree, nodeSorter);
    }

    private static final class KeyValuePair {
        public final String key;
        public final String value;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class NamedNode {
        public final String name;
        public final List<ConfigTree> trees;

        public NamedNode(String name, List<ConfigTree> trees) {
            this.name = name;
            this.trees = trees;
        }
    }

    private ConfigXmlUtils() {
        throw new AssertionError();
    }
}
