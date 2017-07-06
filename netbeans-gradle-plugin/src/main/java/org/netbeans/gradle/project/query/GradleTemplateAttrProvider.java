package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.netbeans.api.templates.CreateDescriptor;
import org.netbeans.api.templates.CreateFromTemplateAttributes;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.license.LicenseHeaderInfo;
import org.netbeans.gradle.project.license.LicenseManager;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.standard.SourceEncodingProperty;

public final class GradleTemplateAttrProvider implements CreateFromTemplateAttributes {
    private final NbGradleProject project;
    private final LicenseManager<? super NbGradleModel> licenseManager;

    public GradleTemplateAttrProvider(
            NbGradleProject project,
            LicenseManager<? super NbGradleModel> licenseManager) {

        this.project = Objects.requireNonNull(project, "project");
        this.licenseManager = Objects.requireNonNull(licenseManager, "licenseManager");
    }

    @Override
    public Map<String, ?> attributesFor(CreateDescriptor desc) {
        Map<String, Object> values = new TreeMap<>();

        LicenseHeaderInfo licenseHeader = project.getCommonProperties().licenseHeaderInfo().getActiveValue();
        if (licenseHeader != null) {
            NbGradleModel currentModel = project.currentModel().getValue();
            String licenseName = licenseManager.tryGetRegisteredLicenseName(currentModel, licenseHeader);
            if (licenseName != null) {
                values.put("license", licenseName);
                for (Map.Entry<String, String> property: licenseHeader.getProperties().entrySet()) {
                    values.put(property.getKey(), property.getValue());
                }
            }
        }

        Charset encoding = project.getEncodingQuery().getEncoding(desc.getTarget());
        if (encoding == null) {
            encoding = SourceEncodingProperty.DEFAULT_SOURCE_ENCODING;
        }

        values.put("encoding", encoding.name());
        values.put("name", project.getName());
        values.put("displayName", project.getDisplayName());

        if (values.size() > 0) {
            return Collections.singletonMap("project", values);
        }
        else {
            return null;
        }
    }
}
