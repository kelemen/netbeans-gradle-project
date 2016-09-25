package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class DefaultLicenseStore implements LicenseStore<DefaultLicenseDef> {
    private static final Logger LOGGER = Logger.getLogger(DefaultLicenseStore.class.getName());

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
    }

    @Override
    public void removeLicense(DefaultLicenseDef licenseDef) throws IOException {
        FileObject licenseRoot = tryGetLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        String baseFileName = toLicenseFileName(licenseDef.getLicenseId());

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

    private static String toLicenseFileName(String licenseName) {
        // This naming patter in required by NetBeans
        return "license-" + licenseName + ".txt";
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
