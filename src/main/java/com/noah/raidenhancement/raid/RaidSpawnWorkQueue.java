package com.noah.raidenhancement.raid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Per-raid, server-thread-owned queue for bounded patch-controlled entity spawning.
 *
 * <p>Planning and execution are intentionally separate. A batch id is reserved once,
 * while individual slots remain pending until they spawn or exhaust their bounded retry
 * allowance. This prevents a partially successful wave from silently discarding every
 * entity that happened to be behind the per-tick safe-spawn budget.</p>
 */
public final class RaidSpawnWorkQueue {
    private final Map<String, BatchState> activeBatches = new LinkedHashMap<>();
    private final Set<String> reservedBatchIds = new HashSet<>();
    private final ArrayDeque<PendingSlot> pendingSlots = new ArrayDeque<>();
    private final ArrayDeque<BatchResult> completedBatches = new ArrayDeque<>();

    public boolean enqueue(BatchPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (reservedBatchIds.contains(plan.batchId())) {
            return false;
        }
        reservedBatchIds.add(plan.batchId());
        BatchState state = new BatchState(plan);
        activeBatches.put(plan.batchId(), state);
        for (SpawnSlot slot : plan.slots()) {
            pendingSlots.addLast(new PendingSlot(plan.batchId(), slot));
        }
        if (plan.slots().isEmpty()) {
            finishBatch(state);
        }
        return true;
    }

    public DrainReport drain(int maxSlots, int maxAttemptsPerSlot, SpawnExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        int limit = Math.max(1, maxSlots);
        int attemptsLimit = Math.max(1, maxAttemptsPerSlot);
        int attempted = 0;
        int spawned = 0;
        int deferred = 0;
        int exhausted = 0;

        while (attempted < limit && !pendingSlots.isEmpty()) {
            PendingSlot pending = pendingSlots.removeFirst();
            BatchState batch = activeBatches.get(pending.batchId);
            if (batch == null) {
                continue;
            }
            attempted++;
            SpawnAttempt attempt = resolveAttempt(batch.plan, pending);
            AttemptResult result;
            try {
                result = executor.attempt(attempt);
            } catch (Throwable exception) {
                result = AttemptResult.RETRY;
            }
            if (result == null) {
                result = AttemptResult.RETRY;
            }

            if (result == AttemptResult.SPAWNED) {
                batch.pending--;
                batch.spawned++;
                spawned++;
                completeIfFinished(batch);
                continue;
            }
            if (result == AttemptResult.STOP_FOR_TICK) {
                pendingSlots.addFirst(pending);
                break;
            }
            if (result == AttemptResult.PERMANENT_FAILURE) {
                batch.pending--;
                batch.exhausted++;
                exhausted++;
                completeIfFinished(batch);
                continue;
            }

            pending.attempts++;
            if (pending.attempts >= attemptsLimit) {
                batch.pending--;
                batch.exhausted++;
                exhausted++;
                completeIfFinished(batch);
            } else {
                pendingSlots.addLast(pending);
                deferred++;
            }
        }
        return new DrainReport(attempted, spawned, deferred, exhausted, pendingSlots.size());
    }

    public boolean hasPendingWave(int logicalWave) {
        for (BatchState state : activeBatches.values()) {
            if (state.plan.logicalWave() == logicalWave && state.pending > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPendingWork() {
        return !pendingSlots.isEmpty();
    }

    public int pendingCount() {
        return pendingSlots.size();
    }

    public List<BatchResult> pollCompleted() {
        if (completedBatches.isEmpty()) {
            return List.of();
        }
        List<BatchResult> results = new ArrayList<>(completedBatches);
        completedBatches.clear();
        return List.copyOf(results);
    }

    /** Allows an explicitly rolled-back all-failed custom wave to create a fresh plan. */
    public void releaseReservation(String batchId) {
        if (batchId == null || activeBatches.containsKey(batchId)) {
            return;
        }
        reservedBatchIds.remove(batchId);
    }

    private SpawnAttempt resolveAttempt(BatchPlan plan, PendingSlot pending) {
        SpawnSlot slot = pending.slot;
        int anchorCount = plan.anchors().size();
        int anchorIndex = Math.floorMod(slot.preferredAnchorIndex() + pending.attempts, anchorCount);
        SpawnAnchor anchor = plan.anchors().get(anchorIndex);
        int retryRound = pending.attempts / Math.max(1, anchorCount);
        int resolvedLocalIndex = slot.localIndex() + retryRound * 7;
        return new SpawnAttempt(plan.batchId(), plan.logicalWave(), slot.slotIndex(), slot.entityId(),
                slot.category(), anchor, resolvedLocalIndex, pending.attempts);
    }

    private void completeIfFinished(BatchState state) {
        if (state.pending <= 0) {
            finishBatch(state);
        }
    }

    private void finishBatch(BatchState state) {
        activeBatches.remove(state.plan.batchId());
        completedBatches.addLast(new BatchResult(state.plan.batchId(), state.plan.logicalWave(),
                state.plan.slots().size(), state.spawned, state.exhausted));
    }

    public enum AttemptResult {
        SPAWNED,
        RETRY,
        STOP_FOR_TICK,
        PERMANENT_FAILURE
    }

    @FunctionalInterface
    public interface SpawnExecutor {
        AttemptResult attempt(SpawnAttempt attempt);
    }

    public record SpawnAnchor(int x, int y, int z) {
    }

    public record SpawnSlot(int slotIndex, String entityId, RaiderCategory category,
                            int preferredAnchorIndex, int localIndex) {
        public SpawnSlot {
            if (slotIndex < 0) {
                throw new IllegalArgumentException("slotIndex cannot be negative");
            }
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("entityId cannot be blank");
            }
            category = category == null ? RaiderCategory.VANILLA_MAIN_POINT : category;
        }
    }

    public record BatchPlan(String batchId, int logicalWave, List<SpawnAnchor> anchors,
                            List<SpawnSlot> slots) {
        public BatchPlan {
            if (batchId == null || batchId.isBlank()) {
                throw new IllegalArgumentException("batchId cannot be blank");
            }
            if (logicalWave <= 0) {
                throw new IllegalArgumentException("logicalWave must be positive");
            }
            anchors = List.copyOf(anchors == null ? List.of() : anchors);
            slots = List.copyOf(slots == null ? List.of() : slots);
            if (anchors.isEmpty()) {
                throw new IllegalArgumentException("at least one spawn anchor is required");
            }
        }
    }

    public record SpawnAttempt(String batchId, int logicalWave, int slotIndex, String entityId,
                               RaiderCategory category, SpawnAnchor anchor, int localIndex,
                               int previousAttempts) {
    }

    public record DrainReport(int attempted, int spawned, int deferred, int exhausted,
                              int totalPending) {
    }

    public record BatchResult(String batchId, int logicalWave, int planned, int spawned,
                              int exhausted) {
        public boolean allFailed() {
            return planned > 0 && spawned <= 0;
        }
    }

    private static final class BatchState {
        private final BatchPlan plan;
        private int pending;
        private int spawned;
        private int exhausted;

        private BatchState(BatchPlan plan) {
            this.plan = plan;
            this.pending = plan.slots().size();
        }
    }

    private static final class PendingSlot {
        private final String batchId;
        private final SpawnSlot slot;
        private int attempts;

        private PendingSlot(String batchId, SpawnSlot slot) {
            this.batchId = batchId;
            this.slot = slot;
        }
    }
}
