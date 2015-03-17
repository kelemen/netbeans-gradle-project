package org.netbeans.gradle.project.properties.standard;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.ConfigTree;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyValueDef;

public final class LicenseHeaderInfoProperty {
    private static final String CONFIG_KEY_LICENSE_HEADER = "license-header";
    private static final String CONFIG_KEY_NAME = "name";
    private static final String CONFIG_KEY_FILE = "template";
    private static final String CONFIG_KEY_PROPERTY = "property";
    private static final String CONFIG_KEY_PROPERTY_NAME = "#attr-name";
    private static final String SAVE_FILE_NAME_SEPARATOR = "/";

    public static final PropertyDef<ConfigTree, LicenseHeaderInfo> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<ConfigTree, LicenseHeaderInfo> createPropertyDef() {
        PropertyDef.Builder<ConfigTree, LicenseHeaderInfo> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_LICENSE_HEADER));
        result.setKeyEncodingDef(CommonProperties.getIdentityTreeKeyEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    private static PropertyValueDef<ConfigTree, LicenseHeaderInfo> getValueDef() {
        return new PropertyValueDef<ConfigTree, LicenseHeaderInfo>() {
            @Override
            public PropertySource<LicenseHeaderInfo> property(ConfigTree valueKey) {
                return PropertyFactory.constSource(readLicenseHeader(valueKey));
            }

            @Override
            public ConfigTree getKeyFromValue(LicenseHeaderInfo value) {
                return value != null ? writeLicenseHeader(value) : null;
            }
        };
    }

    private static File tryReadFilePath(String normalizedPath) {
        if (normalizedPath == null) {
            return null;
        }

        return new File(normalizedPath.trim().replace(SAVE_FILE_NAME_SEPARATOR, File.separator));
    }

    private static String normalizeFilePath(File file) {
        return file.getPath().replace(File.separator, SAVE_FILE_NAME_SEPARATOR);
    }

    private static ConfigTree writeLicenseHeader(LicenseHeaderInfo licenseHeader) {
        ConfigTree.Builder result = new ConfigTree.Builder();

        result.getChildBuilder(CONFIG_KEY_NAME).setValue(licenseHeader.getLicenseName());

        File licenseTemplateFile = licenseHeader.getLicenseTemplateFile();
        if (licenseTemplateFile != null) {
            result.getChildBuilder(CONFIG_KEY_FILE).setValue(normalizeFilePath(licenseTemplateFile));
        }

        for (Map.Entry<String, String> entry: licenseHeader.getProperties().entrySet()) {
            ConfigTree.Builder propertyNode = result.addChildBuilder(CONFIG_KEY_PROPERTY);
            propertyNode.setValue(entry.getValue());
            propertyNode.getChildBuilder(CONFIG_KEY_PROPERTY_NAME).setValue(entry.getKey());
        }

        return result.create();
    }

    private static LicenseHeaderInfo readLicenseHeader(ConfigTree licenseNode) {
        if (licenseNode == null) {
            return null;
        }

        String name = licenseNode.getChildTree(CONFIG_KEY_NAME).getValue(null);
        if (name == null) {
            return null;
        }

        File licenseTemplate = tryReadFilePath(licenseNode.getChildTree(CONFIG_KEY_FILE).getValue(null));

        Map<String, String> properties = new HashMap<>();
        List<ConfigTree> propertyNodes = licenseNode.getChildTrees(CONFIG_KEY_PROPERTY);
        for (ConfigTree propertyNode: propertyNodes) {
            String propertyName = propertyNode.getChildTree(CONFIG_KEY_PROPERTY_NAME).getValue(null);
            String properyValue = propertyNode.getValue(null);

            if (propertyName != null && properyValue != null) {
                properties.put(propertyName.trim(), properyValue.trim());
            }
        }

        return new LicenseHeaderInfo(name.trim(), properties, licenseTemplate);
    }

    private LicenseHeaderInfoProperty() {
        throw new AssertionError();
    }
}
