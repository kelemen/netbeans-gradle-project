package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.lookups.DynamicLookup;
import org.netbeans.gradle.project.lookups.ProjectLookupHack;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class NbGradleProjectLookups {
    private final NbGradleProject project;
    private final Lookup defaultLookup;
    private final DynamicLookup mainLookup;
    private final DynamicLookup combinedExtensionLookup;

    public NbGradleProjectLookups(NbGradleProject project, Lookup defaultLookup) {
        this.project = Objects.requireNonNull(project, "project");
        this.defaultLookup = Objects.requireNonNull(defaultLookup, "defaultLookup");
        this.mainLookup = new DynamicLookup(defaultLookup);
        this.combinedExtensionLookup = new DynamicLookup();
    }

    public void updateExtensions(Collection<? extends NbGradleExtensionRef> extensionRefs) {
        List<LookupProvider> allLookupProviders = new ArrayList<>(extensionRefs.size() + 3);

        allLookupProviders.add(moveToLookupProvider(defaultLookup));
        for (NbGradleExtensionRef extension: extensionRefs) {
            allLookupProviders.add(moveToLookupProvider(extension.getProjectLookup()));
        }

        allLookupProviders.add(moveToLookupProvider(getLookupMergers()));

        allLookupProviders.addAll(moveToLookupProvider(getLookupsFromAnnotations(defaultLookup)));

        Lookup combinedLookupProviders = LookupProviderSupport.createCompositeLookup(Lookup.EMPTY, Lookups.fixed(allLookupProviders.toArray()));
        final Lookup combinedAllLookups = new ProxyLookup(combinedLookupProviders);
        mainLookup.replaceLookups(new ProjectLookupHack(new ProjectLookupHack.LookupContainer() {
            @Override
            public NbGradleProject getProject() {
                return project;
            }

            @Override
            public Lookup getLookup() {
                return combinedAllLookups;
            }

            @Override
            public Lookup getLookupAndActivate() {
                project.ensureLoadRequested();
                return combinedAllLookups;
            }
        }));
        updateExtensionLookups(extensionRefs);
    }

    private void updateExtensionLookups(Collection<? extends NbGradleExtensionRef> extensions) {
        List<Lookup> extensionLookups = new ArrayList<>(extensions.size());
        for (NbGradleExtensionRef extenion: extensions) {
            extensionLookups.add(extenion.getExtensionLookup());
        }
        combinedExtensionLookup.replaceLookups(extensionLookups);
    }

    public Lookup getMainLookup() {
        return mainLookup.getUnmodifiableView();
    }

    public Lookup getCombinedExtensionLookup() {
        return combinedExtensionLookup.getUnmodifiableView();
    }

    private static List<LookupProvider> moveToLookupProvider(List<Lookup> lookups) {
        List<LookupProvider> result = new ArrayList<>(lookups.size());
        for (Lookup lookup: lookups) {
            result.add(moveToLookupProvider(lookup));
        }
        return result;
    }

    private static LookupProvider moveToLookupProvider(final Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (baseContext) -> lookup;
    }

    private static List<Lookup> extractLookupsFromProviders(
            Lookup baseContext,
            Lookup providerContainer) {
        // baseContext must contain the Project instance.

        List<Lookup> result = new ArrayList<>();
        for (LookupProvider provider: providerContainer.lookupAll(LookupProvider.class)) {
            result.add(provider.createAdditionalLookup(baseContext));
        }

        return result;
    }

    private static List<Lookup> getLookupsFromAnnotations(Lookup baseContext) {
        Lookup lookupProviders = Lookups.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Lookup");
        return extractLookupsFromProviders(baseContext, lookupProviders);
    }

    private static Lookup getLookupMergers() {
        return Lookups.fixed(
                UILookupMergerSupport.createPrivilegedTemplatesMerger(),
                UILookupMergerSupport.createProjectProblemsProviderMerger(),
                UILookupMergerSupport.createRecommendedTemplatesMerger()
        );
    }
}
