package org.netbeans.gradle.project.properties2;

import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigXmlUtils {
    private static final char ESCAPE_START_CHAR = '_';
    private static final char ESCAPE_END_CHAR = '.';
    private static final String EMPTY_ESCAPE = ESCAPE_START_CHAR + "" + ESCAPE_END_CHAR;

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

    private static boolean isValidFirstElementChar(char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return true;
        }
        if (ch >= 'a' && ch <= 'z') {
            return true;
        }
        return ch == '_';
    }

    private static boolean isValidElementChar(char ch) {
        if (isValidElementChar(ch)) {
            return true;
        }
        if (ch >= '0' && ch <= '9') {
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

    private static void addAttributes(Element element, ConfigTree.Builder result) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return;
        }

        int attributeCount = attributes.getLength();
        for (int i = 0; i < attributeCount; i++) {
            Node attribute = attributes.item(i);

            String attrName = fromElementName(attribute.getNodeName());
            String attrValue = attribute.getNodeValue();
            result.getSubBuilder(attrName).setValue(attrValue);
        }
    }

    private static int addChildren(Element element, ConfigTree.Builder result) {
        NodeList childNodes = element.getChildNodes();

        int addedChildren = 0;
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element elementChild = (Element)child;

                ConfigKey elementKey = new ConfigKey(
                        fromElementName(elementChild.getNodeName()),
                        elementChild.getNamespaceURI());

                ConfigTree.Builder subBuilder = result.getSubBuilder(elementKey);
                String nodeValue = parseNode(elementChild, subBuilder);
                if (nodeValue != null) {
                    subBuilder.setValue(nodeValue);
                }

                addedChildren++;
            }
        }

        return addedChildren;
    }

    private static String parseNode(Element root, ConfigTree.Builder result) {
        ExceptionHelper.checkNotNullArgument(root, "root");
        ExceptionHelper.checkNotNullArgument(result, "result");

        addAttributes(root, result);

        int addedChildCount = addChildren(root, result);
        if (addedChildCount == 0) {
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

    private ConfigXmlUtils() {
        throw new AssertionError();
    }
}
