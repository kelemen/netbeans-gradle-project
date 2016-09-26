package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class DefaultLicenseStore implements LicenseStore<DefaultLicenseDef>, LicenseSource {
    private static final Logger LOGGER = Logger.getLogger(DefaultLicenseStore.class.getName());

    private static final String LICENSE_FILE_PREFIX = "license-";
    private static final String LICENSE_FILE_SUFFIX = ".txt";

    @Override
    public void addLicense(DefaultLicenseDef licenseDef) throws IOException {
        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        String baseFileName = toLicenseFileName(licenseDef.getLicenseId());

        if (licenseRoot.getFileObject(baseFileName) != null) {
            LOGGER.log(Level.INFO, "License file already exists: {0}", baseFileName);
            return;
        }

        Path licenseTemplateFile = licenseDef.getSrc();
        licenseTemplateFile = licenseTemplateFile.normalize();

        if (licenseTemplateFile.getNameCount() == 0) {
            return;
        }

        FileObject licenseTemplateSrc = FileUtil.toFileObject(licenseTemplateFile.toFile());
        if (licenseTemplateSrc == null) {
            LOGGER.log(Level.WARNING, "Missing license template file: {0}", licenseTemplateFile);
            return;
        }

        FileObject templateFile = licenseTemplateSrc.copy(licenseRoot, baseFileName, "");
        templateFile.setAttribute("template", true);
        templateFile.setAttribute("displayName", licenseDef.getDisplayName());
        templateFile.setAttribute("dynamicLicense", true);
    }

    @Override
    public Collection<LicenseRef> getAllLicense() {
        FileObject licenseRoot = tryGetLicenseRoot();
        if (licenseRoot == null) {
            return Collections.emptySet();
        }

        List<LicenseRef> result = new ArrayList<>();
        Enumeration<? extends FileObject> children = licenseRoot.getChildren(false);
        while (children.hasMoreElements()) {
            FileObject child = children.nextElement();
            LicenseRef licenseRef = tryGetLicenseRef(child);
            if (licenseRef != null) {
                result.add(licenseRef);
            }
        }
        return result;
    }

    private LicenseRef tryGetLicenseRef(FileObject file) {
        if (file.isFolder()) {
            return null;
        }

        String licenseName = tryExtractLicenseName(file.getNameExt());
        if (licenseName == null) {
            return null;
        }

        String displayName = getStringAttr(file, "displayName", licenseName);
        boolean dynamic = getBoolAttr(file, "dynamicLicense", false);
        return new LicenseRef(licenseName, displayName, dynamic);
    }

    private static boolean getBoolAttr(FileObject file, String attrName, boolean defaultValue) {
        Object resultObj = file.getAttribute(attrName);
        if (resultObj instanceof Boolean) {
            return (Boolean)resultObj;
        }

        String resultStr = resultObj != null ? resultObj.toString() : null;
        if (resultStr == null) {
            return defaultValue;
        }

        if (resultStr.equalsIgnoreCase("true")) {
            return true;
        }
        if (resultStr.equalsIgnoreCase("false")) {
            return false;
        }
        return defaultValue;
    }

    private static String getStringAttr(FileObject file, String attrName, String defaultValue) {
        Object resultObj = file.getAttribute(attrName);
        String result = resultObj != null ? resultObj.toString() : null;
        return result != null ? result : defaultValue;
    }

    @Override
    public void removeLicense(String licenseId) throws IOException {
        FileObject licenseRoot = tryGetLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        String baseFileName = toLicenseFileName(licenseId);

        FileObject licenseFile = licenseRoot.getFileObject(baseFileName);
        if (licenseFile == null) {
            LOGGER.log(Level.INFO, "License file does not exist: {0}", baseFileName);
            return;
        }
        licenseFile.delete();
    }

    @Override
    public boolean containsLicense(String licenseId) {
        FileObject licenseRoot = tryGetLicenseRoot();
        if (licenseRoot == null) {
            return false;
        }

        String fileName = toLicenseFileName(licenseId);
        return licenseRoot.getFileObject(fileName) != null;
    }

    private static String tryExtractLicenseName(String fileName) {
        String normName = fileName.toLowerCase(Locale.ROOT);
        if (normName.startsWith(LICENSE_FILE_PREFIX) && normName.endsWith(LICENSE_FILE_SUFFIX)) {
            return fileName.substring(LICENSE_FILE_PREFIX.length(), fileName.length() - LICENSE_FILE_SUFFIX.length());
        }
        else {
            return null;
        }
    }

    private static String toLicenseFileName(String licenseName) {
        // This naming patter in required by NetBeans
        return LICENSE_FILE_PREFIX + licenseName + LICENSE_FILE_SUFFIX;
    }

    private static FileObject tryGetLicenseRoot() {
        FileObject configRoot = FileUtil.getConfigRoot();
        return configRoot != null
                ? configRoot.getFileObject("Templates/Licenses")
                : null;
    }

    private static FileObject getLicenseRoot() throws IOException {
        FileObject configRoot = FileUtil.getConfigRoot();
        if (configRoot == null) {
            return null;
        }

        FileObject templatesDir = FileUtil.createFolder(configRoot, "Templates");
        return FileUtil.createFolder(templatesDir, "Licenses");
    }
}
