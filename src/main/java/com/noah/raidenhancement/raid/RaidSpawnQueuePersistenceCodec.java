package com.noah.raidenhancement.raid;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Dependency-free, bounded Properties codec for {@link RaidSpawnWorkQueue} snapshots. */
public final class RaidSpawnQueuePersistenceCodec {
    private static final int MAX_BATCHES = 64;
    private static final int MAX_ANCHORS_PER_BATCH = 32;
    private static final int MAX_SLOTS_PER_BATCH = 512;
    private static final int MAX_PENDING_SLOTS = 512;
    private static final int MAX_RESERVED_IDS = 256;

    private RaidSpawnQueuePersistenceCodec() {
    }

    public static void write(Properties properties, String prefix,
                             RaidSpawnWorkQueue.QueueSnapshot snapshot) {
        if (properties == null || prefix == null || snapshot == null) {
            return;
        }
        properties.setProperty(prefix + "format", Integer.toString(snapshot.formatVersion()));
        List<RaidSpawnWorkQueue.BatchSnapshot> batches = snapshot.activeBatches();
        properties.setProperty(prefix + "batchCount", Integer.toString(batches.size()));
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            RaidSpawnWorkQueue.BatchSnapshot batch = batches.get(batchIndex);
            RaidSpawnWorkQueue.BatchPlan plan = batch.plan();
            String batchPrefix = prefix + "batch." + batchIndex + ".";
            properties.setProperty(batchPrefix + "id", encode(plan.batchId()));
            properties.setProperty(batchPrefix + "logicalWave", Integer.toString(plan.logicalWave()));
            properties.setProperty(batchPrefix + "spawned", Integer.toString(batch.spawned()));
            properties.setProperty(batchPrefix + "exhausted", Integer.toString(batch.exhausted()));
            properties.setProperty(batchPrefix + "anchorCount", Integer.toString(plan.anchors().size()));
            for (int anchorIndex = 0; anchorIndex < plan.anchors().size(); anchorIndex++) {
                RaidSpawnWorkQueue.SpawnAnchor anchor = plan.anchors().get(anchorIndex);
                String anchorPrefix = batchPrefix + "anchor." + anchorIndex + ".";
                properties.setProperty(anchorPrefix + "x", Integer.toString(anchor.x()));
                properties.setProperty(anchorPrefix + "y", Integer.toString(anchor.y()));
                properties.setProperty(anchorPrefix + "z", Integer.toString(anchor.z()));
            }
            properties.setProperty(batchPrefix + "slotCount", Integer.toString(plan.slots().size()));
            for (int slotIndex = 0; slotIndex < plan.slots().size(); slotIndex++) {
                RaidSpawnWorkQueue.SpawnSlot slot = plan.slots().get(slotIndex);
                String slotPrefix = batchPrefix + "slot." + slotIndex + ".";
                properties.setProperty(slotPrefix + "index", Integer.toString(slot.slotIndex()));
                properties.setProperty(slotPrefix + "entity", encode(slot.entityId()));
                properties.setProperty(slotPrefix + "category", slot.category().name());
                properties.setProperty(slotPrefix + "preferredAnchor", Integer.toString(slot.preferredAnchorIndex()));
                properties.setProperty(slotPrefix + "localIndex", Integer.toString(slot.localIndex()));
            }
        }

        List<RaidSpawnWorkQueue.PendingSlotSnapshot> pending = snapshot.pendingSlots();
        properties.setProperty(prefix + "pendingCount", Integer.toString(pending.size()));
        for (int index = 0; index < pending.size(); index++) {
            RaidSpawnWorkQueue.PendingSlotSnapshot slot = pending.get(index);
            String pendingPrefix = prefix + "pending." + index + ".";
            properties.setProperty(pendingPrefix + "batch", encode(slot.batchId()));
            properties.setProperty(pendingPrefix + "slotIndex", Integer.toString(slot.slotIndex()));
            properties.setProperty(pendingPrefix + "attempts", Integer.toString(slot.attempts()));
        }

