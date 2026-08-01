package com.noah.raidenhancement.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Dependency-free contract test executed before the NeoForge Gradle build. */
public final class RaidSpawnWorkQueueContractTest {
    private RaidSpawnWorkQueueContractTest() {
    }

    public static void main(String[] args) {
        retriesAllSixtyTwoSlotsWithoutDuplicates();
        exhaustsAndExplicitlyReleasesAPlan();
        persistsAndRestoresOnlyRemainingSlots();
        rejectsCorruptPartialBatchSnapshots();
        readsStableNativeRaidIdentity();
        sharesOneBoundedAdmissionBudgetPerLevelTick();
        System.out.println("[spawn-queue-contract] PASS");
    }

    private static void retriesAllSixtyTwoSlotsWithoutDuplicates() {
        RaidSpawnWorkQueue queue = new RaidSpawnWorkQueue();
        List<RaidSpawnWorkQueue.SpawnAnchor> anchors = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            anchors.add(new RaidSpawnWorkQueue.SpawnAnchor(i * 20, 64, 0));
        }
        List<RaidSpawnWorkQueue.SpawnSlot> slots = new ArrayList<>();
        for (int i = 0; i < 62; i++) {
            slots.add(new RaidSpawnWorkQueue.SpawnSlot(i, "minecraft:pillager",
                    RaiderCategory.VANILLA_SIDE_POINT, i % 6, i / 6));
        }
        check(queue.enqueue(new RaidSpawnWorkQueue.BatchPlan("custom:11:0", 11, anchors, slots)),
                "first plan must enqueue");
        check(!queue.enqueue(new RaidSpawnWorkQueue.BatchPlan("custom:11:0", 11, anchors, slots)),
                "duplicate batch id must be rejected");

        Map<Integer, Integer> attemptsBySlot = new HashMap<>();
        Map<Integer, List<Integer>> anchorsBySlot = new HashMap<>();
        Set<Integer> spawnedSlots = new HashSet<>();
        int ticks = 0;
        while (queue.hasPendingWork()) {
            RaidSpawnWorkQueue.DrainReport report = queue.drain(8, 12, attempt -> {
                attemptsBySlot.merge(attempt.slotIndex(), 1, Integer::sum);
                anchorsBySlot.computeIfAbsent(attempt.slotIndex(), ignored -> new ArrayList<>())
                        .add(attempt.anchor().x());
                if (attempt.previousAttempts() < 2) {
                    return RaidSpawnWorkQueue.AttemptResult.RETRY;
                }
                check(spawnedSlots.add(attempt.slotIndex()), "one slot spawned more than once");
                return RaidSpawnWorkQueue.AttemptResult.SPAWNED;
            });
            check(report.attempted() <= 8, "per-tick slot limit exceeded");
            check(++ticks < 100, "queue did not terminate");
        }

