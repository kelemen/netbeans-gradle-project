package org.netbeans.gradle.project.properties.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.properties.standard.PlatformId;
import org.openide.filesystems.FileObject;

public final class PlatformOrder {
    private static final int DEFAULT_ORDER_INDEX = Integer.MAX_VALUE;
    public static final PlatformOrder DEFAULT_ORDER
            = new PlatformOrder(Collections.<String>emptyList(), false);

    private final Map<String, Integer> pathToOrder;
    private final List<String> platformIds;

    public PlatformOrder(List<JavaPlatform> platformPaths) {
        pathToOrder = CollectionUtils.newHashMap(platformPaths.size());
        platformIds = new ArrayList<>(platformPaths.size());

        int index = 0;
        for (JavaPlatform platform: platformPaths) {
            String platformId = getPlatformId(platform);
            pathToOrder.put(platformId, index);
            platformIds.add(platformId);
            index++;
        }
    }

    private PlatformOrder(List<String> platformIds, boolean copyList) {
        this.platformIds = copyList
                ? CollectionUtils.copyNullSafeList(platformIds)
                : platformIds;

        int index = 0;
        pathToOrder = CollectionUtils.newHashMap(platformIds.size());
        for (String platformId: this.platformIds) {
            pathToOrder.put(platformId, index);
            index++;
        }
    }

    public static PlatformOrder fromPlatformIds(List<String> platformIds) {
        return new PlatformOrder(platformIds, true);
    }

    public List<String> getPlatformIds() {
        return platformIds;
    }

    public List<JavaPlatform> orderPlatforms(Collection<? extends JavaPlatform> src) {
        List<PlatformAndOrder> srcWithOrder = new ArrayList<>(src.size());
        for (JavaPlatform platform: src) {
            Integer order = pathToOrder.get(getPlatformId(platform));

            PlatformAndOrder platformWithOrder;
            if (order == null) {
                platformWithOrder = new PlatformAndOrder(platform, DEFAULT_ORDER_INDEX);
            }
            else {
                platformWithOrder = new PlatformAndOrder(platform, order);
            }
            srcWithOrder.add(platformWithOrder);
        }

        Collections.sort(srcWithOrder);

        List<JavaPlatform> result = new ArrayList<>(srcWithOrder.size());
        for (PlatformAndOrder platform: srcWithOrder) {
            result.add(platform.platform);
        }
        return result;
    }

    public List<JavaPlatform> filterIndistinguishable(JavaPlatform[] platforms) {
        return filterIndistinguishable(Arrays.asList(platforms));
    }

    public List<JavaPlatform> filterIndistinguishable(Collection<JavaPlatform> platforms) {
        List<JavaPlatform> result = new ArrayList<>(platforms.size());
        Set<PlatformId> foundVersions = CollectionUtils.newHashSet(platforms.size());

        for (JavaPlatform platform: orderPlatforms(platforms)) {
            PlatformId platformId = PlatformId.tryGetIdOfPlatform(platform);
            if (platformId != null && foundVersions.add(platformId)) {
                result.add(platform);
            }
        }

        return result;
    }

    private static String getPlatformId(JavaPlatform platform) {
        StringBuilder result = new StringBuilder(1024);
        for (FileObject installFolder: platform.getInstallFolders()) {
            String path = installFolder.getPath();
            if (result.length() > 0) {
                result.append(";");
            }
            result.append(path);
        }
        return result.toString();
    }

    private static final class PlatformAndOrder implements Comparable<PlatformAndOrder> {
        public final JavaPlatform platform;
        public final int order;

        public PlatformAndOrder(JavaPlatform platform, int order) {
            this.platform = platform;
            this.order = order;
        }

        @Override
        public int compareTo(PlatformAndOrder o) {
            return Integer.compare(order, o.order);
        }
    }
}