        List<String> reserved = new ArrayList<>(snapshot.reservedBatchIds());
        properties.setProperty(prefix + "reservedCount", Integer.toString(reserved.size()));
        for (int index = 0; index < reserved.size(); index++) {
            properties.setProperty(prefix + "reserved." + index, encode(reserved.get(index)));
        }
    }

    public static RaidSpawnWorkQueue.QueueSnapshot read(Properties properties, String prefix) {
        if (properties == null || prefix == null
                || intProperty(properties, prefix + "format", -1) != RaidSpawnWorkQueue.SNAPSHOT_FORMAT) {
            return RaidSpawnWorkQueue.QueueSnapshot.empty();
        }
        int batchCount = boundedCount(properties, prefix + "batchCount", MAX_BATCHES);
        List<RaidSpawnWorkQueue.BatchSnapshot> batches = new ArrayList<>();
        int rejectedBatches = 0;
        for (int batchIndex = 0; batchIndex < batchCount; batchIndex++) {
            String batchPrefix = prefix + "batch." + batchIndex + ".";
            String batchId = decode(properties.getProperty(batchPrefix + "id", ""));
            int logicalWave = intProperty(properties, batchPrefix + "logicalWave", -1);
            int anchorCount = boundedCount(properties, batchPrefix + "anchorCount", MAX_ANCHORS_PER_BATCH);
            int slotCount = boundedCount(properties, batchPrefix + "slotCount", MAX_SLOTS_PER_BATCH);
            if (batchId == null || batchId.isBlank() || logicalWave <= 0 || anchorCount <= 0 || slotCount <= 0) {
                rejectedBatches++;
                continue;
            }
            List<RaidSpawnWorkQueue.SpawnAnchor> anchors = new ArrayList<>();
            for (int anchorIndex = 0; anchorIndex < anchorCount; anchorIndex++) {
                String anchorPrefix = batchPrefix + "anchor." + anchorIndex + ".";
                anchors.add(new RaidSpawnWorkQueue.SpawnAnchor(
                        intProperty(properties, anchorPrefix + "x", 0),
                        intProperty(properties, anchorPrefix + "y", 64),
                        intProperty(properties, anchorPrefix + "z", 0)));
            }
            List<RaidSpawnWorkQueue.SpawnSlot> slots = new ArrayList<>();
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                String slotPrefix = batchPrefix + "slot." + slotIndex + ".";
                String entityId = decode(properties.getProperty(slotPrefix + "entity", ""));
                RaiderCategory category = category(properties.getProperty(slotPrefix + "category", ""));
                if (entityId == null || entityId.isBlank() || category == null) {
                    slots.clear();
                    break;
                }
                try {
                    slots.add(new RaidSpawnWorkQueue.SpawnSlot(
                            intProperty(properties, slotPrefix + "index", -1), entityId, category,
                            intProperty(properties, slotPrefix + "preferredAnchor", 0),
                            intProperty(properties, slotPrefix + "localIndex", 0)));
                } catch (IllegalArgumentException exception) {
                    slots.clear();
                    break;
                }
            }
            if (slots.size() != slotCount) {
                rejectedBatches++;
                continue;
            }
            try {
                RaidSpawnWorkQueue.BatchPlan plan = new RaidSpawnWorkQueue.BatchPlan(
                        batchId, logicalWave, anchors, slots);
                batches.add(new RaidSpawnWorkQueue.BatchSnapshot(plan,
                        intProperty(properties, batchPrefix + "spawned", 0),
                        intProperty(properties, batchPrefix + "exhausted", 0)));
            } catch (IllegalArgumentException ignored) {
                // Reject one corrupt batch without losing other raid lifecycle metadata.
                rejectedBatches++;
            }
        }

        int pendingCount = boundedCount(properties, prefix + "pendingCount", MAX_PENDING_SLOTS);
        List<RaidSpawnWorkQueue.PendingSlotSnapshot> pending = new ArrayList<>();
        for (int index = 0; index < pendingCount; index++) {
            String pendingPrefix = prefix + "pending." + index + ".";
            String batchId = decode(properties.getProperty(pendingPrefix + "batch", ""));
            if (batchId != null && !batchId.isBlank()) {
                pending.add(new RaidSpawnWorkQueue.PendingSlotSnapshot(batchId,
                        intProperty(properties, pendingPrefix + "slotIndex", -1),
                        intProperty(properties, pendingPrefix + "attempts", -1)));
            }
        }

        int reservedCount = boundedCount(properties, prefix + "reservedCount", MAX_RESERVED_IDS);
        Set<String> reserved = new LinkedHashSet<>();
        for (int index = 0; index < reservedCount; index++) {
            String batchId = decode(properties.getProperty(prefix + "reserved." + index, ""));
            if (batchId != null && !batchId.isBlank()) {
                reserved.add(batchId);
            }
        }
        return new RaidSpawnWorkQueue.QueueSnapshot(RaidSpawnWorkQueue.SNAPSHOT_FORMAT,
                batches, reserved, pending, rejectedBatches);
    }

    private static int boundedCount(Properties properties, String key, int max) {
        return Math.max(0, Math.min(max, intProperty(properties, key, 0)));
    }

    private static int intProperty(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static RaiderCategory category(String value) {
        try {
            return RaiderCategory.valueOf(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
