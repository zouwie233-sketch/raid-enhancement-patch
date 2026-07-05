package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime state container for one active raid.
 *
 * Runtime model populated by the staged raid controller. Step 8.8 keeps the
 * stabilized clear-time budget, non-destructive timeout raider audit counters,
 * and adds an active-combat clock so clear time pauses after all players leave
 * the battlefield for a grace window.
 */
public final class RaidSession {
    private final String sessionKey;
    private final String dimensionId;
    private final int raidId;
    private final long createdGameTime;

    private int currentWave;
    private int totalWaves;
    private int omenLevel;
    private RaidDifficulty difficulty;
    private RaidWavePlan currentWavePlan;

    private long waveStartedGameTime;
    private boolean currentWaveClearStarted;
    private boolean currentWaveClockPaused;
    private long currentWaveNoCombatSinceGameTime;
    private long currentWavePauseStartedGameTime;
    private long currentWaveTotalPausedTicks;
    private long currentWaveLastPresenceUpdateGameTime;
    private int currentWaveActiveCombatPlayers;
    private long lastUpdatedGameTime;
    private int currentWaveBaseBudgetSeconds;
    private int currentWaveRaiderWeightSeconds;
    private int currentWaveSidePointSeconds;
    private int currentWaveExtraWaveSeconds;
    private int currentWaveBudgetSeconds;
    private long currentWaveBudgetTicks;
    private long currentWaveTimeoutGameTime;
    private long lastWaveBudgetRefreshGameTime;
    private boolean currentWaveBudgetLocked;
    private long currentWaveBudgetLockGameTime;
    private int currentWavePostLockExtraSeconds;
    private int currentWavePostLockExtraCapSeconds;
    private boolean currentWaveTimedOut;
    private boolean currentWaveTimeoutWarningSent;
    private long currentWaveLastAuditGameTime;
    private int currentWaveAuditRemainingCount;
    private int currentWaveAuditNormalCount;
    private int currentWaveAuditFarCount;
    private int currentWaveAuditStuckCount;
    private int currentWaveAuditMissingCount;
    private int currentWaveAuditSpecialCount;
    private int currentWaveAuditLocatedCount;
    private boolean failed;
    private boolean completed;
    private String closeReason = "active";

    private final Map<UUID, RaiderRecord> trackedRaiders = new LinkedHashMap<>();
    private final Map<UUID, ProtectedVillagerRecord> protectedVillagers = new LinkedHashMap<>();
    private final Set<UUID> settlementParticipants = new HashSet<>();
    private long lastSettlementParticipantUpdateGameTime;

    public RaidSession(String sessionKey, String dimensionId, int raidId, long createdGameTime) {
        this.sessionKey = Objects.requireNonNull(sessionKey, "sessionKey");
        this.dimensionId = dimensionId == null || dimensionId.isBlank() ? "unknown" : dimensionId;
        this.raidId = raidId;
        this.createdGameTime = createdGameTime;
        this.lastUpdatedGameTime = createdGameTime;
        this.difficulty = RaidDifficulty.NORMAL;
    }

