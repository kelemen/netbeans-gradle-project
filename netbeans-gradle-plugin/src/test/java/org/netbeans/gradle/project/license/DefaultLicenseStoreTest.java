package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.gradle.project.util.TemporaryFileRule;

import static org.junit.Assert.*;

public class DefaultLicenseStoreTest {
    @ClassRule
    public static final TemporaryFileRule TMP_FILE = new TemporaryFileRule("TestLicenseContent");

    @Test
    public void testAddAndRemove() throws IOException {
        Path src = TMP_FILE.getFile();

        DefaultLicenseDef licenseDef = new DefaultLicenseDef(src, "TestLicense1", "TestDisplayName1");
        DefaultLicenseStore store = new DefaultLicenseStore();

        store.addLicense(licenseDef);
        try {
            assertTrue(store.containsLicense(licenseDef.getLicenseId()));
        } finally {
            store.removeLicense(licenseDef.getLicenseId());
        }

        assertFalse(store.containsLicense(licenseDef.getLicenseId()));
    }

    @Test
    public void testRemoveNonExistantDoesNotFail() throws IOException {
        Path src = TMP_FILE.getFile();

        DefaultLicenseDef licenseDef = new DefaultLicenseDef(src, "TestLicense2", "TestDisplayName2");

        DefaultLicenseStore store = new DefaultLicenseStore();

        assertFalse(store.containsLicense(licenseDef.getLicenseId()));
        store.removeLicense(licenseDef.getLicenseId());
        assertFalse(store.containsLicense(licenseDef.getLicenseId()));
    }
}
