package org.netbeans.gradle.project.properties2;

import org.netbeans.api.java.platform.JavaPlatform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class StandardProperties {
    static PropertyXmlDef<PlatformId> getTargetPlatformXmlDef() {
        return new PropertyXmlDef<PlatformId>() {
            @Override
            public PlatformId loadFromXml(Element node) {
                Element nameElement = tryGetChildElementByName(node, "target-platform-name");
                Element versionElement = tryGetChildElementByName(node, "target-platform");

                return new PlatformId(
                        getPlatformName(nameElement),
                        getPlatformVersion(versionElement));
            }

            @Override
            public void addToXml(Element parent, PlatformId value) {
                switch (parent.getNodeName()) {
                    case "target-platform-name":
                        parent.setTextContent(value.getName());
                        break;
                    case "target-platform":
                        parent.setTextContent(value.getVersion());
                        break;
                }
            }
        };
    }

    private static String getPlatformName(Element nameElement) {
        if (nameElement == null) {
            return PlatformId.DEFAULT_NAME;
        }

        return nameElement.getTextContent().trim();
    }

    private static String getPlatformVersion(Element versionElement) {
        if (versionElement == null) {
            return JavaPlatform.getDefault().getSpecification().getVersion().toString();
        }

        return versionElement.getTextContent().trim();
    }

    private static Element tryGetChildElementByName(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        int count = children.getLength();
        for (int i = 0; i < count; i++) {
            Node child = children.item(i);
            if (name.equals(child.getNodeName())) {
                return child instanceof Element ? (Element)child : null;
            }
        }
        return null;
    }

    private static void addTextChild(Element parent, String key, String value) {
        Document document = parent.getOwnerDocument();
        Element element = document.createElement(key);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private StandardProperties() {
        throw new AssertionError();
    }
}