        List<RaidSpawnWorkQueue.BatchResult> results = queue.pollCompleted();
        check(results.size() == 1, "expected one completed batch");
        RaidSpawnWorkQueue.BatchResult result = results.getFirst();
        check(result.planned() == 62, "planned count changed");
        check(result.spawned() == 62, "not every retryable slot spawned");
        check(result.exhausted() == 0, "retryable slots were exhausted");
        check(spawnedSlots.size() == 62, "spawned slot identity count changed");
        check(attemptsBySlot.values().stream().allMatch(value -> value == 3),
                "each slot should succeed on its third attempt");
        check(new HashSet<>(anchorsBySlot.get(0)).size() == 3,
                "failed slots did not rotate through anchors");
    }

    private static void exhaustsAndExplicitlyReleasesAPlan() {
        RaidSpawnWorkQueue queue = new RaidSpawnWorkQueue();
        List<RaidSpawnWorkQueue.SpawnAnchor> anchors = List.of(
                new RaidSpawnWorkQueue.SpawnAnchor(0, 64, 0));
        List<RaidSpawnWorkQueue.SpawnSlot> slots = List.of(
                new RaidSpawnWorkQueue.SpawnSlot(0, "minecraft:ravager",
                        RaiderCategory.VANILLA_MAIN_POINT, 0, 0));
        RaidSpawnWorkQueue.BatchPlan plan = new RaidSpawnWorkQueue.BatchPlan("custom:11:0", 11, anchors, slots);
        check(queue.enqueue(plan), "exhaustion plan must enqueue");
        while (queue.hasPendingWork()) {
            queue.drain(1, 3, ignored -> RaidSpawnWorkQueue.AttemptResult.RETRY);
        }
        RaidSpawnWorkQueue.BatchResult result = queue.pollCompleted().getFirst();
        check(result.allFailed(), "all-failed batch was not reported");
        check(result.exhausted() == 1, "exhausted slot count changed");
        queue.releaseReservation(plan.batchId());
        check(queue.enqueue(plan), "explicit rollback did not release idempotency reservation");
    }

    private static void sharesOneBoundedAdmissionBudgetPerLevelTick() {
        Object level = new Object();
        check(RaidSpawnTickBudget.claim(level, 100L, 10, 16) == 10,
                "first queue did not receive its requested allowance");
        check(RaidSpawnTickBudget.claim(level, 100L, 10, 16) == 6,
                "same-level queues exceeded the shared tick cap");
        check(RaidSpawnTickBudget.claim(level, 100L, 1, 16) == 0,
                "exhausted level budget admitted more work");
        check(RaidSpawnTickBudget.claim(level, 101L, 8, 16) == 8,
                "level budget did not reset on the next game time");
        check(RaidSpawnTickBudget.claim(new Object(), 100L, 16, 16) == 16,
                "independent level key did not receive an independent budget");
    }

    private static void persistsAndRestoresOnlyRemainingSlots() {
        RaidSpawnWorkQueue queue = new RaidSpawnWorkQueue();
        List<RaidSpawnWorkQueue.SpawnAnchor> anchors = List.of(
                new RaidSpawnWorkQueue.SpawnAnchor(0, 64, 0),
                new RaidSpawnWorkQueue.SpawnAnchor(20, 64, 0));
        List<RaidSpawnWorkQueue.SpawnSlot> slots = List.of(
                new RaidSpawnWorkQueue.SpawnSlot(0, "minecraft:pillager", RaiderCategory.VANILLA_MAIN_POINT, 0, 0),
                new RaidSpawnWorkQueue.SpawnSlot(1, "minecraft:vindicator", RaiderCategory.VANILLA_SIDE_POINT, 0, 1),
                new RaidSpawnWorkQueue.SpawnSlot(2, "minecraft:ravager", RaiderCategory.VANILLA_MAIN_POINT, 1, 2));
        RaidSpawnWorkQueue.BatchPlan plan = new RaidSpawnWorkQueue.BatchPlan("custom:11:0", 11, anchors, slots);
        check(queue.enqueue(plan), "persistence plan must enqueue");
        queue.drain(2, 12, attempt -> attempt.slotIndex() == 0
                ? RaidSpawnWorkQueue.AttemptResult.SPAWNED : RaidSpawnWorkQueue.AttemptResult.RETRY);

        Properties properties = new Properties();
        RaidSpawnQueuePersistenceCodec.write(properties, "test.", queue.snapshot());
        RaidSpawnWorkQueue.QueueSnapshot decoded = RaidSpawnQueuePersistenceCodec.read(properties, "test.");
        RaidSpawnWorkQueue restored = new RaidSpawnWorkQueue();
        RaidSpawnWorkQueue.RestoreReport restore = restored.restore(decoded);
        check(restore.restoredBatches() == 1, "active batch was not restored");
        check(restore.restoredPendingSlots() == 2, "completed slot was restored as pending");
        check(!restored.enqueue(plan), "restored reservation allowed duplicate enqueue");

        Set<Integer> spawnedAfterRestore = new HashSet<>();
        while (restored.hasPendingWork()) {
            restored.drain(8, 12, attempt -> {
                check(attempt.slotIndex() != 0, "already-completed slot was retried after restore");
                if (attempt.slotIndex() == 1) {
                    check(attempt.previousAttempts() == 1, "retry count was not restored");
                    check(attempt.anchor().x() == 20, "anchor rotation did not continue after restore");
                }
                check(spawnedAfterRestore.add(attempt.slotIndex()), "restored slot spawned twice");
                return RaidSpawnWorkQueue.AttemptResult.SPAWNED;
            });
        }
        RaidSpawnWorkQueue.BatchResult result = restored.pollCompleted().getFirst();
        check(result.planned() == 3 && result.spawned() == 3 && result.exhausted() == 0,
                "restored batch accounting changed");
    }

    private static void rejectsCorruptPartialBatchSnapshots() {
        RaidSpawnWorkQueue queue = new RaidSpawnWorkQueue();
        RaidSpawnWorkQueue.BatchPlan plan = new RaidSpawnWorkQueue.BatchPlan("special:7", 7,
                List.of(new RaidSpawnWorkQueue.SpawnAnchor(0, 64, 0)),
                List.of(
                        new RaidSpawnWorkQueue.SpawnSlot(0, "minecraft:witch", RaiderCategory.RAIDS_ENHANCED_SPECIAL, 0, 0),
                        new RaidSpawnWorkQueue.SpawnSlot(1, "minecraft:evoker", RaiderCategory.RAIDS_ENHANCED_SPECIAL, 0, 1)));
        check(queue.enqueue(plan), "corruption plan must enqueue");
        Properties properties = new Properties();
        RaidSpawnQueuePersistenceCodec.write(properties, "corrupt.", queue.snapshot());
        properties.setProperty("corrupt.pending.1.slotIndex", "0");
        RaidSpawnWorkQueue restored = new RaidSpawnWorkQueue();
        RaidSpawnWorkQueue.RestoreReport report = restored.restore(
                RaidSpawnQueuePersistenceCodec.read(properties, "corrupt."));
        check(report.rejectedBatches() == 1, "corrupt batch was not rejected");
        check(!restored.hasPendingWork(), "partial corrupt batch leaked pending work");
        check(restored.enqueue(plan), "rejected batch reservation blocked a fresh safe plan");
    }

    private static void readsStableNativeRaidIdentity() {
        check(Integer.valueOf(42).equals(RaidKeyService.nativeRaidNumericId(new FakeRaid(42))),
                "stable native raid id was not readable");
        check(RaidKeyService.nativeRaidNumericId(new Object()) == null,
                "missing native raid id did not fail closed");
    }

    public record FakeRaid(int id) {
        public int getId() {
            return id;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
