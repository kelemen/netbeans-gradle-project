package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
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

        final DefaultLicenseDef licenseDef = new DefaultLicenseDef(src, "TestLicense1", "TestDisplayName1");
        final DefaultLicenseStore store = new DefaultLicenseStore();

        addAndRemove(store, licenseDef, () -> {
            assertTrue(store.containsLicense(licenseDef.getLicenseId()));
        });

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

    @Test
    public void testGetLicenses() throws IOException {
        Path src = TMP_FILE.getFile();

        final DefaultLicenseDef licenseDef = new DefaultLicenseDef(src, "TestLicense1", "TestDisplayName1");
        final DefaultLicenseStore store = new DefaultLicenseStore();

        addAndRemove(store, licenseDef, () -> {
            Collection<LicenseRef> licenses = store.getAllLicense();
            LicenseRef found = findById(licenses, licenseDef.getLicenseId());
            assertNotNull("License", found);

            assertEquals("Dynamic", true, found.isDynamic());
            assertEquals("DisplayName", licenseDef.getDisplayName(), found.getDisplayName());
            assertEquals("Id", licenseDef.getLicenseId(), found.getId());
        });

        assertFalse(store.containsLicense(licenseDef.getLicenseId()));
    }

    private static void addAndRemove(
            LicenseStore<DefaultLicenseDef> store,
            DefaultLicenseDef licenseDef,
            Runnable taskWhileAdded) throws IOException {
        store.addLicense(licenseDef);
        try {
            taskWhileAdded.run();
        } finally {
            store.removeLicense(licenseDef.getLicenseId());
        }
    }

    private static LicenseRef findById(Collection<? extends LicenseRef> refs, String licenseId) {
        for (LicenseRef ref: refs) {
            if (Objects.equals(ref.getId(), licenseId)) {
                return ref;
            }
        }
        return null;
    }
}
