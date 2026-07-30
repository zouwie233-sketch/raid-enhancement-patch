package com.noah.raidenhancement.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-free contract test executed before the NeoForge Gradle build. */
public final class RaidSpawnWorkQueueContractTest {
    private RaidSpawnWorkQueueContractTest() {
    }

    public static void main(String[] args) {
        retriesAllSixtyTwoSlotsWithoutDuplicates();
        exhaustsAndExplicitlyReleasesAPlan();
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
