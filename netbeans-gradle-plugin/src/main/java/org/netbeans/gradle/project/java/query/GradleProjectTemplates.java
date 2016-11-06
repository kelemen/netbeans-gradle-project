package org.netbeans.gradle.project.java.query;

import org.netbeans.gradle.project.filesupport.GradleTemplateConsts;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;

public final class GradleProjectTemplates implements PrivilegedTemplates, RecommendedTemplates {
    private static final String[] JAR_APPLICATION_TYPES = {
        "java-classes",
        "java-main-class",
        "java-forms",
        "java-beans",
        "j2ee-types",
        "javafx",
        "gui-java-application",
        "java-beans",
        "oasis-XML-catalogs",
        "XML",
        "ant-script",
        "ant-task",
        "web-service-clients",
        "REST-clients",
        "wsdl",
        "servlet-types",
        "web-types",
        "junit",
        "simple-files",
        "ear-types",
        "persistence",
        GradleTemplateConsts.CATEGORY_NAME
    };
    private static final String[] PRIVILEGED_TEMPLATES = {
        "Templates/Classes/Class.java",
        "Templates/Classes/Package",
        "Templates/Classes/Interface.java",
        "Templates/GUIForms/JPanel.java",
        "Templates/GUIForms/JFrame.java",
        "Templates/Persistence/Entity.java", 
        "Templates/Persistence/RelatedCMP",   
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