    public String sessionKey() {
        return sessionKey;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public int raidId() {
        return raidId;
    }

    public long createdGameTime() {
        return createdGameTime;
    }

    public int currentWave() {
        return currentWave;
    }

    public int totalWaves() {
        return totalWaves;
    }

    public int omenLevel() {
        return omenLevel;
    }

    public RaidDifficulty difficulty() {
        return difficulty;
    }

    public long waveStartedGameTime() {
        return waveStartedGameTime;
    }

    public boolean currentWaveClearStarted() {
        return currentWaveClearStarted;
    }

    public boolean currentWaveClockPaused() {
        return currentWaveClockPaused;
    }

    public int currentWaveActiveCombatPlayers() {
        return currentWaveActiveCombatPlayers;
    }

    public long currentWaveNoCombatSinceGameTime() {
        return currentWaveNoCombatSinceGameTime;
    }

    public long currentWaveTotalPausedTicks(long gameTime) {
        return pausedTicksAsOf(gameTime);
    }

    public long lastUpdatedGameTime() {
        return lastUpdatedGameTime;
    }

    public int currentWaveBaseBudgetSeconds() {
        return currentWaveBaseBudgetSeconds;
    }

    public int currentWaveRaiderWeightSeconds() {
        return currentWaveRaiderWeightSeconds;
    }

    public int currentWaveSidePointSeconds() {
        return currentWaveSidePointSeconds;
    }

    public int currentWaveExtraWaveSeconds() {
        return currentWaveExtraWaveSeconds;
    }

    public int currentWaveBudgetSeconds() {
        return currentWaveBudgetSeconds;
    }

    public long currentWaveBudgetTicks() {
        return currentWaveBudgetTicks;
    }

    public long currentWaveTimeoutGameTime() {
        return currentWaveTimeoutGameTime;
    }

    public boolean currentWaveBudgetLocked() {
        return currentWaveBudgetLocked;
    }

    public long currentWaveBudgetLockGameTime() {
        return currentWaveBudgetLockGameTime;
    }

    public int currentWavePostLockExtraSeconds() {
        return currentWavePostLockExtraSeconds;
    }

    public int currentWavePostLockExtraCapSeconds() {
        return currentWavePostLockExtraCapSeconds;
    }

    public boolean currentWaveBudgetCollecting(long gameTime) {
        if (!RaidEnhancementConfig.WAVE_TIME_BUDGET_ENABLED || currentWave <= 0
                || !currentWaveClearStarted || currentWaveBudgetLocked) {
            return false;
        }
        long collectionTicks = Math.max(0L, RaidEnhancementConfig.WAVE_TIME_BUDGET_COLLECTION_TICKS);
        return waveStartedGameTime > 0L && currentWaveEffectiveElapsedTicks(gameTime) < collectionTicks;
    }

    public int currentWaveCollectionRemainingSeconds(long gameTime) {
        if (!currentWaveClearStarted || waveStartedGameTime <= 0L || currentWaveBudgetLocked) {
            return 0;
        }
        long collectionTicks = Math.max(0L, RaidEnhancementConfig.WAVE_TIME_BUDGET_COLLECTION_TICKS);
        long remainingTicks = collectionTicks - currentWaveEffectiveElapsedTicks(gameTime);
        return (int) Math.max(0L, (remainingTicks + 19L) / 20L);
    }

    public int currentWaveElapsedSeconds(long gameTime) {
        if (waveStartedGameTime <= 0L || gameTime <= waveStartedGameTime) {
            return 0;
        }
        return (int) Math.max(0L, currentWaveEffectiveElapsedTicks(gameTime) / 20L);
    }

    public int currentWaveRemainingSeconds(long gameTime) {
        if (currentWaveBudgetTicks <= 0L || waveStartedGameTime <= 0L) {
            return 0;
        }
        long remainingTicks = currentWaveBudgetTicks - currentWaveEffectiveElapsedTicks(gameTime);
        return (int) Math.max(0L, (remainingTicks + 19L) / 20L);
    }

    public boolean currentWaveTimedOut() {
        return currentWaveTimedOut;
    }

    public boolean currentWaveTimeoutWarningSent() {
        return currentWaveTimeoutWarningSent;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String closeReason() {
        return closeReason;
    }

    public Optional<RaidWavePlan> currentWavePlan() {
        return Optional.ofNullable(currentWavePlan);
    }

    public void beginWave(RaidWavePlan wavePlan, long gameTime) {
        this.currentWavePlan = Objects.requireNonNull(wavePlan, "wavePlan");
        this.currentWave = wavePlan.waveIndex();
        this.totalWaves = wavePlan.totalWaves();
        this.omenLevel = wavePlan.omenLevel();
        this.difficulty = wavePlan.difficulty();
        // Step 8.6: planning a wave is not the same as starting the clear timer.
        // Vanilla can create a Raid object before the first raider has actually
        // spawned. Keep the timer dormant until the first live raider is observed.
        this.waveStartedGameTime = 0L;
        this.currentWaveClearStarted = false;
        this.currentWaveClockPaused = false;
        this.currentWaveNoCombatSinceGameTime = 0L;
        this.currentWavePauseStartedGameTime = 0L;
        this.currentWaveTotalPausedTicks = 0L;
        this.currentWaveLastPresenceUpdateGameTime = gameTime;
        this.currentWaveActiveCombatPlayers = 0;
        this.lastUpdatedGameTime = gameTime;
        this.currentWaveBaseBudgetSeconds = 0;
        this.currentWaveRaiderWeightSeconds = 0;
        this.currentWaveSidePointSeconds = 0;
        this.currentWaveExtraWaveSeconds = 0;
        this.currentWaveBudgetSeconds = 0;
        this.currentWaveBudgetTicks = 0L;
        this.currentWaveTimeoutGameTime = 0L;
        this.lastWaveBudgetRefreshGameTime = 0L;
        this.currentWaveBudgetLocked = false;
        this.currentWaveBudgetLockGameTime = 0L;
        this.currentWavePostLockExtraSeconds = 0;
        this.currentWavePostLockExtraCapSeconds = postLockExtraCapSecondsForWave(wavePlan.waveIndex());
        this.currentWaveTimedOut = false;
        this.currentWaveTimeoutWarningSent = false;
        resetCurrentWaveAudit();
        this.trackedRaiders.clear();
    }

    private void startCurrentWaveClearIfNeeded(long gameTime, boolean alive) {
        if (!alive || currentWave <= 0 || currentWaveClearStarted) {
            return;
        }
        currentWaveClearStarted = true;
        waveStartedGameTime = Math.max(1L, gameTime);
        currentWaveClockPaused = false;
        currentWaveNoCombatSinceGameTime = 0L;
        currentWavePauseStartedGameTime = 0L;
        currentWaveTotalPausedTicks = 0L;
        currentWaveLastPresenceUpdateGameTime = gameTime;
        currentWaveActiveCombatPlayers = 0;
        currentWaveBudgetLocked = false;
        currentWaveBudgetLockGameTime = 0L;
        currentWavePostLockExtraSeconds = 0;
        currentWavePostLockExtraCapSeconds = postLockExtraCapSecondsForWave(currentWave);
        currentWaveTimedOut = false;
        currentWaveTimeoutWarningSent = false;
        currentWaveBudgetSeconds = 0;
        currentWaveBudgetTicks = 0L;
        currentWaveTimeoutGameTime = 0L;
        lastWaveBudgetRefreshGameTime = 0L;
    }

    public boolean addRaider(RaiderRecord record) {
        Objects.requireNonNull(record, "record");
        RaiderRecord existing = trackedRaiders.get(record.entityUuid());
        if (existing == null && trackedRaiders.size() >= RaidEnhancementConfig.MAX_TRACKED_RAIDERS_PER_RAID) {
            return false;
        }
        trackedRaiders.put(record.entityUuid(), existing == null ? record : existing.refreshed(
                record.entityId(), record.category(), record.waveIndex(), record.lastSeenGameTime(),
                record.lastKnownX(), record.lastKnownY(), record.lastKnownZ(), record.aliveWhenLastSeen(),
                record.timeWeightSeconds(), record.raidCaptainWhenLastSeen()));
        startCurrentWaveClearIfNeeded(record.lastSeenGameTime(), record.aliveWhenLastSeen());
        touch(record.lastSeenGameTime());
        refreshWaveTimeBudget(record.lastSeenGameTime());
        return true;
    }

    public boolean recordRaiderObservation(UUID entityUuid, String entityId, RaiderCategory category, int waveIndex,
                                           long gameTime, int x, int y, int z, boolean alive) {
        return recordRaiderObservation(entityUuid, entityId, category, waveIndex, gameTime, x, y, z, alive,
                RaiderTimeWeights.weightSeconds(entityId, category, false), false);
    }

    public boolean recordRaiderObservation(UUID entityUuid, String entityId, RaiderCategory category, int waveIndex,
                                           long gameTime, int x, int y, int z, boolean alive,
                                           int timeWeightSeconds, boolean raidCaptain) {
        Objects.requireNonNull(entityUuid, "entityUuid");
        RaiderCategory resolvedCategory = category == null ? RaiderCategory.VANILLA_MAIN_POINT : category;
        RaiderRecord existing = trackedRaiders.get(entityUuid);
        if (existing == null) {
            if (trackedRaiders.size() >= RaidEnhancementConfig.MAX_TRACKED_RAIDERS_PER_RAID) {
                return false;
            }
            trackedRaiders.put(entityUuid, new RaiderRecord(entityUuid, entityId, resolvedCategory,
                    Math.max(1, waveIndex), gameTime, gameTime, x, y, z, alive,
                    timeWeightSeconds, raidCaptain));
        } else {
            trackedRaiders.put(entityUuid, existing.refreshed(entityId, resolvedCategory, Math.max(1, waveIndex),
                    gameTime, x, y, z, alive, timeWeightSeconds, raidCaptain));
        }
        startCurrentWaveClearIfNeeded(gameTime, alive);
        touch(gameTime);
        refreshWaveTimeBudget(gameTime);
        return true;
    }

    public int removeRaidersNotSeenSince(long oldestAllowedLastSeenGameTime) {
        int before = trackedRaiders.size();
        trackedRaiders.entrySet().removeIf(entry -> entry.getValue().lastSeenGameTime() < oldestAllowedLastSeenGameTime);
        int removed = before - trackedRaiders.size();
        if (removed > 0) {
            refreshWaveTimeBudget(oldestAllowedLastSeenGameTime);
        }
        return removed;
    }

    public boolean removeRaider(UUID entityUuid) {
        if (entityUuid == null) {
            return false;
        }
        boolean removed = trackedRaiders.remove(entityUuid) != null;
        if (removed) {
            refreshWaveTimeBudget(lastUpdatedGameTime);
        }
        return removed;
    }

    public Collection<RaiderRecord> trackedRaiders() {
        return List.copyOf(trackedRaiders.values());
    }

    public int trackedRaiderCount() {
        return trackedRaiders.size();
    }

    public int specialRaiderCount() {
        int count = 0;
        for (RaiderRecord record : trackedRaiders.values()) {
            if (record.isSpecial()) {
                count++;
            }
        }
        return count;
    }

    public int vanillaRaiderCount() {
        return trackedRaiderCount() - specialRaiderCount();
    }

    public boolean updateActiveCombatPresence(long gameTime, boolean hasActiveCombatPlayers, int activeCombatPlayers) {
        currentWaveLastPresenceUpdateGameTime = Math.max(currentWaveLastPresenceUpdateGameTime, gameTime);
        currentWaveActiveCombatPlayers = Math.max(0, activeCombatPlayers);
        if (!RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ENABLED || !currentWaveClearStarted || waveStartedGameTime <= 0L) {
            return false;
        }
        boolean changed = false;
        if (hasActiveCombatPlayers) {
            if (currentWaveClockPaused) {
                long pauseStart = currentWavePauseStartedGameTime <= 0L ? gameTime : currentWavePauseStartedGameTime;
                currentWaveTotalPausedTicks += Math.max(0L, gameTime - pauseStart);
                currentWaveClockPaused = false;
                currentWavePauseStartedGameTime = 0L;
                changed = true;
            }
            if (currentWaveNoCombatSinceGameTime != 0L) {
                currentWaveNoCombatSinceGameTime = 0L;
                changed = true;
            }
        } else {
            if (currentWaveNoCombatSinceGameTime <= 0L) {
                currentWaveNoCombatSinceGameTime = Math.max(waveStartedGameTime, gameTime);
                changed = true;
            }
            long graceTicks = Math.max(0L, RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ABSENCE_GRACE_TICKS);
            long pauseStart = currentWaveNoCombatSinceGameTime + graceTicks;
            if (!currentWaveClockPaused && gameTime >= pauseStart) {
                currentWaveClockPaused = true;
                currentWavePauseStartedGameTime = Math.max(waveStartedGameTime, pauseStart);
                changed = true;
            }
        }
        recalculateTimeoutGameTime(gameTime);
        if (currentWaveTimedOut && currentWaveRemainingSeconds(gameTime) > 0) {
            currentWaveTimedOut = false;
            changed = true;
        }
        if (changed) {
            touch(gameTime);
        }
        return changed;
    }

    public boolean currentWaveHasActiveCombatPlayers() {
        return currentWaveActiveCombatPlayers > 0;
    }

    public boolean currentWaveClockPausedAfterGrace() {
        return currentWaveClockPaused;
    }

    private long pausedTicksAsOf(long gameTime) {
        long paused = Math.max(0L, currentWaveTotalPausedTicks);
        if (currentWaveClockPaused && currentWavePauseStartedGameTime > 0L && gameTime > currentWavePauseStartedGameTime) {
            paused += gameTime - currentWavePauseStartedGameTime;
        }
        return paused;
    }

    private long currentWaveEffectiveElapsedTicks(long gameTime) {
        if (waveStartedGameTime <= 0L || gameTime <= waveStartedGameTime) {
            return 0L;
        }
        return Math.max(0L, gameTime - waveStartedGameTime - pausedTicksAsOf(gameTime));
    }

    private void recalculateTimeoutGameTime(long gameTime) {
        if (waveStartedGameTime <= 0L || currentWaveBudgetTicks <= 0L) {
            currentWaveTimeoutGameTime = 0L;
            return;
        }
        currentWaveTimeoutGameTime = waveStartedGameTime + currentWaveBudgetTicks + pausedTicksAsOf(gameTime);
    }

    public boolean refreshWaveTimeBudget(long gameTime) {
        if (!RaidEnhancementConfig.WAVE_TIME_BUDGET_ENABLED || currentWave <= 0 || !currentWaveClearStarted) {
            return false;
        }
        int baseSeconds = RaiderTimeWeights.baseBudgetSecondsForWave(currentWave);
        int totalAliveRaiderWeightSeconds = sumAliveRaiderWeightSeconds();
        int sidePointSeconds = sidePointBudgetSeconds();
        int extraWaveSeconds = RaiderTimeWeights.extraWaveBonusSeconds(currentWave);
        int maxSeconds = Math.max(baseSeconds, RaiderTimeWeights.maxBudgetSecondsForWave(currentWave));
        int proposedSeconds = clampBudgetSeconds(baseSeconds + totalAliveRaiderWeightSeconds + sidePointSeconds + extraWaveSeconds,
                baseSeconds, maxSeconds);

        boolean changed = false;
        if (!currentWaveBudgetLocked) {
            if (currentWaveBudgetSeconds <= 0 || proposedSeconds > currentWaveBudgetSeconds) {
                applyBudgetSeconds(proposedSeconds);
                changed = true;
            }
            currentWaveBaseBudgetSeconds = baseSeconds;
            currentWaveRaiderWeightSeconds = Math.max(currentWaveRaiderWeightSeconds, totalAliveRaiderWeightSeconds);
            currentWaveSidePointSeconds = Math.max(currentWaveSidePointSeconds, sidePointSeconds);
            currentWaveExtraWaveSeconds = Math.max(currentWaveExtraWaveSeconds, extraWaveSeconds);

            long collectionTicks = Math.max(0L, RaidEnhancementConfig.WAVE_TIME_BUDGET_COLLECTION_TICKS);
            if (waveStartedGameTime > 0L && currentWaveEffectiveElapsedTicks(gameTime) >= collectionTicks) {
                currentWaveBudgetLocked = true;
                currentWaveBudgetLockGameTime = gameTime;
                currentWavePostLockExtraSeconds = 0;
                currentWavePostLockExtraCapSeconds = postLockExtraCapSecondsForWave(currentWave);
                if (proposedSeconds > currentWaveBudgetSeconds) {
                    applyBudgetSeconds(proposedSeconds);
                    changed = true;
                }
            }
        } else {
            int trustedPostLockExtra = trustedPostLockExtraSeconds();
            int cappedExtra = Math.min(Math.max(0, trustedPostLockExtra), Math.max(0, currentWavePostLockExtraCapSeconds));
            int lockedBaselineSeconds = Math.max(baseSeconds,
                    currentWaveBudgetSeconds - Math.max(0, currentWavePostLockExtraSeconds));
            int postLockProposedSeconds = clampBudgetSeconds(lockedBaselineSeconds + cappedExtra, baseSeconds, maxSeconds);
            if (postLockProposedSeconds > currentWaveBudgetSeconds) {
                applyBudgetSeconds(postLockProposedSeconds);
                changed = true;
            }
            currentWavePostLockExtraSeconds = Math.max(currentWavePostLockExtraSeconds, cappedExtra);
            currentWaveBaseBudgetSeconds = Math.max(currentWaveBaseBudgetSeconds, baseSeconds);
            currentWaveRaiderWeightSeconds = Math.max(currentWaveRaiderWeightSeconds,
                    Math.max(0, totalAliveRaiderWeightSeconds - Math.max(0, currentWavePostLockExtraSeconds))
                            + currentWavePostLockExtraSeconds);
            currentWaveSidePointSeconds = Math.max(currentWaveSidePointSeconds, sidePointSeconds);
            currentWaveExtraWaveSeconds = Math.max(currentWaveExtraWaveSeconds, extraWaveSeconds);
        }

        lastWaveBudgetRefreshGameTime = Math.max(lastWaveBudgetRefreshGameTime, gameTime);
        recalculateTimeoutGameTime(gameTime);
        if (currentWaveTimedOut && currentWaveRemainingSeconds(gameTime) > 0) {
            currentWaveTimedOut = false;
        }
        touch(gameTime);
        return changed;
    }

    private int sumAliveRaiderWeightSeconds() {
        int raiderWeightSeconds = 0;
        for (RaiderRecord record : trackedRaiders.values()) {
            if (record.aliveWhenLastSeen()) {
                raiderWeightSeconds += Math.max(0, record.timeWeightSeconds());
            }
        }
        return raiderWeightSeconds;
    }

    private int sidePointBudgetSeconds() {
        if (currentWavePlan == null) {
            return 0;
        }
        long sidePoints = currentWavePlan.attackPoints().stream()
                .filter(point -> point.role() == AttackPointRole.SIDE_ATTACK_POINT)
                .count();
        return (int) Math.min(Integer.MAX_VALUE,
                sidePoints * (long) Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_SIDE_ATTACK_POINT_SECONDS));
    }

    private int trustedPostLockExtraSeconds() {
        if (!currentWaveBudgetLocked || currentWaveBudgetLockGameTime <= 0L) {
            return 0;
        }
        int seconds = 0;
        for (RaiderRecord record : trackedRaiders.values()) {
            if (!record.aliveWhenLastSeen()) {
                continue;
            }
            if (record.firstSeenGameTime() <= currentWaveBudgetLockGameTime) {
                continue;
            }
            if (!isTrustedPostLockExtension(record)) {
                continue;
            }
            seconds += Math.max(0, record.timeWeightSeconds());
        }
        return seconds;
    }

    private boolean isTrustedPostLockExtension(RaiderRecord record) {
        if (record == null) {
            return false;
        }
        // Ordinary VANILLA_MAIN_POINT records discovered after the lock are treated as
        // late cache discoveries, not real reinforcements. They remain tracked for later
        // stuck-raider work, but they no longer inflate the clear timer.
        return record.category() == RaiderCategory.VANILLA_SIDE_POINT
                || record.category() == RaiderCategory.RAIDS_ENHANCED_SPECIAL;
    }

    private void applyBudgetSeconds(int seconds) {
        currentWaveBudgetSeconds = Math.max(0, seconds);
        currentWaveBudgetTicks = currentWaveBudgetSeconds * 20L;
        currentWaveTimeoutGameTime = waveStartedGameTime + currentWaveBudgetTicks + Math.max(0L, currentWaveTotalPausedTicks);
    }

    private static int clampBudgetSeconds(int value, int min, int max) {
        return Math.min(Math.max(min, value), Math.max(min, max));
    }

    private static int postLockExtraCapSecondsForWave(int waveIndex) {
        if (waveIndex >= 11) {
            return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_11_PLUS);
        }
        if (waveIndex == 10) {
            return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_10);
        }
        if (waveIndex == 9) {
            return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_9);
        }
        if (waveIndex >= 7) {
            return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_7_TO_8);
        }
        if (waveIndex >= 4) {
            return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_4_TO_6);
        }
        return Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_1_TO_3);
    }

    public boolean updateWaveTimeoutState(long gameTime) {
        if (!RaidEnhancementConfig.WAVE_TIME_BUDGET_ENABLED || currentWaveBudgetTicks <= 0L
                || waveStartedGameTime <= 0L || !currentWaveBudgetLocked) {
            return false;
        }
        if (trackedRaiders.isEmpty()) {
            return false;
        }
        if (currentWaveEffectiveElapsedTicks(gameTime) < Math.max(0, RaidEnhancementConfig.WAVE_TIME_BUDGET_TIMEOUT_EVALUATION_GRACE_TICKS)) {
            return false;
        }
        boolean hasAliveKnownRaider = false;
        for (RaiderRecord record : trackedRaiders.values()) {
            if (record.aliveWhenLastSeen()) {
                hasAliveKnownRaider = true;
                break;
            }
        }
        if (!hasAliveKnownRaider) {
            currentWaveTimedOut = false;
            return false;
        }
        recalculateTimeoutGameTime(gameTime);
        if (currentWaveClockPaused) {
            return false;
        }
        boolean timedOutNow = currentWaveEffectiveElapsedTicks(gameTime) > currentWaveBudgetTicks;
        boolean shouldWarn = timedOutNow && !currentWaveTimedOut && !currentWaveTimeoutWarningSent;
        currentWaveTimedOut = timedOutNow;
        touch(gameTime);
        return shouldWarn;
    }

    public void markWaveTimeoutWarningSent(long gameTime) {
        this.currentWaveTimeoutWarningSent = true;
        this.currentWaveTimedOut = true;
        touch(gameTime);
    }

    public void updateTimeoutAuditSummary(long gameTime, int remaining, int normal, int far,
                                          int stuck, int missing, int special, int located) {
        this.currentWaveLastAuditGameTime = Math.max(this.currentWaveLastAuditGameTime, gameTime);
        this.currentWaveAuditRemainingCount = Math.max(0, remaining);
        this.currentWaveAuditNormalCount = Math.max(0, normal);
        this.currentWaveAuditFarCount = Math.max(0, far);
        this.currentWaveAuditStuckCount = Math.max(0, stuck);
        this.currentWaveAuditMissingCount = Math.max(0, missing);
        this.currentWaveAuditSpecialCount = Math.max(0, special);
        this.currentWaveAuditLocatedCount = Math.max(0, located);
        touch(gameTime);
    }

    private void resetCurrentWaveAudit() {
        this.currentWaveLastAuditGameTime = 0L;
        this.currentWaveAuditRemainingCount = 0;
        this.currentWaveAuditNormalCount = 0;
        this.currentWaveAuditFarCount = 0;
        this.currentWaveAuditStuckCount = 0;
        this.currentWaveAuditMissingCount = 0;
        this.currentWaveAuditSpecialCount = 0;
        this.currentWaveAuditLocatedCount = 0;
    }

    public long currentWaveLastAuditGameTime() {
        return currentWaveLastAuditGameTime;
    }

    public int currentWaveAuditRemainingCount() {
        return currentWaveAuditRemainingCount;
    }

    public int currentWaveAuditNormalCount() {
        return currentWaveAuditNormalCount;
    }

    public int currentWaveAuditFarCount() {
        return currentWaveAuditFarCount;
    }

    public int currentWaveAuditStuckCount() {
        return currentWaveAuditStuckCount;
    }

    public int currentWaveAuditMissingCount() {
        return currentWaveAuditMissingCount;
    }

    public int currentWaveAuditSpecialCount() {
        return currentWaveAuditSpecialCount;
    }

    public int currentWaveAuditLocatedCount() {
        return currentWaveAuditLocatedCount;
    }

    public boolean protectVillager(UUID villagerUuid, int gameTick) {
        Objects.requireNonNull(villagerUuid, "villagerUuid");
        if (protectedVillagers.size() >= RaidEnhancementConfig.MAX_PROTECTED_VILLAGERS_PER_RAID
                && !protectedVillagers.containsKey(villagerUuid)) {
            return false;
        }
        ProtectedVillagerRecord existing = protectedVillagers.get(villagerUuid);
        protectedVillagers.put(villagerUuid, existing == null
                ? new ProtectedVillagerRecord(villagerUuid, gameTick, gameTick)
                : existing.refreshed(gameTick));
        return true;
    }

    public boolean unprotectVillager(UUID villagerUuid) {
        if (villagerUuid == null) {
            return false;
        }
        return protectedVillagers.remove(villagerUuid) != null;
    }


    public void markSettlementParticipant(UUID playerUuid, long gameTime) {
        if (playerUuid == null) {
            return;
        }
        settlementParticipants.add(playerUuid);
        lastSettlementParticipantUpdateGameTime = Math.max(lastSettlementParticipantUpdateGameTime, gameTime);
        touch(gameTime);
    }

    public Collection<UUID> settlementParticipants() {
        return List.copyOf(settlementParticipants);
    }

    public int settlementParticipantCount() {
        return settlementParticipants.size();
    }

    public long lastSettlementParticipantUpdateGameTime() {
        return lastSettlementParticipantUpdateGameTime;
    }

    public Collection<ProtectedVillagerRecord> protectedVillagers() {
        return List.copyOf(protectedVillagers.values());
    }

    public int protectedVillagerCount() {
        return protectedVillagers.size();
    }

    public List<UUID> trackedRaiderUuids() {
        return new ArrayList<>(trackedRaiders.keySet());
    }

    public List<UUID> protectedVillagerUuids() {
        return new ArrayList<>(protectedVillagers.keySet());
    }

    public void markFailed(String reason, long gameTime) {
        this.failed = true;
        this.completed = false;
        this.closeReason = reason == null || reason.isBlank() ? "failed" : reason;
        touch(gameTime);
    }

    public void markCompleted(String reason, long gameTime) {
        this.completed = true;
        this.failed = false;
        this.closeReason = reason == null || reason.isBlank() ? "completed" : reason;
        touch(gameTime);
    }

    public void touch(long gameTime) {
        this.lastUpdatedGameTime = Math.max(this.lastUpdatedGameTime, gameTime);
    }

    public boolean isClosed() {
        return failed || completed;
    }

    public String shortSummary() {
        return "RaidSession{" +
                "key='" + sessionKey + '\'' +
                ", dimension='" + dimensionId + '\'' +
                ", raidId=" + raidId +
                ", wave=" + currentWave + "/" + totalWaves +
                ", omen=" + omenLevel +
                ", difficulty=" + difficulty +
                ", raiders=" + trackedRaiderCount() +
                ", specials=" + specialRaiderCount() +
                ", clearStarted=" + currentWaveClearStarted +
                ", activePlayers=" + currentWaveActiveCombatPlayers +
                ", clockPaused=" + currentWaveClockPaused +
                ", pausedTicks=" + currentWaveTotalPausedTicks +
                ", budgetSeconds=" + currentWaveBudgetSeconds +
                ", budgetLocked=" + currentWaveBudgetLocked +
                ", postLockExtraSeconds=" + currentWavePostLockExtraSeconds +
                ", raiderWeightSeconds=" + currentWaveRaiderWeightSeconds +
                ", timedOut=" + currentWaveTimedOut +
                ", auditRemaining=" + currentWaveAuditRemainingCount +
                ", auditFar=" + currentWaveAuditFarCount +
                ", auditStuck=" + currentWaveAuditStuckCount +
                ", auditMissing=" + currentWaveAuditMissingCount +
                ", protectedVillagers=" + protectedVillagerCount() +
                ", closed=" + isClosed() +
                '}';
    }
}
