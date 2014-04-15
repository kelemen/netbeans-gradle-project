package org.gradle.plugins.nbm;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.gradle.api.tasks.SourceSet;

public final class NbmFriendPackages {
    private final List<PackageNameGenerator> packageList;

    public NbmFriendPackages() {
        this.packageList = new LinkedList<>();
    }

    private static void getPackagesInDir(String packageName, File currentDir, List<String> result) {
        boolean hasFile = false;
        for (File file: currentDir.listFiles()) {
            if (file.isDirectory()) {
                String lastPart = file.getName();
                String subPackageName = packageName.isEmpty()
                        ? lastPart
                        : packageName + "." + lastPart;
                getPackagesInDir(subPackageName, file, result);
            }
            else if (!hasFile && file.isFile()) {
                hasFile = true;
            }
        }

        if (hasFile) {
            result.add(packageName);
        }
    }

    private static void findAllPackages(File sourceRoot, String packageName, List<String> result) {
        String[] pathParts = packageName.split(Pattern.quote("."));
        File startDir = sourceRoot;
        for (String part: pathParts) {
            startDir = new File(startDir, part);
        }

        if (!startDir.isDirectory()) {
            return;
        }

        getPackagesInDir(packageName, startDir, result);
    }

    private static void findAllPackages(SourceSet sourceSet, String packageName, List<String> result) {
        for (File sourceRoot: sourceSet.getAllJava().getSrcDirs()) {
            findAllPackages(sourceRoot, packageName, result);
        }
    }

    public void addWithSubPackages(final SourceSet sourceSet, final String packageName) {
        Objects.requireNonNull(sourceSet, "sourceSet");
        Objects.requireNonNull(packageName, "packageName");

        packageList.add(new PackageNameGenerator() {
            @Override
            public void findPackages(List<String> result) {
                findAllPackages(sourceSet, packageName, result);
            }
        });
    }

    public void add(final String packageName) {
        Objects.requireNonNull(packageName, "packageName");

        packageList.add(new PackageNameGenerator() {
            @Override
            public void findPackages(List<String> result) {
                result.add(packageName);
            }
        });
    }

    public List<String> getPackageList() {
        List<String> result = new LinkedList<>();
        for (PackageNameGenerator currentNames: packageList) {
            currentNames.findPackages(result);
        }
        return result;
    }

    private interface PackageNameGenerator {
        public void findPackages(List<String> result);
    }
}
