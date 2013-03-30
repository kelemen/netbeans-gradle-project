package org.netbeans.gradle.project.query;

import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;

public final class GradleProjectTemplates implements PrivilegedTemplates, RecommendedTemplates {
    private static final String[] JAR_APPLICATION_TYPES = {
        "java-classes",
        "java-main-class",
        "java-forms",
        "gui-java-application",
        "java-beans",
        "oasis-XML-catalogs",
        "XML",
        "web-service-clients",
        "REST-clients",
        "wsdl",
        "junit",
        "simple-files"
    };
    private static final String[] PRIVILEGED_TEMPLATES = {
        "Templates/Classes/Class.java",
        "Templates/Classes/Package",
        "Templates/Classes/Interface.java",
        "Templates/GUIForms/JPanel.java",
        "Templates/GUIForms/JFrame.java",
        "Templates/WebServices/WebServiceClient"
    };

    public GradleProjectTemplates() {
    }

    @Override
    public String[] getRecommendedTypes() {
        return JAR_APPLICATION_TYPES.clone();
    }

    @Override
    public String[] getPrivilegedTemplates() {
        return PRIVILEGED_TEMPLATES.clone();
    }
}
