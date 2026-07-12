package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.compat.CachedReflection;
import com.noah.raidenhancement.compat.MobEffectCompat;
import com.noah.raidenhancement.integration.RaidsEnhancedIds;
import com.noah.raidenhancement.villager.ProtectedVillagerState;
import com.noah.raidenhancement.villager.RaidAutoVillagerProtector;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * Step 0.5.4 comprehensive HUD/native wave sync fix.
 *
 * Vanilla 1.21.1 raid spawn-count tables are unsafe when the native Raid is
 * pushed to 9+ logical waves in this modpack. This controller therefore keeps
 * the native raid at the stable cap, remembers the true configured wave target,
 * and spawns the missing high-omen waves as conservative vanilla-raider waves.
 *
 * Step 6 keeps Raids Enhanced native RaidMixin untouched. It does not block,
 * move, or replace the mod's own final-wave special. Instead it adds separate
 * per-wave/per-raid special-raider reinforcements at the user's selected waves.
 *
 * Step 7 adds a lightweight glowing locator layer only. Low-count remaining
 * raiders receive a short glow and a chat hint, while Raids Enhanced special
 * raiders receive a long high-priority glow.
 *
 * Step 8.2 adds raider time weights and a dynamic per-wave clear budget.
 * Step 8.3 adds visible actionbar/chat display for that clear timer.
 * Step 8.4 stabilizes the budget with an initial collection window and merges
 * the timer into the existing top-center HUD. Step 8.5 adds HUD/session
 * lifecycle cleanup and lightweight metadata recovery. Step 8.6 prevents HUD
 * flicker and starts clear timing only after raiders are actually observed.
 * Step 8.7 adds a non-destructive timeout raider audit layer for classifying
 * remaining raiders after timeout monitoring begins. Step 8.8 adds an
 * active-combat clock gate so clear timers and timeout audits pause after all
 * players leave the battlefield for a grace window. Step 8.8.2 fixes return-to-raid
 * stability: paused raids no longer keep spawning optional special packs, reinforced-wave
 * locks are persisted, and existing same-wave timers are not reset after re-discovery.
 * Step 8.8.3 tightens this further: restored in-progress waves rebuild missing runtime
 * sessions, late optional reinforcements are locked out, and special packs spawn only
 * the deficit beyond already-present Raids Enhanced special raiders.
 * Step 8.8.5 rolls back the unsafe 0.8.8.4 RaidSession-level re-planning guard and
 * instead centralizes return/reload optional-reinforcement decisions in this controller:
 * optional packs only open during the live startup collection window of the current
 * RaidSession, while paused/returned/locked waves are reserved closed. Step 8.8.6 fixes
 * the custom-wave return bug: waves 9-11 freeze immediately when no active player is
 * present, so an unloaded/empty scan cannot complete the wave and spawn the next one;
 * the custom-wave native base is also frozen when extra waves are armed so HUD and
 * spawning cannot drift past the planned target. Step 8.8.7 extends the same freeze
 * to the native-final-wave bridge (8 -> 9) and adds a short return-load grace so
 * the bridge cannot treat an unloaded wave-8 field as cleared.
 * These stages are still non-destructive: no hard failure, teleport, clearing, damage, or direct
 * vanilla Raid state mutation is performed.
 */
public final class RaidExtraWaveController {
    private static final String GET_RAID_AT_METHOD = "getRaidAt";
    private static final String BLOCK_POSITION_METHOD = "blockPosition";
    private static final String LEVEL_DIFFICULTY_METHOD = "getDifficulty";
    private static final String RAID_OMEN_METHOD = "getRaidOmenLevel";
    private static final String RAID_NUM_GROUPS_FIELD = "numGroups";
    private static final String RAID_GROUPS_SPAWNED_FIELD = "groupsSpawned";
    private static final String RAID_STATUS_FIELD = "status";
    private static final String RAIDER_CLASS_NAME = "net.minecraft.world.entity.raid.Raider";
    private static final String AABB_CLASS_NAME = "net.minecraft.world.phys.AABB";

    private static final Map<String, ExtraWaveState> STATES = new LinkedHashMap<>();
    private static final Map<String, Long> TERMINATED_RAID_KEYS = new LinkedHashMap<>();
    private static final long TERMINATED_KEY_TTL_TICKS = 12000L;
    private static final long EXTRA_WAVE_RETURN_LOAD_GRACE_TICKS = 60L;
    private static boolean warnedTickFailure;
    private static boolean warnedCommandFailure;
    private static boolean warnedDirectSpawnFailure;
    private static boolean warnedUnsafeSpawnFailure;
    private static boolean warnedSpawnBudgetExhausted;
    private static boolean warnedProtectionFailure;
    private static boolean warnedScanFailure;
    private static boolean warnedFinalizeFailure;
    private static boolean warnedJoinRaidFailure;
    private static boolean warnedEquipFailure;
    private static boolean warnedRaidsEnhancedSpecialFailure;
    private static boolean warnedRaidsEnhancedBlimpFailure;
    private static boolean warnedGlowingLocatorFailure;
    private static boolean warnedKnownRaiderCacheFailure;
    private static boolean warnedWaveTimeBudgetFailure;
    private static boolean warnedWaveTimeDisplayFailure;
    private static boolean warnedTimeoutRaiderAuditFailure;
    private static boolean announcedActiveCombatClock;
    private static boolean announcedFirstSession;
    private static boolean announcedKnownRaiderCache;
    private static boolean announcedWaveTimeBudget;
    private static boolean announcedWaveTimeDisplay;
    private static boolean announcedTimeoutRaiderAudit;
    private static boolean announcedPerformanceAuditCache;
    private static volatile RaidHudSnapshot lastHudSnapshot;
    private static final Map<String, RaidHudSnapshot> LAST_HUD_SNAPSHOTS = new LinkedHashMap<>();
    private static final Map<String, PersistedLifecycleSnapshot> PERSISTED_LIFECYCLE_SNAPSHOTS = new LinkedHashMap<>();
    private static boolean lifecycleSnapshotsLoaded;
    private static boolean warnedLifecyclePersistenceFailure;
    private static boolean announcedLifecyclePersistence;

    private RaidExtraWaveController() {
    }

    public static void tick(ServerLevel level) {
        if (!RaidEnhancementConfig.EXTRA_WAVE_LAYER_ENABLED || level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        try {
            ensureLifecycleSnapshotsLoaded();
            pruneTerminatedRaidKeys(gameTime);
            pruneStaleHudSnapshots(level, gameTime);
            RaidEncounterAuthority.prune(gameTime, Math.max(RaidEnhancementConfig.RAID_WAVE_HUD_STALE_TICKS,
                    RaidEnhancementConfig.RAID_WAVE_HUD_CLIENT_STALE_TICKS));
            pruneStalePersistedLifecycleSnapshots(gameTime);
            if (RaidEnhancementConfig.PERFORMANCE_CLEANUP_ENDED_SESSIONS_AGGRESSIVELY
                    && gameTime % Math.max(20, RaidEnhancementConfig.SESSION_CLEANUP_INTERVAL_TICKS) == 0L) {
                int cleanedSessions = RaidSessionManager.cleanupClosedSessions();
                if (cleanedSessions > 0 && RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_LOGS_ENABLED
                        && RaidEnhancementConfig.VILLAGE_SECURITY_DEBUG_PERFORMANCE) {
                    System.out.println("[Raid Enhancement Patch] Performance cleanup removed "
                            + cleanedSessions + " closed raid session(s).");
                }
            }
            if (!announcedPerformanceAuditCache) {
                announcedPerformanceAuditCache = true;
                System.out.println("[Raid Enhancement Patch] 0.8.9.8.5 wave-authority layer is active. Raider cache, straggler locator and timeout audit checks use configurable low-frequency throttles; no raid outcome logic is changed.");
            }
            // 0.4.21: dynamic scan throttle. 0.4.20 used 1-tick discovery to fix
            // missing early HUD snapshots. Keep that fast cadence during startup and
            // continuous-assault bridge windows, but return to a lighter cadence while
            // idle or in stable middle waves. Processing remains 1 tick only when a
            // native->custom or custom->custom transition can race vanilla victory.
            int discoveryInterval = dynamicDiscoveryInterval(level, gameTime);
            if (gameTime % Math.max(1, discoveryInterval) == 0L) {
                discoverNativeRaids(level, gameTime);
            }
            int processInterval = dynamicProcessInterval(level, gameTime);
            if (processInterval <= 1 || gameTime % Math.max(1, processInterval) == 0L) {
                processExtraWaveStates(level, gameTime);
            }
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave layer tick failed once and was suppressed: " + throwable);
            }
        }
    }

    private static int dynamicDiscoveryInterval(ServerLevel level, long gameTime) {
        if (hasContinuousAssaultState(level)) {
            return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_CONTINUOUS_ASSAULT_DISCOVERY_INTERVAL_TICKS);
        }
        if (hasStartupState(level, gameTime)) {
            return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_STARTUP_FAST_DISCOVERY_INTERVAL_TICKS);
        }
        if (hasActiveState(level)) {
            return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_STABLE_DISCOVERY_INTERVAL_TICKS);
        }
        return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_IDLE_DISCOVERY_INTERVAL_TICKS);
    }

    private static int dynamicProcessInterval(ServerLevel level, long gameTime) {
        if (hasContinuousAssaultState(level) || hasStartupState(level, gameTime)) {
            return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_CONTINUOUS_ASSAULT_PROCESS_INTERVAL_TICKS);
        }
        if (hasActiveState(level)) {
            return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_STABLE_PROCESS_INTERVAL_TICKS);
        }
        return Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_IDLE_DISCOVERY_INTERVAL_TICKS);
    }

    private static boolean hasStartupState(ServerLevel level, long gameTime) {
        String dimension = dimensionId(level);
        long startupTicks = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_STARTUP_FAST_DURATION_TICKS);
        for (ExtraWaveState state : STATES.values()) {
            if (state == null || state.completed || !state.dimensionId.equals(dimension)) {
                continue;
            }
            if (gameTime - state.firstSeenGameTime <= startupTicks) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveState(ServerLevel level) {
        String dimension = dimensionId(level);
        for (ExtraWaveState state : STATES.values()) {
            if (state != null && !state.completed && state.dimensionId.equals(dimension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousAssaultState(ServerLevel level) {
        String dimension = dimensionId(level);
        for (ExtraWaveState state : STATES.values()) {
            if (state == null || state.completed || !state.dimensionId.equals(dimension)) {
                continue;
            }
            if (state.extraWavesNeeded() <= 0) {
                continue;
            }
            boolean nativeBridgeWindow = state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves()
                    || state.nativeRaidFinishedObserved
                    || state.bridgeHoldActive;
            boolean customLayerActive = state.armedForExtraWaves
                    || state.currentWaveActive
                    || state.customWavesSpawned > 0;
            if (nativeBridgeWindow || customLayerActive) {
                return true;
            }
        }
        return false;
    }

    private static void discoverNativeRaids(ServerLevel level, long gameTime) {
        Set<String> visited = new HashSet<>();
        for (Object playerObject : playersSnapshot(level)) {
            if (!(playerObject instanceof Entity player) || player.level().isClientSide()) {
                continue;
            }
            Object basePos = invokeNoArg(player, BLOCK_POSITION_METHOD);
            discoverAroundPosition(level, basePos, visited, gameTime);
        }
        for (UUID uuid : VillagerProtectionController.protectedUuidsSnapshot()) {
            Optional<ProtectedVillagerState> stateOptional = VillagerProtectionController.getState(uuid);
            if (stateOptional.isEmpty()) {
                continue;
            }
            ProtectedVillagerState protectedState = stateOptional.get();
            if (!protectedState.dimensionId().equals(dimensionId(level))) {
                continue;
            }
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Object basePos = invokeNoArg(entity, BLOCK_POSITION_METHOD);
            discoverAroundPosition(level, basePos, visited, gameTime);
        }
    }

    private static void discoverAroundPosition(ServerLevel level, Object basePos, Set<String> visited, long gameTime) {
        if (basePos == null || STATES.size() >= Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_MAX_ACTIVE_SESSIONS)) {
            return;
        }
        for (int[] offset : sampleOffsets()) {
            Object pos = offset[0] == 0 && offset[1] == 0 && offset[2] == 0
                    ? basePos
                    : offsetPosition(basePos, offset[0], offset[1], offset[2]);
            if (pos == null) {
                continue;
            }
            Object raid = getRaidAt(level, pos);
            if (raid == null) {
                continue;
            }
            String key = stateKey(level, raid);
            if (isTerminatedRaidKey(key, gameTime)) {
                // 0.4.20: a terminated center-key must only suppress the old finished
                // Raid object. The user tests repeated raids in the same village center;
                // with the 0.4.17 center-key lock, a successful previous raid could block
                // discovery of the next active raid for the full TTL, causing the HUD and
                // extra-wave layer to be absent at the start of Hard Omen V tests.
                // If getRaidAt now exposes an active, non-finished Raid at the same center,
                // treat it as a new raid and reopen the key safely.
                if (!isRaidFinished(raid)) {
                    TERMINATED_RAID_KEYS.remove(key);
                } else {
                    continue;
                }
            }
            if (!visited.add(key)) {
                continue;
            }
            rememberRaid(level, raid, pos, key, gameTime);
        }
    }

    private static void rememberRaid(ServerLevel level, Object raid, Object fallbackPos, String key, long gameTime) {
        if (raid == null) {
            return;
        }
        String existingKey = key;
        ExtraWaveState existingState = STATES.get(existingKey);
        if (existingState != null && existingState.completed) {
            rememberTerminatedRaidKey(existingKey, gameTime);
            return;
        }
        if (existingState == null && isRaidFinished(raid)) {
            rememberTerminatedRaidKey(existingKey, gameTime);
            clearHudSnapshotByKey(existingKey);
            return;
        }
        int rawOmen = invokeInt(raid, RAID_OMEN_METHOD, 1);
        int omen = normalizedOmenLevel(rawOmen);
        String difficulty = runtimeDifficultyName(level);
        int authorityTarget = RaidWaveAuthority.targetTotalWaves(difficulty, omen);
        int nativeSafeMax = nativeSafeMaxWaves();
        int spawned = getGroupsSpawned(raid);
        int plannedTarget = authoritativeLogicalTarget(authorityTarget, spawned);

        // 0.4.20: create/maintain a state for every active raid, not only raids
        // whose total target exceeds the native-safe cap. The HUD is a user-facing
        // full-raid wave display, so Easy/Normal and non-extra-wave raids must also
        // publish a snapshot. extraWavesNeeded() remains zero for those raids, so
        // the custom spawn layer stays dormant.
        // 0.5.4: the display target must also include vanilla's native ominous bonus
        // wave. Otherwise combinations such as Easy + Omen III visibly run 5 waves
        // while the HUD remains stuck at 4/4. This is not a one-case patch: every
        // difficulty/omen combination is reconciled from Raid.numGroups, the native
        // ominous bonus setting, and the highest observed groupsSpawned value.
        ExtraWaveState state = existingState;
        boolean createdState = state == null;
        if (state == null) {
            int[] center = centerOf(raid, fallbackPos);
            state = new ExtraWaveState(key, dimensionId(level), center[0], center[1], center[2], difficulty, omen,
                    nativeSafeMax, plannedTarget, gameTime);
            state.nativeRaid = raid;
            state.observedNativeTargetWaves = Math.max(0, getNativeNumGroups(raid));
            applyPersistedLifecycleSnapshotIfFresh(state, gameTime);
            STATES.put(key, state);
            if (!announcedFirstSession) {
                announcedFirstSession = true;
                System.out.println("[Raid Enhancement Patch] Extra wave layer armed for a raid. Native safe cap="
                        + nativeSafeMax + ", planned target=" + authorityTarget
                        + ", authoritative HUD target=" + plannedTarget + ".");
            }
            RaidEncounterAuthority.debugDiscovery(key, difficulty, rawOmen, omen, authorityTarget,
                    RaidWaveAuthority.nativeWaveLimitForTarget(authorityTarget), state.observedNativeTargetWaves,
                    spawned, RaidWaveAuthority.customWaveCount(authorityTarget));
        } else {
            int[] center = centerOf(raid, fallbackPos);
            state.centerX = center[0];
            state.centerY = center[1];
            state.centerZ = center[2];
            // Step 8.5: keep the creation difficulty for this raid session stable.
            // Runtime world difficulty may change after the raid starts; the current
            // raid's planned total waves should not be downgraded by that change.
            if (state.difficultyName == null || state.difficultyName.isBlank()) {
                state.difficultyName = difficulty;
            }
            // 0.8.9.8.3: keep the raid's difficulty/omen/target stable after first discovery.
            // Later runtime Raid#getRaidOmenLevel observations are not reliable enough to
            // raise the HUD target or the bridge chain; only actual groupsSpawned can prove
            // that the native raid has already gone beyond the planned target.
            if (state.omenLevel <= 0) {
                state.omenLevel = omen;
            }
            state.nativeSafeWaves = nativeSafeMax;
            state.nativeRaid = raid;
            int stablePlanTarget = RaidWaveAuthority.targetTotalWaves(state.difficultyName, state.omenLevel);
            reconcileObservedWaveTarget(state, raid, stablePlanTarget, spawned);
            state.lastSeenGameTime = gameTime;
        }

        RaidKeyDiagnostics.logRaidDiscovery(createdState ? "created" : "updated", level, state.key, state.dimensionId,
                state.centerX, state.centerY, state.centerZ, raidIdForState(state), state.firstSeenGameTime, gameTime, state.nativeRaid);

        int stablePlanTarget = RaidWaveAuthority.targetTotalWaves(state.difficultyName, state.omenLevel);
        reconcileObservedWaveTarget(state, raid, stablePlanTarget, spawned);
        state.maxObservedNativeGroupsSpawned = Math.max(state.maxObservedNativeGroupsSpawned, spawned);
        boolean nativeRaidDone = isRaidFinished(raid);
        if (nativeRaidDone) {
            state.nativeRaidFinishedObserved = true;
            if (!state.armedForExtraWaves && state.customWavesSpawned <= 0) {
                if (state.hasCustomExtraWaves() && canBridgeFromFinishedNativeRaid(state)) {
                    if (hasActiveCombatPlayers(level, state) && bridgeReturnLoadGraceExpired(state, gameTime)) {
                        armExtraWaves(state, gameTime);
                    } else {
                        // 0.8.8.7: discovering a finished native raid while the player is
                        // away, or during the first ticks after returning, must not arm the
                        // custom layer. The native-final bridge is frozen until the player is
                        // present and nearby raiders have had time to reload.
                        freezeReturnSensitiveWaveState(state, gameTime);
                    }
                } else {
                    completeState(level, state, gameTime);
                    return;
                }
            }
        }
        publishHudSnapshot(state, gameTime);
        sendRaidWaveHudActionBar(level, state, gameTime);
        persistLifecycleSnapshot(state, gameTime, false);
        // 0.4.10: do not arm merely because vanilla reports victory. Prefer the
        // pre-victory path in processExtraWaveStates so custom raiders can join
        // the native raid before the victory state is broadcast. If we missed it,
        // processExtraWaveStates can still salvage the session after a short grace.
    }

    private static void processExtraWaveStates(ServerLevel level, long gameTime) {
        Iterator<Map.Entry<String, ExtraWaveState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            ExtraWaveState state = iterator.next().getValue();
            if (!state.dimensionId.equals(dimensionId(level))) {
                continue;
            }
            refreshNativeObservation(level, state, gameTime);
            forceNativeRaidOngoingIfBridgeActive(state);
            if (!state.completed) {
                updateBridgeHudHeartbeat(state, gameTime);
            }
            if (state.completed) {
                releaseManagedVillagerProtectionNearState(level, state);
                clearHudSnapshot(state);
                rememberTerminatedRaidKey(state.key, gameTime);
                if (gameTime - state.completedGameTime > 200L) {
                    iterator.remove();
                }
                continue;
            }
            if (state.extraWavesNeeded() <= 0
                    && state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves()
                    && nativeRaidGoneOrFinished(level, state)
                    && countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                    RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS) <= 0) {
                completeState(level, state, gameTime);
                continue;
            }
            if (state.hasCustomExtraWaves() && !state.armedForExtraWaves && state.customWavesSpawned <= 0 && state.nativeRaidFinishedObserved) {
                if (canBridgeFromFinishedNativeRaid(state)
                        && hasActiveCombatPlayers(level, state)
                        && bridgeReturnLoadGraceExpired(state, gameTime)
                        && countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                        RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS) <= 0) {
                    armExtraWaves(state, gameTime);
                } else if (!canBridgeFromFinishedNativeRaid(state)) {
                    completeState(level, state, gameTime);
                    continue;
                }
            }
            publishHudSnapshot(state, gameTime);
            sendRaidWaveHudActionBar(level, state, gameTime);

            // 0.8.8.2: observe/cache and update the active-combat clock before any
            // optional reinforcement logic. If all players have left long enough for
            // the clear clock to pause, the patch layer must not keep adding special
            // raiders or advance custom-wave transitions in the background.
            processKnownRaiderCache(level, state, gameTime);
            processWaveTimeBudget(level, state, gameTime);
            processRaiderGlowingLocator(level, state, gameTime);
            processVillageSecurity(level, state, gameTime);
            boolean combatLayerPaused = isCombatLayerPausedForState(state);
            boolean customLayerActive = state.armedForExtraWaves || state.customWavesSpawned > 0;
            boolean nativeBridgeWindow = nativeFinalBridgeWindowActive(state);
            boolean returnSensitiveWaveState = customLayerActive || nativeBridgeWindow;
            boolean hasCombatPlayerForReturnSensitiveLayer = hasActiveCombatPlayers(level, state);
            if (returnSensitiveWaveState && !hasCombatPlayerForReturnSensitiveLayer) {
                // 0.8.8.7: freeze both the custom layer and the native-final bridge
                // immediately when the player leaves. The timer can still spend the
                // normal 30s absence grace, but wave progression and special packs must
                // not use empty/unloaded scans while nobody is fighting.
                freezeReturnSensitiveWaveState(state, gameTime);
                protectVillagersNearState(level, state);
                publishHudSnapshot(state, gameTime);
                persistLifecycleSnapshot(state, gameTime, false);
                continue;
            }
            if (returnSensitiveWaveState && !bridgeReturnLoadGraceExpired(state, gameTime)) {
                // Give raiders/chunks a short time to reload after the player comes back
                // before evaluating "nearbyRaiders <= 0". This specifically protects
                // wave 8 -> 9 from being skipped on the first return tick.
                protectVillagersNearState(level, state);
                publishHudSnapshot(state, gameTime);
                persistLifecycleSnapshot(state, gameTime, false);
                continue;
            }
            if (combatLayerPaused) {
                publishHudSnapshot(state, gameTime);
                persistLifecycleSnapshot(state, gameTime, false);
                continue;
            }

            // 0.5.2: vanilla waves 1-8 remain generated by the native Raid.
            // Once a selected native wave is observed as spawned, add one-time
            // side reinforcement squads from extra attack points. This gives
            // native waves a multi-point pressure layer without replacing
            // Raid#spawnGroup or findRandomSpawnPos.
            spawnNativeWaveReinforcementsIfNeeded(level, state, gameTime);
            spawnRaidsEnhancedSpecialsOnNativeWaveIfNeeded(level, state, gameTime);

            // 0.4.9: keep villagers protected while custom waves are armed/active.
            // Vanilla may already have marked the raid as victory after the native-safe cap,
            // so the normal auto raid detector may release villagers too early.
            if (state.armedForExtraWaves || state.customWavesSpawned > 0) {
                protectVillagersNearState(level, state);
            }

            // 0.4.9: do not arm custom waves just because vanilla alive count briefly
            // reaches zero between native waves. Wait until the native raid is gone or
            // explicitly finished, otherwise custom waves can appear during wave 7.
            if (state.hasCustomExtraWaves()
                    && !state.armedForExtraWaves
                    && state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves()
                    && hasActiveCombatPlayers(level, state)
                    && bridgeReturnLoadGraceExpired(state, gameTime)
                    && countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                    RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS) <= 0) {
                boolean nativeStillActive = state.nativeRaid != null && !isRaidFinished(state.nativeRaid);
                boolean bridgeFinishedNativeRaid = !nativeStillActive && canBridgeFromFinishedNativeRaid(state);
                if (nativeStillActive || bridgeFinishedNativeRaid) {
                    armExtraWaves(state, gameTime);
                }
            }
            if (!state.armedForExtraWaves) {
                continue;
            }
            if (state.currentWaveActive) {
                int nearbyRaiders = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                        RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
                state.lastKnownNearbyRaiders = nearbyRaiders;
                if (gameTime - state.lastCustomWaveSpawnedGameTime >= RaidEnhancementConfig.EXTRA_WAVE_MIN_ACTIVE_TICKS
                        && nearbyRaiders <= 0) {
                    state.currentWaveActive = false;
                    if (state.customWavesSpawned >= customExtraWavesNeeded(state)) {
                        completeState(level, state, gameTime);
                    } else {
                        // 0.4.19: multi-extra-wave chains must not wait after wave 9.
                        // A delayed 9 -> 10 transition lets vanilla mark the native Raid as
                        // victory before the next custom wave is attached, producing visible
                        // "Raid - Victory" followed by inert late-spawned raiders. Chain the
                        // next logical wave in the same server tick, while victory is still
                        // suppressed by RaidVictorySuppressMixin.
                        state.nextActionGameTime = gameTime;
                        spawnNextCustomWave(level, state, gameTime);
                    }
                }
                continue;
            }
            if (gameTime >= state.nextActionGameTime && state.customWavesSpawned < customExtraWavesNeeded(state)) {
                spawnNextCustomWave(level, state, gameTime);
            }
        }
    }


    /**
     * 0.4.19: queried by RaidVictorySuppressMixin.
     *
     * Vanilla can set the native Raid to victory as soon as the currently joined
     * raider list becomes empty. That is safe for a single extra wave only if the
     * next custom wave has no follow-up. For Hard Omen IV/V we need to keep the
     * same Raid logically alive while waves 9 -> 10 -> 11 are chained, otherwise
     * later waves spawn after victory and do not receive reliable raid AI.
     */
    public static boolean shouldSuppressVictory(Object raid) {
        if (raid == null) {
            return false;
        }
        try {
            int[] raidCenter = centerOf(raid, null);
            String raidDimension = dimensionIdFromRaid(raid);
            for (ExtraWaveState state : STATES.values()) {
                if (state == null || state.completed) {
                    continue;
                }
                if (raidDimension != null && !raidDimension.equals(state.dimensionId)) {
                    continue;
                }
                boolean sameRaid = state.nativeRaid == raid
                        || (state.centerX == raidCenter[0] && state.centerY == raidCenter[1] && state.centerZ == raidCenter[2]);
                if (!sameRaid) {
                    continue;
                }
                if (state.extraWavesNeeded() <= 0) {
                    continue;
                }
                boolean nativeBridgeWindow = state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves()
                        || state.nativeRaidFinishedObserved;
                boolean customLayerActive = state.armedForExtraWaves
                        || state.currentWaveActive
                        || state.customWavesSpawned > 0
                        || state.bridgeHoldActive;
                if (nativeBridgeWindow || customLayerActive) {
                    int needed = customExtraWavesNeeded(state);
                    return state.currentWaveActive
                            || state.bridgeHoldActive
                            || state.customWavesSpawned < needed;
                }
            }
        } catch (Throwable ignored) {
            // Fail open. Never crash or permanently alter vanilla raid victory.
        }
        return false;
    }

    private static String dimensionIdFromRaid(Object raid) {
        Object level = invokeNoArg(raid, "getLevel");
        if (level instanceof ServerLevel serverLevel) {
            return dimensionId(serverLevel);
        }
        if (level == null) {
            return null;
        }
        try {
            Object dimension = invokeNoArg(level, "dimension");
            Object location = invokeNoArg(dimension, "location");
            return location == null ? null : location.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean nativeFinalBridgeWindowActive(ExtraWaveState state) {
        if (state == null || state.completed || state.extraWavesNeeded() <= 0) {
            return false;
        }
        if (state.armedForExtraWaves || state.customWavesSpawned > 0 || state.currentWaveActive) {
            return true;
        }
        return state.bridgeHoldActive
                || state.nativeRaidFinishedObserved
                || state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves();
    }

    private static void freezeReturnSensitiveWaveState(ExtraWaveState state, long gameTime) {
        if (state == null) {
            return;
        }
        long grace = Math.max(20L, EXTRA_WAVE_RETURN_LOAD_GRACE_TICKS);
        state.returnSensitiveFrozenUntilGameTime = Math.max(state.returnSensitiveFrozenUntilGameTime, gameTime + grace);
        if (nativeFinalBridgeWindowActive(state) && !state.armedForExtraWaves && state.customWavesSpawned <= 0) {
            state.bridgeHoldActive = true;
            if (state.bridgeHoldStartedGameTime <= 0L) {
                state.bridgeHoldStartedGameTime = gameTime;
            }
        }
    }

    private static boolean bridgeReturnLoadGraceExpired(ExtraWaveState state, long gameTime) {
        return state == null || state.returnSensitiveFrozenUntilGameTime <= 0L
                || gameTime >= state.returnSensitiveFrozenUntilGameTime;
    }

    private static void armExtraWaves(ExtraWaveState state, long gameTime) {
        if (state == null || state.armedForExtraWaves || state.completed || !state.hasCustomExtraWaves()) {
            return;
        }
        int plannedTarget = RaidWaveAuthority.targetTotalWaves(state.difficultyName, state.omenLevel);
        state.logicalTargetWaves = Math.max(1, plannedTarget);
        state.customBaseNativeWaves = RaidWaveAuthority.nativeWaveLimitForTarget(state.logicalTargetWaves);
        state.plannedCustomExtraWaves = RaidWaveAuthority.customWaveCount(state.logicalTargetWaves);
        if (state.plannedCustomExtraWaves <= 0) {
            return;
        }
        state.armedForExtraWaves = true;
        state.bridgeHoldActive = true;
        if (state.bridgeHoldStartedGameTime <= 0L) {
            state.bridgeHoldStartedGameTime = gameTime;
        }
        state.nextActionGameTime = gameTime + RaidEnhancementConfig.EXTRA_WAVE_FIRST_DELAY_TICKS;
    }

    private static void updateBridgeHudHeartbeat(ExtraWaveState state, long gameTime) {
        if (state == null || state.completed || state.customWavesSpawned > 0 || customExtraWavesNeeded(state) <= 0) {
            return;
        }
        if (state.maxObservedNativeGroupsSpawned < state.effectiveNativeWaves() && !state.nativeRaidFinishedObserved) {
            return;
        }
        if (!state.bridgeHoldActive) {
            state.bridgeHoldActive = true;
            state.bridgeHoldStartedGameTime = gameTime;
        }
        // Keep a fresh server-side HUD snapshot during the fragile vanilla-victory window.
        // The client stale-TTL should never be the reason the wave HUD disappears between
        // logical wave 8 and the custom wave 9 spawn.
        publishHudSnapshot(state, gameTime);
    }

    private static boolean canBridgeFromFinishedNativeRaid(ExtraWaveState state) {
        if (state == null || state.completed || state.armedForExtraWaves || state.customWavesSpawned > 0) {
            return false;
        }
        return customExtraWavesNeeded(state) > 0
                && (state.maxObservedNativeGroupsSpawned >= state.effectiveNativeWaves()
                || state.nativeRaidFinishedObserved);
    }

    private static void spawnNextCustomWave(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (state == null || state.completed || !state.hasCustomExtraWaves() || state.customWavesSpawned >= customExtraWavesNeeded(state)) {
            return;
        }
        if (!hasActiveCombatPlayers(level, state)) {
            // 0.8.8.7: never spawn/advance custom waves while the player is away.
            // The clear timer can still account for the absence grace in RaidSession,
            // but wave 9-11 spawning must be player-present only.
            publishHudSnapshot(state, gameTime);
            return;
        }
        forceNativeRaidOngoingIfBridgeActive(state);
        if (state.nativeRaid == null && !state.nativeRaidFinishedObserved) {
            completeState(level, state, gameTime);
            return;
        }
        if (state.customBaseNativeWaves <= 0) {
            state.customBaseNativeWaves = RaidWaveAuthority.nativeWaveLimitForTarget(state.logicalTargetWaves);
        }
        int logicalWave = customBaseNativeWaves(state) + state.customWavesSpawned + 1;
        if (logicalWave > Math.max(1, state.logicalTargetWaves)) {
            completeState(level, state, gameTime);
            return;
        }
        if (state.activeCustomLogicalWave == logicalWave && state.currentWaveActive) {
            return;
        }

        // 0.4.17: reserve the custom wave before spawning any entities.
        // In the user's modpack, some reflective spawn paths can visibly add raiders but
        // still report failure to this controller. 0.4.16 then rolled customWavesSpawned
        // back to 0 and retried forever, creating an infinite wave-9 spawn loop. By
        // reserving the logical wave first, one planned extra wave can only be attempted
        // once unless no raider appears at all.
        int nearbyBefore = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
        state.activeCustomLogicalWave = logicalWave;
        state.customWavesSpawned++;
        state.currentWaveActive = true;
        state.lastCustomWaveSpawnedGameTime = gameTime;
        state.nextActionGameTime = Long.MAX_VALUE / 4L;
        publishHudSnapshot(state, gameTime);

        int remainingIncludingThis = state.logicalTargetWaves - logicalWave + 1;
        List<String> mainAttackIds = compositionFor(state, logicalWave, remainingIncludingThis <= 1);
        prepareSpawnAnchors(level, state, logicalWave);
        int spawned = spawnCustomWaveWithMainAndReinforcements(level, state, logicalWave, mainAttackIds);
        spawned += spawnRaidsEnhancedSpecialsForCustomWaveIfNeeded(level, state, logicalWave, gameTime);
        int nearbyAfter = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
        boolean physicalRaidersAppeared = nearbyAfter > nearbyBefore;
        if (spawned > 0 || physicalRaidersAppeared) {
            int observedSpawned = spawned > 0 ? spawned : Math.max(1, nearbyAfter - nearbyBefore);
            state.bridgeHoldActive = false;
            state.failedSpawnRetries = 0;
            state.currentWaveActive = true;
            state.lastKnownNearbyRaiders = Math.max(Math.max(observedSpawned, nearbyAfter), state.lastKnownNearbyRaiders);
            publishHudSnapshot(state, gameTime);
            return;
        }

        // No visible raider was added; only then is it safe to roll the reservation back
        // and retry with a different focused anchor. This preserves the 0.4.16 HUD bridge
        // without allowing duplicated wave-9 packs.
        state.customWavesSpawned = Math.max(0, state.customWavesSpawned - 1);
        state.activeCustomLogicalWave = -1;
        state.currentWaveActive = false;
        state.bridgeHoldActive = true;
        if (state.bridgeHoldStartedGameTime <= 0L) {
            state.bridgeHoldStartedGameTime = gameTime;
        }
        state.failedSpawnRetries++;
        state.spawnAnchorWave = -1;
        state.nextActionGameTime = gameTime + Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_FAILED_SPAWN_RETRY_DELAY_TICKS);
        publishHudSnapshot(state, gameTime);
        if (!warnedCommandFailure) {
            warnedCommandFailure = true;
            System.out.println("[Raid Enhancement Patch] Extra wave summon produced no visible raiders once; keeping bridge HUD alive and retrying with a fresh anchor.");
        }
        if (state.failedSpawnRetries > Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_FAILED_SPAWN_MAX_RETRIES)
                && gameTime - state.bridgeHoldStartedGameTime > Math.max(20, RaidEnhancementConfig.EXTRA_WAVE_BRIDGE_HUD_HOLD_TICKS)) {
            completeState(level, state, gameTime);
        }
    }



    private static int customBaseNativeWaves(ExtraWaveState state) {
        if (state == null) {
            return 1;
        }
        return RaidWaveAuthority.nativeWaveLimitForTarget(state.logicalTargetWaves);
    }

    private static int customExtraWavesNeeded(ExtraWaveState state) {
        if (state == null || !state.hasCustomExtraWaves()) {
            return 0;
        }
        return RaidWaveAuthority.customWaveCount(state.logicalTargetWaves);
    }

    private static boolean hasPendingExtraWaveChain(ExtraWaveState state) {
        if (state == null || state.completed || state.extraWavesNeeded() <= 0) {
            return false;
        }
        int needed = customExtraWavesNeeded(state);
        if (needed <= 0) {
            return false;
        }
        return state.bridgeHoldActive
                || state.armedForExtraWaves
                || state.currentWaveActive
                || state.customWavesSpawned < needed;
    }

    private static void forceNativeRaidOngoingIfBridgeActive(ExtraWaveState state) {
        if (state == null || state.nativeRaid == null || !hasPendingExtraWaveChain(state)) {
            return;
        }
        try {
            Field statusField = findField(state.nativeRaid.getClass(), RAID_STATUS_FIELD);
            if (statusField == null) {
                statusField = findRaidStatusField(state.nativeRaid.getClass());
            }
            if (statusField == null) {
                return;
            }
            statusField.setAccessible(true);
            Object current = statusField.get(state.nativeRaid);
            if (!(current instanceof Enum<?> currentStatus)) {
                return;
            }
            String name = currentStatus.name();
            if (!"VICTORY".equalsIgnoreCase(name)) {
                return;
            }
            Object ongoing = enumConstant(currentStatus.getDeclaringClass(), "ONGOING");
            if (ongoing != null) {
                statusField.set(state.nativeRaid, ongoing);
            }
        } catch (Throwable ignored) {
            // Best-effort bridge repair. The victory-suppression mixin remains the primary guard.
        }
    }

    private static Field findRaidStatusField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!fieldType.isEnum()) {
                    continue;
                }
                Object victory = enumConstant(fieldType, "VICTORY");
                Object ongoing = enumConstant(fieldType, "ONGOING");
                if (victory != null && ongoing != null) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }



    private enum OptionalReinforcementDecision {
        OPEN,
        WAIT,
        LOCK_CLOSED
    }


    /**
     * Step 8.1: known-raider runtime cache.
     *
     * This is deliberately a data layer only. It observes and records raider UUIDs,
     * type ids, logical wave, category and last known position. It does not teleport,
     * clear, fail, damage, retime, or directly mutate the native Raid object.
     */
    private static void processKnownRaiderCache(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.KNOWN_RAIDER_CACHE_ENABLED) {
            return;
        }
        try {
            int wave = currentLogicalWaveForGlowing(state);
            if (wave <= 0) {
                return;
            }
            ensureKnownRaiderSession(state, gameTime, wave, displayTotalWaves(state));

            int interval = RaidEnhancementConfig.VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED
                    ? Math.max(5, RaidEnhancementConfig.PERFORMANCE_RAIDER_CACHE_SCAN_INTERVAL_TICKS)
                    : Math.max(1, RaidEnhancementConfig.KNOWN_RAIDER_CACHE_SCAN_INTERVAL_TICKS);
            if (RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ENABLED
                    && !hasActiveCombatPlayers(level, state)) {
                // Step 8.8: when every player has left the battlefield, keep the
                // cache alive at a much lower cadence. This avoids expensive
                // background scans while the clear timer is paused.
                interval = Math.max(interval,
                        RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_NO_PLAYER_CACHE_SCAN_INTERVAL_TICKS);
            }
            if (state.lastKnownRaiderCacheScanGameTime > 0L
                    && gameTime - state.lastKnownRaiderCacheScanGameTime < interval) {
                return;
            }
            state.lastKnownRaiderCacheScanGameTime = gameTime;

            int radius = Math.max(RaidEnhancementConfig.KNOWN_RAIDER_CACHE_SCAN_RADIUS,
                    Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS, RaidEnhancementConfig.RAIDER_GLOWING_SCAN_RADIUS));
            List<Entity> raiders = nearbyRaiders(level, state.centerX, state.centerY, state.centerZ, radius);
            for (Entity raider : raiders) {
                RaiderCategory category = isRaidsEnhancedSpecialEntityCached(state, raider, gameTime)
                        ? RaiderCategory.RAIDS_ENHANCED_SPECIAL
                        : RaiderCategory.VANILLA_MAIN_POINT;
                rememberKnownRaiderFromEntity(state, raider, category, wave, gameTime);
            }

            long oldestAllowed = gameTime - Math.max(200L, RaidEnhancementConfig.KNOWN_RAIDER_CACHE_STALE_RECORD_TTL_TICKS);
            RaidSessionManager.get(state.key).ifPresent(session -> {
                session.removeRaidersNotSeenSince(oldestAllowed);
                state.lastKnownRaiderCacheCount = session.trackedRaiderCount();
            });
            RaidSessionManager.cleanupInactiveSessions(gameTime);
            if (!announcedKnownRaiderCache) {
                announcedKnownRaiderCache = true;
                System.out.println("[Raid Enhancement Patch] Step 8.1 known-raider cache is active. It records raider UUIDs only; timeout, teleport, clearing and hard failure are disabled.");
            }
        } catch (Throwable throwable) {
            if (!warnedKnownRaiderCacheFailure) {
                warnedKnownRaiderCacheFailure = true;
                System.out.println("[Raid Enhancement Patch] Known-raider cache failed once and was suppressed: " + throwable);
            }
        }
    }

    private static void ensureKnownRaiderSession(ExtraWaveState state, long gameTime, int wave, int totalWaves) {
        if (state == null || !RaidEnhancementConfig.KNOWN_RAIDER_CACHE_ENABLED) {
            return;
        }
        int resolvedWave = clamp(wave, 1, Math.max(1, totalWaves));

        Optional<RaidSession> existingSession = RaidSessionManager.get(state.key);
        if (existingSession.isPresent()) {
            RaidSession session = existingSession.get();
            // 0.8.8.2: ExtraWaveState can be re-discovered after the player leaves
            // the battlefield and later returns. If the RaidSession already owns this
            // wave, do not call beginWave again: that would reset the locked clear
            // budget and make the timer appear to be re-calculated on return.
            if (session.currentWave() == resolvedWave) {
                state.knownRaiderCacheWave = resolvedWave;
                return;
            }
            // Never downgrade an already-started higher logical wave because a stale
            // vanilla observation briefly reports the bridge/native wave again.
            if (session.currentWave() > resolvedWave && session.currentWaveClearStarted()) {
                state.knownRaiderCacheWave = session.currentWave();
                return;
            }

            if (state.knownRaiderCacheWave == resolvedWave) {
                return;
            }
        } else if (state.knownRaiderCacheWave == resolvedWave) {
            // 0.8.8.3: lifecycle metadata can restore state.knownRaiderCacheWave,
            // but RaidSession itself is runtime-only. If we return here with no
            // session, the top HUD can show only the wave text and lose the clear
            // timer. Re-create the session, and reserve optional reinforcement locks
            // for this already-known wave so returning/reloading cannot add another
            // late special pack or side pack to the same in-progress wave.
            state.nativeReinforcedWaves.add(resolvedWave);
            state.specialReinforcedWaves.add(resolvedWave);
        }

        state.knownRaiderCacheWave = resolvedWave;
        resetTimeoutRaiderAuditState(state);
        RaidSessionHooks.onWavePlanned(state.key, state.dimensionId, raidIdForState(state), gameTime,
                resolvedWave, Math.max(resolvedWave, totalWaves), RaidDifficulty.fromName(state.difficultyName),
                state.omenLevel, cacheAttackPointsFor(state, resolvedWave));
    }

    private static List<AttackPoint> cacheAttackPointsFor(ExtraWaveState state, int wave) {
        if (state == null) {
            return List.of();
        }
        List<AttackPoint> points = new ArrayList<>();
        if (state.spawnAnchorWave == wave && state.spawnAnchors != null && state.spawnAnchors.length > 0) {
            int planned = Math.max(0, state.lastKnownNearbyRaiders);
            for (int i = 0; i < state.spawnAnchors.length; i++) {
                int[] anchor = state.spawnAnchors[i];
                if (anchor == null || anchor.length < 3) {
                    continue;
                }
                points.add(i == 0
                        ? AttackPoint.main(anchor[0], anchor[1], anchor[2], planned)
                        : AttackPoint.side(anchor[0], anchor[1], anchor[2], 0));
            }
        }
        if (points.isEmpty()) {
            points.add(AttackPoint.main(state.centerX, state.centerY, state.centerZ, Math.max(0, state.lastKnownNearbyRaiders)));
        }
        return points;
    }

    private static OptionalReinforcementDecision optionalReinforcementDecision(ServerLevel level, ExtraWaveState state,
                                                                               int logicalWave, long gameTime) {
        if (state == null || logicalWave <= 0) {
            return OptionalReinforcementDecision.LOCK_CLOSED;
        }
        if (state.nativeRaidFinishedObserved || state.completed) {
            return OptionalReinforcementDecision.LOCK_CLOSED;
        }
        boolean hasCombatPlayer = hasActiveCombatPlayers(level, state);
        Optional<RaidSession> session = RaidSessionManager.get(state.key);
        if (session.isEmpty()) {
            // No runtime timer means this is either pre-start discovery or lifecycle recovery.
            // Pre-start discovery should wait; a known/restored wave must be reserved closed
            // so returning/reloading cannot add optional packs to an already-running battle.
            return state.knownRaiderCacheWave == logicalWave || state.restoredFromLifecycleSnapshot
                    ? OptionalReinforcementDecision.LOCK_CLOSED
                    : OptionalReinforcementDecision.WAIT;
        }
        RaidSession raidSession = session.get();
        if (raidSession.currentWave() != logicalWave) {
            return raidSession.currentWaveClearStarted() || state.knownRaiderCacheWave == logicalWave
                    ? OptionalReinforcementDecision.LOCK_CLOSED
                    : OptionalReinforcementDecision.WAIT;
        }
        if (!hasCombatPlayer || raidSession.currentWaveClockPausedAfterGrace()) {
            // The player has left this combat layer. Optional reinforcements are startup
            // pressure only; absence/return must not be interpreted as a fresh wave.
            return state.knownRaiderCacheWave == logicalWave || raidSession.currentWaveClearStarted()
                    ? OptionalReinforcementDecision.LOCK_CLOSED
                    : OptionalReinforcementDecision.WAIT;
        }
        if (!raidSession.currentWaveClearStarted()) {
            // Wait until the known-raider cache has observed at least one live raider and
            // started the clear clock. This prevents optional packs from being spawned
            // just because a Raid object was re-observed while the wave body is not ready.
            return OptionalReinforcementDecision.WAIT;
        }
        if (raidSession.currentWaveBudgetLocked()) {
            return OptionalReinforcementDecision.LOCK_CLOSED;
        }
        return raidSession.currentWaveBudgetCollecting(gameTime)
                ? OptionalReinforcementDecision.OPEN
                : OptionalReinforcementDecision.WAIT;
    }


    private static boolean rememberKnownRaiderFromEntity(ExtraWaveState state, Entity entity,
                                                         RaiderCategory category, int logicalWave, long gameTime) {
        if (state == null || entity == null || !RaidEnhancementConfig.KNOWN_RAIDER_CACHE_ENABLED) {
            return false;
        }
        UUID uuid = entity.getUUID();
        if (uuid == null) {
            return false;
        }
        int wave = clamp(logicalWave <= 0 ? currentLogicalWaveForGlowing(state) : logicalWave,
                1, Math.max(1, displayTotalWaves(state)));
        ensureKnownRaiderSession(state, gameTime, wave, displayTotalWaves(state));
        int[] pos = entityBlockPosition(entity, state.centerX, state.centerY, state.centerZ);
        RaiderCategory resolvedCategory = isRaidsEnhancedSpecialEntityCached(state, entity, gameTime)
                ? RaiderCategory.RAIDS_ENHANCED_SPECIAL
                : (category == null ? RaiderCategory.VANILLA_MAIN_POINT : category);
        String entityId = entityTypeId(entity);
        boolean raidCaptain = isRaidCaptain(entity);
        int timeWeightSeconds = RaiderTimeWeights.weightSeconds(entityId, resolvedCategory, raidCaptain);
        return RaidSessionHooks.onRaiderObserved(state.key, state.dimensionId, raidIdForState(state), gameTime,
                uuid, entityId, resolvedCategory, wave, pos[0], pos[1], pos[2], entity.isAlive(),
                timeWeightSeconds, raidCaptain);
    }

    /**
     * Step 8.2: dynamic wave clear budget.
     *
     * This is intentionally non-destructive. A timeout only marks the RaidSession
     * and optionally sends a one-time chat warning. Failure finalization remains
     * disabled by WAVE_TIME_BUDGET_ENFORCE_FAILURE=false.
     */
    private static void processWaveTimeBudget(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.WAVE_TIME_BUDGET_ENABLED) {
            return;
        }
        try {
            int interval = Math.max(1, RaidEnhancementConfig.WAVE_TIME_BUDGET_RECALCULATE_INTERVAL_TICKS);
            if (state.lastWaveTimeBudgetProcessGameTime > 0L
                    && gameTime - state.lastWaveTimeBudgetProcessGameTime < interval) {
                return;
            }
            state.lastWaveTimeBudgetProcessGameTime = gameTime;
            RaidSessionManager.get(state.key).ifPresent(session -> {
                int activeCombatPlayers = countActiveCombatPlayers(level, state);
                boolean hasActiveCombatPlayers = activeCombatPlayers > 0;
                boolean presenceChanged = session.updateActiveCombatPresence(gameTime, hasActiveCombatPlayers, activeCombatPlayers);

                boolean budgetChanged = session.refreshWaveTimeBudget(gameTime) || presenceChanged;
                state.lastWaveBudgetSeconds = session.currentWaveBudgetSeconds();
                state.lastWaveRaiderWeightSeconds = session.currentWaveRaiderWeightSeconds();
                state.lastWaveTimeoutGameTime = session.currentWaveTimeoutGameTime();
                state.activeCombatPlayerCount = activeCombatPlayers;
                state.clearClockPaused = session.currentWaveClockPausedAfterGrace();

                processWaveTimeDisplay(level, state, session, gameTime, budgetChanged);
                publishHudSnapshot(state, gameTime);

                if (session.currentWaveClockPausedAfterGrace()) {
                    // Step 8.8: no active participants after the absence grace window.
                    // Keep the HUD/session alive, but do not advance timeout state and
                    // do not run the expensive timeout audit/glowing loops.
                    return;
                }

                if (session.updateWaveTimeoutState(gameTime)) {
                    state.waveTimeoutWarnings.add(session.currentWave());
                    if (RaidEnhancementConfig.WAVE_TIME_BUDGET_TIMEOUT_CHAT_WARNING_ENABLED) {
                        sendRaidMessage(level, state, RaidEnhancementConfig.WAVE_TIME_BUDGET_TIMEOUT_MESSAGE);
                    }
                    session.markWaveTimeoutWarningSent(gameTime);
                    processWaveTimeDisplay(level, state, session, gameTime, false);
                    publishHudSnapshot(state, gameTime);
                    System.out.println("[Raid Enhancement Patch] Wave clear budget timed out in "
                            + session.shortSummary() + ". This 0.8.8 build records, displays, audits, and gates the active combat clock only; failure enforcement is disabled.");
                }
                if (session.currentWaveTimedOut()) {
                    processTimeoutRaiderAudit(level, state, session, gameTime);
                }
            });
            if (!announcedWaveTimeBudget) {
                announcedWaveTimeBudget = true;
                System.out.println("[Raid Enhancement Patch] Step 8.2 dynamic wave timeout budget is active. Raider time weights are recorded; hard failure, teleport and clearing remain disabled.");
            }
            if (!announcedWaveTimeDisplay) {
                announcedWaveTimeDisplay = true;
                System.out.println("[Raid Enhancement Patch] Step 8.6 raid HUD/session lifecycle is active. Clear timer is merged into the top HUD, stale/range snapshots are ignored, and failure enforcement remains disabled.");
            }
            if (!announcedActiveCombatClock) {
                announcedActiveCombatClock = true;
                System.out.println("[Raid Enhancement Patch] Step 8.8 active-combat clock is active. Clear timers and timeout audits pause after all players leave the battlefield for the configured grace window.");
            }
        } catch (Throwable throwable) {
            if (!warnedWaveTimeBudgetFailure) {
                warnedWaveTimeBudgetFailure = true;
                System.out.println("[Raid Enhancement Patch] Wave time budget failed once and was suppressed: " + throwable);
            }
        }
    }


    private static void processVillageSecurity(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.VILLAGE_SECURITY_ENABLED) {
            return;
        }
        int wave = currentLogicalWaveForGlowing(state);
        int total = displayTotalWaves(state);
        boolean activePlayer = hasActiveCombatPlayers(level, state);
        VillageSecurityController.tick(level, state.key, state.dimensionId, state.centerX, state.centerY, state.centerZ,
                state.difficultyName, wave, total, gameTime, activePlayer);
    }

    private static boolean isCombatLayerPausedForState(ExtraWaveState state) {
        if (state == null || !RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ENABLED) {
            return false;
        }
        Optional<RaidSession> session = RaidSessionManager.get(state.key);
        return session.isPresent() && session.get().currentWaveClockPausedAfterGrace();
    }

    /**
     * Step 8.7: timeout raider audit.
     *
     * This layer runs only after a wave has already entered timeout monitoring.
     * It audits known remaining raiders at low frequency and classifies them for
     * later recovery/failure stages. It never teleports, clears, kills, fails, or
     * mutates vanilla Raid state.
     */
    private static void processTimeoutRaiderAudit(ServerLevel level, ExtraWaveState state, RaidSession session, long gameTime) {
        if (level == null || state == null || session == null || state.completed
                || !RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_ENABLED
                || !session.currentWaveTimedOut()) {
            return;
        }
        try {
            if (RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ENABLED
                    && session.currentWaveClockPausedAfterGrace()) {
                return;
            }
            int interval = RaidEnhancementConfig.VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED
                    ? Math.max(20, RaidEnhancementConfig.PERFORMANCE_RAIDER_AUDIT_INTERVAL_TICKS)
                    : Math.max(1, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_INTERVAL_TICKS);
            if (state.lastTimeoutRaiderAuditGameTime > 0L
                    && gameTime - state.lastTimeoutRaiderAuditGameTime < interval) {
                return;
            }
            state.lastTimeoutRaiderAuditGameTime = gameTime;

            TimeoutAuditCounts counts = new TimeoutAuditCounts();
            int normalRadius = Math.max(16, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_NORMAL_RADIUS);
            int farRadius = Math.max(normalRadius + 1, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_FAR_RADIUS);
            long normalRadiusSquared = (long) normalRadius * (long) normalRadius;
            long missingGraceTicks = Math.max(0L, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_MISSING_GRACE_TICKS);
            boolean shouldGlow = state.lastTimeoutRaiderAuditGlowGameTime <= 0L
                    || gameTime - state.lastTimeoutRaiderAuditGlowGameTime
                    >= Math.max(1, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_GLOW_INTERVAL_TICKS);
            if (shouldGlow) {
                state.lastTimeoutRaiderAuditGlowGameTime = gameTime;
            }

            Set<UUID> stillTracked = new HashSet<>();
            for (RaiderRecord record : session.trackedRaiders()) {
                if (record == null || record.entityUuid() == null || record.waveIndex() != session.currentWave()) {
                    continue;
                }
                stillTracked.add(record.entityUuid());
                Entity entity = entityByUuid(level, record.entityUuid());
                if (entity == null || !entity.isAlive()) {
                    // A freshly unseen raider may simply be between cache refreshes or in an
                    // unloaded chunk. Keep it non-destructive and label it missing only after
                    // a short grace window.
                    if (gameTime - record.lastSeenGameTime() >= missingGraceTicks) {
                        counts.missing++;
                        counts.remaining++;
                    }
                    continue;
                }

                int[] pos = entityBlockPosition(entity, record.lastKnownX(), record.lastKnownY(), record.lastKnownZ());
                boolean special = record.isSpecial() || isRaidsEnhancedSpecialEntityCached(state, entity, gameTime);
                if (special) {
                    counts.special++;
                }
                counts.located++;
                counts.remaining++;
                long distanceSquared = distanceSquared(pos[0], pos[1], pos[2], state.centerX, state.centerY, state.centerZ);
                if (distanceSquared > normalRadiusSquared) {
                    counts.far++;
                } else if (isSuspectedStuck(state, record.entityUuid(), pos[0], pos[1], pos[2], gameTime)) {
                    counts.stuck++;
                } else {
                    counts.normal++;
                }

                if (shouldGlow) {
                    if (special) {
                        applySpecialRaiderGlowing(entity, state);
                    } else {
                        applyLowCountRaiderGlowing(entity);
                    }
                }
            }
            state.timeoutAuditMovementRecords.keySet().removeIf(uuid -> !stillTracked.contains(uuid));
            session.updateTimeoutAuditSummary(gameTime, counts.remaining, counts.normal, counts.far,
                    counts.stuck, counts.missing, counts.special, counts.located);

            boolean shouldMessage = state.lastTimeoutRaiderAuditMessageGameTime <= 0L
                    || gameTime - state.lastTimeoutRaiderAuditMessageGameTime
                    >= Math.max(1, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_MESSAGE_INTERVAL_TICKS);
            if (shouldMessage) {
                state.lastTimeoutRaiderAuditMessageGameTime = gameTime;
                sendRaidMessage(level, state, timeoutAuditMessage(counts));
            }
            if (!announcedTimeoutRaiderAudit) {
                announcedTimeoutRaiderAudit = true;
                System.out.println("[Raid Enhancement Patch] Step 8.7 timeout raider audit is active. It classifies remaining raiders and re-applies locator glowing; teleport, clearing and failure remain disabled.");
            }
        } catch (Throwable throwable) {
            if (!warnedTimeoutRaiderAuditFailure) {
                warnedTimeoutRaiderAuditFailure = true;
                System.out.println("[Raid Enhancement Patch] Timeout raider audit failed once and was suppressed: " + throwable);
            }
        }
    }

    private static Entity entityByUuid(ServerLevel level, UUID uuid) {
        if (level == null || uuid == null) {
            return null;
        }
        try {
            for (Method method : level.getClass().getMethods()) {
                if (!method.getName().equals("getEntity") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (!parameter.isAssignableFrom(UUID.class) && !UUID.class.isAssignableFrom(parameter)) {
                    continue;
                }
                method.setAccessible(true);
                Object result = method.invoke(level, uuid);
                return result instanceof Entity entity ? entity : null;
            }
        } catch (Throwable ignored) {
            // UUID lookup is optional. Missing means suspected-missing after grace.
        }
        return null;
    }

    private static boolean isSuspectedStuck(ExtraWaveState state, UUID uuid, int x, int y, int z, long gameTime) {
        if (state == null || uuid == null) {
            return false;
        }
        TimeoutAuditMovementRecord movement = state.timeoutAuditMovementRecords.get(uuid);
        if (movement == null) {
            state.timeoutAuditMovementRecords.put(uuid, new TimeoutAuditMovementRecord(x, y, z, gameTime));
            return false;
        }
        int threshold = Math.max(0, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_STUCK_MOVEMENT_THRESHOLD_BLOCKS);
        long thresholdSquared = (long) threshold * (long) threshold;
        long movedSquared = distanceSquared(x, y, z, movement.lastX, movement.lastY, movement.lastZ);
        if (movedSquared > thresholdSquared) {
            movement.lastX = x;
            movement.lastY = y;
            movement.lastZ = z;
            movement.stillSinceGameTime = gameTime;
            movement.lastAuditGameTime = gameTime;
            return false;
        }
        movement.lastAuditGameTime = gameTime;
        return gameTime - movement.stillSinceGameTime
                >= Math.max(1L, RaidEnhancementConfig.TIMEOUT_RAIDER_AUDIT_STUCK_STILL_TICKS);
    }

    private static void resetTimeoutRaiderAuditState(ExtraWaveState state) {
        if (state == null) {
            return;
        }
        state.lastTimeoutRaiderAuditGameTime = 0L;
        state.lastTimeoutRaiderAuditGlowGameTime = 0L;
        state.lastTimeoutRaiderAuditMessageGameTime = 0L;
        state.timeoutAuditMovementRecords.clear();
    }

    private static String timeoutAuditMessage(TimeoutAuditCounts counts) {
        if (counts == null || counts.remaining <= 0) {
            return "超时监控：缓存中暂未确认剩余袭击者，等待袭击状态刷新。";
        }
        StringBuilder builder = new StringBuilder("超时监控：剩余袭击者 ")
                .append(counts.remaining)
                .append(" 个，正常 ")
                .append(counts.normal)
                .append("，疑似远离 ")
                .append(counts.far)
                .append("，疑似卡怪 ")
                .append(counts.stuck)
                .append("，疑似失联 ")
                .append(counts.missing);
        if (counts.special > 0) {
            builder.append("，特殊 ").append(counts.special);
        }
        builder.append("；已重新标记可定位目标。");
        return builder.toString();
    }


    /**
     * Step 8.4: visible wave clear timer display.
     *
     * This method only sends UI information. It never fails raids, moves raiders,
     * clears entities, damages villagers, or mutates vanilla Raid state.
     */
    private static void processWaveTimeDisplay(ServerLevel level, ExtraWaveState state, RaidSession session,
                                               long gameTime, boolean budgetChanged) {
        if (level == null || state == null || session == null || state.completed
                || !RaidEnhancementConfig.WAVE_TIME_DISPLAY_ENABLED
                || session.currentWave() <= 0 || session.currentWaveBudgetSeconds() <= 0) {
            return;
        }
        try {
            int wave = session.currentWave();
            int total = Math.max(1, session.totalWaves());
            int budgetSeconds = Math.max(0, session.currentWaveBudgetSeconds());
            boolean budgetLocked = session.currentWaveBudgetLocked();
            if (!budgetLocked) {
                // Step 8.4: during the short collection window, the top HUD shows
                // "confirming raid size". Do not send final budget chat or countdown
                // warnings until the initial budget has been locked.
                return;
            }
            int remainingSeconds = session.currentWaveRemainingSeconds(gameTime);
            boolean timedOut = session.currentWaveTimedOut() || remainingSeconds <= 0 && gameTime > session.currentWaveTimeoutGameTime();
            boolean clockPaused = session.currentWaveClockPausedAfterGrace();

            if (clockPaused) {
                // In normal play the top HUD is hidden when the player is outside
                // display range. If a player is in a transition edge case, avoid
                // sending warnings or start-chat while the active-combat clock is paused.
                return;
            }

            long startDelay = Math.max(0, RaidEnhancementConfig.WAVE_TIME_DISPLAY_WAVE_START_DELAY_TICKS);
            if (RaidEnhancementConfig.WAVE_TIME_DISPLAY_WAVE_START_CHAT_ENABLED
                    && gameTime - session.waveStartedGameTime() >= startDelay
                    && state.waveTimeDisplayMessageLocks.add("start:" + wave + ":" + session.currentWaveBudgetLockGameTime())) {
                String message = "第 " + wave + " 波清剿时限：基础 "
                        + formatChineseDuration(session.currentWaveBaseBudgetSeconds())
                        + "，袭击者加时 " + formatChineseDuration(session.currentWaveRaiderWeightSeconds())
                        + "，附属点加时 " + formatChineseDuration(session.currentWaveSidePointSeconds())
                        + "，总攻加时 " + formatChineseDuration(session.currentWaveExtraWaveSeconds())
                        + "，锁定后加时上限 " + formatChineseDuration(session.currentWavePostLockExtraCapSeconds())
                        + "，总时限 " + formatChineseDuration(budgetSeconds) + "。";
                sendRaidMessage(level, state, message);
            }

            if (RaidEnhancementConfig.WAVE_TIME_DISPLAY_WARNING_CHAT_ENABLED && !timedOut) {
                for (int threshold : RaidEnhancementConfig.WAVE_TIME_DISPLAY_WARNING_THRESHOLDS_SECONDS) {
                    if (threshold <= 0 || remainingSeconds <= 0 || remainingSeconds > threshold) {
                        continue;
                    }
                    String key = "warning:" + wave + ":" + threshold;
                    if (state.waveTimeDisplayMessageLocks.add(key)) {
                        sendRaidMessage(level, state, "警告：当前波次清剿剩余 " + threshold + " 秒。");
                    }
                }
            }

            if (RaidEnhancementConfig.WAVE_TIME_DISPLAY_ACTIONBAR_ENABLED) {
                int interval = Math.max(1, RaidEnhancementConfig.WAVE_TIME_DISPLAY_ACTIONBAR_INTERVAL_TICKS);
                if (budgetChanged || state.lastWaveTimeDisplayActionbarGameTime <= 0L
                        || gameTime - state.lastWaveTimeDisplayActionbarGameTime >= interval) {
                    state.lastWaveTimeDisplayActionbarGameTime = gameTime;
                    String remainingText = timedOut ? "超时" : formatClockDuration(remainingSeconds);
                    String text = "袭击清剿 第 " + wave + " / " + total + " 波｜剩余 " + remainingText
                            + "｜总时限 " + formatClockDuration(budgetSeconds);
                    if (timedOut) {
                        text += "｜超时监控";
                    }
                    sendRaidActionBar(level, state, text);
                }
            }
        } catch (Throwable throwable) {
            if (!warnedWaveTimeDisplayFailure) {
                warnedWaveTimeDisplayFailure = true;
                System.out.println("[Raid Enhancement Patch] Wave time display failed once and was suppressed: " + throwable);
            }
        }
    }

    private static String formatClockDuration(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int minutes = safeSeconds / 60;
        int remainder = safeSeconds % 60;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, remainder);
    }

    private static String formatChineseDuration(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int minutes = safeSeconds / 60;
        int remainder = safeSeconds % 60;
        if (minutes <= 0) {
            return remainder + "秒";
        }
        if (remainder <= 0) {
            return minutes + "分";
        }
        return minutes + "分" + remainder + "秒";
    }

    private static boolean isRaidCaptain(Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            for (String methodName : List.of("isPatrolLeader", "isCaptain", "isRaidCaptain")) {
                try {
                    Method method = entity.getClass().getMethod(methodName);
                    if (method.getParameterCount() == 0
                            && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                        method.setAccessible(true);
                        Object value = method.invoke(entity);
                        if (Boolean.TRUE.equals(value)) {
                            return true;
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the next optional name.
                }
            }
        } catch (Throwable ignored) {
            // Captain bonus is optional; never break cache observation.
        }
        return false;
    }

    private static int[] entityBlockPosition(Entity entity, int fallbackX, int fallbackY, int fallbackZ) {
        Object pos = invokeNoArg(entity, BLOCK_POSITION_METHOD);
        if (pos == null) {
            return new int[]{fallbackX, fallbackY, fallbackZ};
        }
        return new int[]{
                invokeInt(pos, "getX", fallbackX),
                invokeInt(pos, "getY", fallbackY),
                invokeInt(pos, "getZ", fallbackZ)
        };
    }

    private static int raidIdForState(ExtraWaveState state) {
        return state == null || state.key == null ? 0 : state.key.hashCode();
    }


    /**
     * Step 7: raider glowing locator.
     *
     * Low-count glow is only a short locator effect for ordinary remaining raiders.
     * Raids Enhanced special raiders are high-priority targets and always keep the
     * long 30-minute glow; the 30-second low-count effect must never shorten it.
     */
    private static void processRaiderGlowingLocator(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.RAIDER_GLOWING_LOCATOR_ENABLED) {
            return;
        }
        if (!hasActiveCombatPlayers(level, state)) {
            // 0.8.8.1: Do not run the one-shot low-count locator while all players
            // are away from the raid battlefield. In 0.8.8 the scan could still run
            // against loaded raiders, consume the per-wave low-count lock, and make
            // the real "残兵已被锁定" trigger unavailable after the player returns.
            // The active-combat gate preserves the proven 0.8.8 HUD/event chain and
            // only prevents off-battlefield background scans from spending the lock.
            return;
        }
        int locatorInterval = RaidEnhancementConfig.VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED
                ? Math.max(Math.max(20, RaidEnhancementConfig.PERFORMANCE_STRAGGLER_GLOW_INTERVAL_TICKS),
                        Math.max(20, RaidEnhancementConfig.PERFORMANCE_SPECIAL_RAIDER_SCAN_INTERVAL_TICKS))
                : Math.max(1, RaidEnhancementConfig.WAVE_EXPANSION_REFRESH_INTERVAL_TICKS);
        if (state.lastRaiderGlowingLocatorCheckGameTime > 0L
                && gameTime - state.lastRaiderGlowingLocatorCheckGameTime < locatorInterval) {
            return;
        }
        state.lastRaiderGlowingLocatorCheckGameTime = gameTime;
        try {
            List<Entity> raiders = nearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                    Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS, RaidEnhancementConfig.RAIDER_GLOWING_SCAN_RADIUS));
            if (raiders.isEmpty()) {
                return;
            }

            // Mark Raids Enhanced native specials even when they were not spawned by this patch.
            // No chat message is sent for this long special-raider glow.
            for (Entity raider : raiders) {
                if (isRaidsEnhancedSpecialEntityCached(state, raider, gameTime)) {
                    applySpecialRaiderGlowing(raider, state);
                }
            }

            int aliveCount = 0;
            for (Entity raider : raiders) {
                if (raider != null && raider.isAlive()) {
                    aliveCount++;
                }
            }
            int threshold = Math.max(1, RaidEnhancementConfig.RAIDER_GLOWING_LOW_COUNT_THRESHOLD);
            if (aliveCount <= 0 || aliveCount > threshold) {
                return;
            }
            int wave = currentLogicalWaveForGlowing(state);
            if (wave <= 0 || state.lowCountGlowingWaves.contains(wave)) {
                return;
            }
            state.lowCountGlowingWaves.add(wave);
            for (Entity raider : raiders) {
                if (raider == null || !raider.isAlive()) {
                    continue;
                }
                if (isRaidsEnhancedSpecialEntityCached(state, raider, gameTime)) {
                    applySpecialRaiderGlowing(raider, state);
                } else {
                    applyLowCountRaiderGlowing(raider);
                }
            }
            sendRaidMessage(level, state, RaidEnhancementConfig.RAIDER_GLOWING_LOW_COUNT_MESSAGE);
        } catch (Throwable throwable) {
            if (!warnedGlowingLocatorFailure) {
                warnedGlowingLocatorFailure = true;
                System.out.println("[Raid Enhancement Patch] Raider glowing locator failed once and was suppressed: " + throwable);
            }
        }
    }

    private static int currentLogicalWaveForGlowing(ExtraWaveState state) {
        if (state == null) {
            return 0;
        }
        int total = displayTotalWaves(state);
        int wave;
        if (state.activeCustomLogicalWave > 0 && (state.currentWaveActive || state.customWavesSpawned > 0)) {
            wave = state.activeCustomLogicalWave;
        } else if (state.customWavesSpawned > 0) {
            wave = customBaseNativeWaves(state) + state.customWavesSpawned;
        } else if (state.bridgeHoldActive || state.armedForExtraWaves || state.nativeRaidFinishedObserved) {
            wave = Math.max(state.effectiveNativeWaves(), state.maxObservedNativeGroupsSpawned);
        } else {
            wave = Math.max(1, state.maxObservedNativeGroupsSpawned);
        }
        return clamp(wave, 1, Math.max(1, total));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Entity> nearbyRaiders(ServerLevel level, int x, int y, int z, int radius) {
        try {
            Class<?> raiderClass = Class.forName(RAIDER_CLASS_NAME);
            Class<?> aabbClass = Class.forName(AABB_CLASS_NAME);
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                    double.class, double.class, double.class);
            int r = Math.max(1, radius);
            Object box = constructor.newInstance(x - r, y - r, z - r, x + r, y + r, z + r);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, aabbClass);
            Object result = getEntities.invoke(level, (Class) raiderClass, box);
            if (!(result instanceof List<?> list)) {
                return List.of();
            }
            List<Entity> raiders = new ArrayList<>();
            for (Object candidate : list) {
                if (candidate instanceof Entity entity && entity.isAlive()) {
                    raiders.add(entity);
                }
            }
            return raiders;
        } catch (Throwable throwable) {
            if (!warnedScanFailure) {
                warnedScanFailure = true;
                System.out.println("[Raid Enhancement Patch] Raider glowing locator scan failed once: " + throwable);
            }
            return List.of();
        }
    }

    private static void applyLowCountRaiderGlowing(Object entity) {
        MobEffectCompat.addEffect(entity, MobEffectCompat.GLOWING_NAMES,
                Math.max(1, RaidEnhancementConfig.RAIDER_GLOWING_LOW_COUNT_DURATION_TICKS), 0);
    }

    private static void applySpecialRaiderGlowing(Object entity, ExtraWaveState state) {
        if (entity == null || !RaidEnhancementConfig.RAIDER_GLOWING_LOCATOR_ENABLED) {
            return;
        }
        UUID uuid = entity instanceof Entity e ? e.getUUID() : null;
        if (state != null && uuid != null && state.longGlowingSpecialEntityIds.contains(uuid)) {
            return;
        }
        MobEffectCompat.addEffect(entity, MobEffectCompat.GLOWING_NAMES,
                Math.max(RaidEnhancementConfig.RAIDER_GLOWING_SPECIAL_DURATION_TICKS,
                        RaidEnhancementConfig.RAIDER_GLOWING_LOW_COUNT_DURATION_TICKS), 0);
        if (state != null && uuid != null) {
            state.longGlowingSpecialEntityIds.add(uuid);
        }
    }

    private static boolean isRaidsEnhancedSpecialEntityCached(ExtraWaveState state, Object entity, long gameTime) {
        if (entity == null) {
            return false;
        }
        UUID uuid = entity instanceof Entity e ? e.getUUID() : null;
        if (state == null || uuid == null) {
            return isRaidsEnhancedSpecialEntity(entity);
        }
        Boolean cached = state.specialRaiderIdentityCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        boolean special = isRaidsEnhancedSpecialEntity(entity);
        if (state.specialRaiderIdentityCache.size() > 512) {
            state.specialRaiderIdentityCache.clear();
        }
        state.specialRaiderIdentityCache.put(uuid, special);
        state.lastSpecialRaiderIdentityScanGameTime = Math.max(state.lastSpecialRaiderIdentityScanGameTime, gameTime);
        return special;
    }

    private static boolean isRaidsEnhancedSpecialEntity(Object entity) {
        String id = entityTypeId(entity);
        if (RaidsEnhancedIds.isSpecialRaiderId(id)) {
            return true;
        }
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName();
        return className.contains("raids_enhanced.content.entities.raid_drill")
                || className.contains("raids_enhanced.content.entities.golem_of_last_resort")
                || className.contains("raids_enhanced.content.entities.engineer.ZapperIllager")
                || className.contains("raids_enhanced.content.entities.raid_blimp");
    }

    private static String entityTypeId(Object entity) {
        if (entity == null) {
            return null;
        }
        Object encodeId = invokeNoArg(entity, "getEncodeId");
        if (encodeId instanceof String value && !value.isBlank()) {
            return value;
        }
        Object type = invokeNoArg(entity, "getType");
        if (type == null) {
            return null;
        }
        try {
            Object registry = staticField("net.minecraft.core.registries.BuiltInRegistries", "ENTITY_TYPE");
            if (registry == null) {
                return null;
            }
            for (Method method : registry.getClass().getMethods()) {
                if (!method.getName().equals("getKey") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (!parameter.isAssignableFrom(type.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                Object key = method.invoke(registry, type);
                return key == null ? null : key.toString();
            }
        } catch (Throwable ignored) {
            // Optional mapping compatibility path.
        }
        return null;
    }


    private static boolean hasActiveCombatPlayers(ServerLevel level, ExtraWaveState state) {
        return countActiveCombatPlayers(level, state) > 0;
    }

    private static int countActiveCombatPlayers(ServerLevel level, ExtraWaveState state) {
        if (level == null || state == null || !RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_ENABLED) {
            return 1;
        }
        int radius = Math.max(RaidEnhancementConfig.RAID_WAVE_HUD_CLEAR_RADIUS,
                RaidEnhancementConfig.ACTIVE_COMBAT_CLOCK_RADIUS);
        long radiusSquared = (long) radius * (long) radius;
        int count = 0;
        for (Object playerObject : playersSnapshot(level)) {
            if (!(playerObject instanceof Entity player) || !player.isAlive() || isSpectatorPlayer(player)) {
                continue;
            }
            int[] pos = entityBlockPosition(player, state.centerX, state.centerY, state.centerZ);
            if (distanceSquared(pos[0], pos[1], pos[2], state.centerX, state.centerY, state.centerZ) <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSpectatorPlayer(Entity player) {
        if (player == null) {
            return false;
        }
        return invokeBoolean(player, "isSpectator");
    }

    private static void sendRaidActionBar(ServerLevel level, ExtraWaveState state, String message) {
        if (level == null || state == null || message == null || message.isBlank()) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            Object component = literal.invoke(null, message);
            int radius = Math.max(64, RaidEnhancementConfig.WAVE_TIME_DISPLAY_RADIUS);
            long radiusSquared = (long) radius * (long) radius;
            for (Object playerObject : playersSnapshot(level)) {
                if (!(playerObject instanceof Entity player) || !player.isAlive()) {
                    continue;
                }
                int px = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getX", state.centerX);
                int py = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getY", state.centerY);
                int pz = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getZ", state.centerZ);
                long dx = (long) px - state.centerX;
                long dy = (long) py - state.centerY;
                long dz = (long) pz - state.centerZ;
                if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                    continue;
                }
                for (Method method : player.getClass().getMethods()) {
                    if (!method.getName().equals("displayClientMessage") || method.getParameterCount() != 2) {
                        continue;
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (!parameterTypes[0].isAssignableFrom(componentClass)
                            || !(parameterTypes[1] == boolean.class || parameterTypes[1] == Boolean.class)) {
                        continue;
                    }
                    method.setAccessible(true);
                    method.invoke(player, component, true);
                    break;
                }
            }
        } catch (Throwable ignored) {
            // Actionbar display is optional and must never affect raid logic.
        }
    }

    private static void sendRaidMessage(ServerLevel level, ExtraWaveState state, String message) {
        if (level == null || state == null || message == null || message.isBlank()) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            Object component = literal.invoke(null, message);
            int radius = Math.max(64, RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS);
            long radiusSquared = (long) radius * (long) radius;
            for (Object playerObject : playersSnapshot(level)) {
                if (!(playerObject instanceof Entity player) || !player.isAlive()) {
                    continue;
                }
                int px = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getX", state.centerX);
                int py = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getY", state.centerY);
                int pz = invokeInt(invokeNoArg(player, BLOCK_POSITION_METHOD), "getZ", state.centerZ);
                long dx = (long) px - state.centerX;
                long dy = (long) py - state.centerY;
                long dz = (long) pz - state.centerZ;
                if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                    continue;
                }
                for (Method method : player.getClass().getMethods()) {
                    if (!method.getName().equals("sendSystemMessage") || method.getParameterCount() != 1) {
                        continue;
                    }
                    if (!method.getParameterTypes()[0].isAssignableFrom(componentClass)) {
                        continue;
                    }
                    method.setAccessible(true);
                    method.invoke(player, component);
                    break;
                }
            }
        } catch (Throwable ignored) {
            // Chat hint is optional; the glow effect itself is the important behavior.
        }
    }

    /**
     * Step 6 native-wave hook for Raids Enhanced special reinforcements.
     *
     * This deliberately runs beside Raids Enhanced's own RaidMixin instead of
     * cancelling or moving it. Easy adds none. Normal adds one random special
     * once per raid near the native-special wave. Hard adds wave-locked special
     * packs starting at wave 7.
     */
    private static void spawnRaidsEnhancedSpecialsOnNativeWaveIfNeeded(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.RAIDS_ENHANCED_SPECIALS_ENABLED) {
            return;
        }
        if (state.nativeRaid == null || isRaidFinished(state.nativeRaid)) {
            return;
        }
        if (state.armedForExtraWaves || state.customWavesSpawned > 0 || state.nativeRaidFinishedObserved) {
            return;
        }
        String difficulty = difficultyKey(state.difficultyName);
        if (difficulty.contains("EASY")) {
            return;
        }
        int nativeWave = clamp(state.maxObservedNativeGroupsSpawned, 1, displayTotalWaves(state));
        if (nativeWave <= 0 || nativeWave > Math.min(8, displayTotalWaves(state))) {
            return;
        }
        OptionalReinforcementDecision specialDecision = optionalReinforcementDecision(level, state, nativeWave, gameTime);
        if (specialDecision == OptionalReinforcementDecision.WAIT) {
            return;
        }
        if (specialDecision == OptionalReinforcementDecision.LOCK_CLOSED) {
            state.specialReinforcedWaves.add(nativeWave);
            return;
        }
        int nearbyRaiders = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
        if (nearbyRaiders <= 0) {
            // Do not create stale special reinforcements after the wave is already cleared.
            return;
        }
        if (difficulty.contains("HARD")) {
            int count = hardRaidsEnhancedSpecialCount(nativeWave);
            if (count > 0) {
                spawnRaidsEnhancedSpecialPack(level, state, nativeWave, count, true, gameTime);
            }
            return;
        }
        if (state.normalRaidsEnhancedSpecialDone) {
            return;
        }
        int triggerWave = normalRaidsEnhancedSpecialTriggerWave(state);
        if (nativeWave >= triggerWave) {
            int count = Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_NORMAL_EXTRA_PER_RAID);
            int spawned = spawnRaidsEnhancedSpecialPack(level, state, nativeWave, count, false, gameTime);
            // Mark even on partial/failure so an absent optional dependency or blocked
            // spawn cannot retry every tick in Normal raids.
            state.normalRaidsEnhancedSpecialDone = true;
            if (spawned > 0) {
                publishHudSnapshot(state, gameTime);
            }
        }
    }

    /** Adds hard-mode Raids Enhanced special packs to custom logical waves 9/10/11. */
    private static int spawnRaidsEnhancedSpecialsForCustomWaveIfNeeded(ServerLevel level, ExtraWaveState state, int logicalWave, long gameTime) {
        if (level == null || state == null || state.completed || !RaidEnhancementConfig.RAIDS_ENHANCED_SPECIALS_ENABLED) {
            return 0;
        }
        if (!difficultyKey(state.difficultyName).contains("HARD")) {
            return 0;
        }
        int count = hardRaidsEnhancedSpecialCount(logicalWave);
        if (count <= 0) {
            return 0;
        }
        return spawnRaidsEnhancedSpecialPack(level, state, logicalWave, count, true, gameTime);
    }

    private static int spawnRaidsEnhancedSpecialPack(ServerLevel level, ExtraWaveState state, int logicalWave,
                                                     int requestedCount, boolean hardMode, long gameTime) {
        if (requestedCount <= 0 || state == null || state.specialReinforcedWaves.contains(logicalWave)) {
            return 0;
        }
        int existingSpecials = countNearbyRaidsEnhancedSpecialRaiders(level, state,
                Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS, RaidEnhancementConfig.RAIDER_GLOWING_SCAN_RADIUS));
        int effectiveCount = Math.max(0, requestedCount - Math.max(0, existingSpecials));
        if (effectiveCount <= 0) {
            // 0.8.8.3 conservative cap: count already-existing Raids Enhanced special
            // raiders against this wave's requested pack. Re-discovery or return from
            // a pause must never add a full second pack on top of native specials.
            state.specialReinforcedWaves.add(logicalWave);
            return 0;
        }
        requestedCount = effectiveCount;
        // Reserve before spawning. The user allows duplicate special raider types in the
        // same wave, but the trigger itself must never repeat every tick.
        state.specialReinforcedWaves.add(logicalWave);
        int[][] anchors = chooseRaidsEnhancedSpecialAnchors(level, state, logicalWave, requestedCount);
        int blimpCap = raidsEnhancedBlimpCapForWave(logicalWave, hardMode);
        int blimpsSpawned = 0;
        int spawned = 0;
        for (int i = 0; i < requestedCount; i++) {
            String entityId = chooseRaidsEnhancedSpecialId(state, logicalWave, i, hardMode, blimpsSpawned < blimpCap);
            if (RaidsEnhancedIds.RAID_BLIMP.equals(entityId)) {
                blimpsSpawned++;
            }
            int[] anchor = anchors[Math.floorMod(i, anchors.length)];
            int[] pos = spawnPositionAtSpecificAnchor(anchor, i / Math.max(1, anchors.length));
            if (spawnRaidsEnhancedSpecialEntity(level, entityId, pos[0], pos[1], pos[2], state, logicalWave)) {
                spawned++;
            }
        }
        if (spawned > 0) {
            state.lastKnownNearbyRaiders = Math.max(state.lastKnownNearbyRaiders,
                    countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                            RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS));
            publishHudSnapshot(state, gameTime);
        }
        return spawned;
    }

    private static int countNearbyRaidsEnhancedSpecialRaiders(ServerLevel level, ExtraWaveState state, int radius) {
        if (level == null || state == null) {
            return 0;
        }
        int count = 0;
        try {
            for (Entity raider : nearbyRaiders(level, state.centerX, state.centerY, state.centerZ, Math.max(1, radius))) {
                if (raider != null && raider.isAlive() && isRaidsEnhancedSpecialEntityCached(state, raider, level.getGameTime())) {
                    count++;
                }
            }
        } catch (Throwable ignored) {
            return 0;
        }
        return count;
    }

    private static int normalRaidsEnhancedSpecialTriggerWave(ExtraWaveState state) {
        int total = Math.max(1, displayTotalWaves(state));
        int nativeNumGroups = state == null || state.nativeRaid == null ? 0 : getNativeNumGroups(state.nativeRaid);
        if (state != null && state.omenLevel <= 1) {
            // Raids Enhanced normally does not add a special for Omen I, so place the
            // user's extra special on the same practical late-raid slot: one before last.
            return clamp(total - 1, 1, Math.min(8, total));
        }
        if (nativeNumGroups > 0) {
            return clamp(nativeNumGroups, 1, Math.min(8, total));
        }
        return clamp(total - 1, 1, Math.min(8, total));
    }

    private static int hardRaidsEnhancedSpecialCount(int logicalWave) {
        return switch (logicalWave) {
            case 7 -> Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_HARD_WAVE_7_COUNT);
            case 8 -> Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_HARD_WAVE_8_COUNT);
            case 9 -> Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_HARD_WAVE_9_COUNT);
            case 10 -> Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_HARD_WAVE_10_COUNT);
            case 11 -> Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_HARD_WAVE_11_COUNT);
            default -> 0;
        };
    }

    private static int raidsEnhancedBlimpCapForWave(int logicalWave, boolean hardMode) {
        if (!hardMode) {
            return logicalWave >= 8 ? 1 : 0;
        }
        if (logicalWave >= 9) {
            return Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_BLIMP_CAP_WAVE_9_TO_11);
        }
        return Math.max(0, RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_BLIMP_CAP_WAVE_7_TO_8);
    }

    private static String chooseRaidsEnhancedSpecialId(ExtraWaveState state, int logicalWave, int index,
                                                       boolean hardMode, boolean allowBlimp) {
        List<String> pool = new ArrayList<>();
        pool.add(RaidsEnhancedIds.RAID_DRILL);
        pool.add(RaidsEnhancedIds.GOLEM_OF_LAST_RESORT);
        pool.add(RaidsEnhancedIds.ZAPPER);
        if (allowBlimp && (hardMode || (state != null && state.omenLevel >= 5))) {
            pool.add(RaidsEnhancedIds.RAID_BLIMP);
        }
        int seed = Math.abs((state == null || state.key == null ? 0 : state.key.hashCode())
                + logicalWave * 131 + index * 41 + (state == null ? 0 : state.omenLevel * 17));
        return pool.get(Math.floorMod(seed, pool.size()));
    }

    private static int[][] chooseRaidsEnhancedSpecialAnchors(ServerLevel level, ExtraWaveState state, int logicalWave, int desired) {
        int count = Math.max(1, Math.min(Math.max(1, desired), Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MAX_POINTS)));
        List<int[]> anchors = new ArrayList<>();
        if (state != null && state.spawnAnchorWave == logicalWave && state.spawnAnchors != null && state.spawnAnchors.length > 0) {
            for (int[] anchor : state.spawnAnchors) {
                addAnchorIfDistinct(anchors, anchor, count);
            }
        }
        if (anchors.size() < count && state != null && state.nativeRaid != null && !isRaidFinished(state.nativeRaid)) {
            for (int[] nativeAnchor : findNativeRaidSpawnAnchors(state, Math.max(1, Math.min(logicalWave, state.effectiveNativeWaves())), count + 2)) {
                addAnchorIfDistinct(anchors, nativeAnchor, count);
            }
        }
        if (anchors.size() < count) {
            for (int[] fallback : fallbackNativeReinforcementAnchors(state, logicalWave, count)) {
                addAnchorIfDistinct(anchors, fallback, count);
            }
        }
        if (anchors.isEmpty()) {
            anchors.add(fallbackFocusedSpawnAnchor(state, logicalWave));
        }
        while (anchors.size() < count) {
            int[] base = anchors.get(anchors.size() - 1);
            int step = Math.max(12, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MIN_ANCHOR_DISTANCE + anchors.size() * 4);
            anchors.add(new int[]{base[0] + step, base[1], base[2] - step});
        }
        return anchors.toArray(new int[0][]);
    }

    private static boolean spawnRaidsEnhancedSpecialEntity(ServerLevel level, String entityId, int x, int y, int z,
                                                           ExtraWaveState state, int logicalWave) {
        if (entityId == null) {
            return false;
        }
        if (RaidsEnhancedIds.RAID_BLIMP.equals(entityId) && RaidEnhancementConfig.RAIDS_ENHANCED_SPECIAL_BLIMP_NATIVE_INIT) {
            return spawnRaidsEnhancedBlimp(level, x, y, z, state, logicalWave);
        }
        return summon(level, entityId, x, y, z, state, logicalWave, RaiderCategory.RAIDS_ENHANCED_SPECIAL);
    }

    private static boolean spawnRaidsEnhancedBlimp(ServerLevel level, int x, int y, int z, ExtraWaveState state, int logicalWave) {
        if (level == null || state == null || state.nativeRaid == null) {
            return false;
        }
        try {
            Object created = createEntityFromId(RaidsEnhancedIds.RAID_BLIMP, level);
            if (!(created instanceof Entity entity)) {
                return false;
            }
            int skyY = Math.max(y + 12, motionBlockingHeight(level, x, z) + 25);
            if (RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_VALIDATION_ENABLED) {
                SafeRaidSpawnResolver.Resolution resolution = SafeRaidSpawnResolver.resolveAir(level, entity, x, skyY, z);
                if (!resolution.found()) {
                    reportSafeSpawnFailure(resolution);
                    return false;
                }
                if (!setBlimpVec3Position(entity, resolution.x(), resolution.y(), resolution.z())) {
                    moveEntity(entity, resolution.x(), resolution.y(), resolution.z());
                }
            } else if (!setBlimpVec3Position(entity, x + 0.5D, skyY, z + 0.5D)) {
                moveEntity(entity, x + 0.5D, skyY, z + 0.5D);
            }
            int raidWave = Math.max(1, Math.min(Math.max(1, logicalWave), Math.max(1, state.effectiveNativeWaves())));
            invokeOptional(entity, "setCurrentRaid", state.nativeRaid);
            invokeOptional(entity, "setWave", raidWave);
            invokeOptional(entity, "setCanJoinRaid", Boolean.TRUE);
            invokeOptional(entity, "setTicksOutsideRaid", 0);
            boolean added = addFreshEntity(level, entity);
            if (added) {
                addWaveMobIfPossible(state.nativeRaid, raidWave, entity, true);
                applySpecialRaiderGlowing(entity, state);
                rememberKnownRaiderFromEntity(state, entity, RaiderCategory.RAIDS_ENHANCED_SPECIAL,
                        logicalWave, level.getGameTime());
            }
            return added;
        } catch (Throwable throwable) {
            if (!warnedRaidsEnhancedBlimpFailure) {
                warnedRaidsEnhancedBlimpFailure = true;
                System.out.println("[Raid Enhancement Patch] Raids Enhanced blimp special spawn failed once: " + throwable);
            }
            return false;
        }
    }

    private static Object createEntityFromId(String entityId, ServerLevel level) throws ReflectiveOperationException {
        Class<?> entityTypeClass = CachedReflection.findClass("net.minecraft.world.entity.EntityType");
        Method byString = CachedReflection.findMethod(entityTypeClass, "byString", entityId);
        if (byString == null) {
            return null;
        }
        Object optional = byString.invoke(null, entityId);
        Object entityType = null;
        if (optional instanceof Optional<?> opt && opt.isPresent()) {
            entityType = opt.get();
        }
        return entityType == null ? null : createEntity(entityType, level);
    }

    private static int motionBlockingHeight(ServerLevel level, int x, int z) {
        try {
            Class<?> heightmapTypesClass = Class.forName("net.minecraft.world.level.levelgen.Heightmap$Types");
            Object motionBlocking = Enum.valueOf((Class<Enum>) heightmapTypesClass.asSubclass(Enum.class), "MOTION_BLOCKING");
            for (Method method : level.getClass().getMethods()) {
                if (!method.getName().equals("getHeight") || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(motionBlocking.getClass())) {
                    continue;
                }
                Object result = method.invoke(level, motionBlocking, x, z);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Throwable ignored) {
            // Fall back to the supplied anchor Y below.
        }
        return 64;
    }

    private static boolean setBlimpVec3Position(Object entity, double x, double y, double z) {
        try {
            Class<?> vec3Class = CachedReflection.findClass("net.minecraft.world.phys.Vec3");
            Object vec3 = CachedReflection.construct(vec3Class, x, y, z);
            Method method = CachedReflection.findMethod(entity.getClass(), "setPos", vec3);
            if (method == null) {
                return false;
            }
            method.invoke(entity, vec3);
            return true;
        } catch (Throwable ignored) {
            // Fall back to Entity#setPos/moveTo.
            return false;
        }
    }

    private static void addWaveMobIfPossible(Object raid, int logicalWave, Object entity, boolean flag) {
        if (raid == null || entity == null) {
            return;
        }
        try {
            for (Method method : raid.getClass().getMethods()) {
                if (!method.getName().equals("addWaveMob") || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if ((types[0] == int.class || types[0] == Integer.TYPE)
                        && types[1].isAssignableFrom(entity.getClass())
                        && (types[2] == boolean.class || types[2] == Boolean.TYPE)) {
                    method.setAccessible(true);
                    method.invoke(raid, Math.max(1, logicalWave), entity, flag);
                    return;
                }
            }
        } catch (Throwable throwable) {
            if (!warnedRaidsEnhancedSpecialFailure) {
                warnedRaidsEnhancedSpecialFailure = true;
                System.out.println("[Raid Enhancement Patch] Raids Enhanced addWaveMob failed once; special may still exist but may not count correctly: " + throwable);
            }
        }
    }

    private static void spawnNativeWaveReinforcementsIfNeeded(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed
                || !RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_POINTS_ENABLED) {
            return;
        }
        if (state.nativeRaid == null || isRaidFinished(state.nativeRaid)) {
            return;
        }
        if (state.armedForExtraWaves || state.customWavesSpawned > 0 || state.nativeRaidFinishedObserved) {
            return;
        }
        int nativeWave = clamp(state.maxObservedNativeGroupsSpawned, 1, state.effectiveNativeWaves());
        if (nativeWave < RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_MIN_WAVE
                || nativeWave > RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_MAX_WAVE
                || nativeWave > state.effectiveNativeWaves()) {
            return;
        }
        OptionalReinforcementDecision nativeDecision = optionalReinforcementDecision(level, state, nativeWave, gameTime);
        if (nativeDecision == OptionalReinforcementDecision.WAIT) {
            return;
        }
        if (nativeDecision == OptionalReinforcementDecision.LOCK_CLOSED) {
            state.nativeReinforcedWaves.add(nativeWave);
            return;
        }
        if (state.nativeReinforcedWaves.contains(nativeWave)) {
            return;
        }
        int points = nativeReinforcementPointCount(state, nativeWave);
        int squadSize = nativeReinforcementSquadSize(nativeWave);
        if (points <= 0 || squadSize <= 0) {
            state.nativeReinforcedWaves.add(nativeWave);
            return;
        }
        int nearbyRaiders = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
        if (nearbyRaiders <= 0) {
            // Do not create stale reinforcements after a native wave is already cleared.
            return;
        }

        // Reserve before spawning, matching the 0.4.17 custom-wave safety rule.
        // If reflection reports a partial failure but raiders visibly appeared, this
        // native wave still cannot be re-added every tick.
        state.nativeReinforcedWaves.add(nativeWave);

        int spawned = 0;
        int[][] anchors = chooseNativeReinforcementAnchors(level, state, nativeWave, points);
        int maxMobs = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_MAX_MOBS_PER_WAVE);
        for (int anchorIndex = 0; anchorIndex < anchors.length && spawned < maxMobs; anchorIndex++) {
            List<String> ids = nativeReinforcementCompositionFor(nativeWave, squadSize);
            for (int localIndex = 0; localIndex < ids.size() && spawned < maxMobs; localIndex++) {
                int[] pos = spawnPositionAtSpecificAnchor(anchors[anchorIndex], localIndex);
                if (summon(level, ids.get(localIndex), pos[0], pos[1], pos[2], state, nativeWave,
                        RaiderCategory.VANILLA_SIDE_POINT)) {
                    spawned++;
                }
            }
        }
        if (spawned > 0) {
            state.lastKnownNearbyRaiders = Math.max(state.lastKnownNearbyRaiders,
                    countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                            RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS));
            publishHudSnapshot(state, gameTime);
        }
    }

    private static int nativeReinforcementPointCount(ExtraWaveState state, int nativeWave) {
        if (state == null || nativeWave <= 0) {
            return 0;
        }
        String difficulty = difficultyKey(state.difficultyName);
        int total = Math.max(1, state.logicalTargetWaves);
        int omen = clamp(state.omenLevel, 1, 5);
        if (difficulty.contains("EASY")) {
            return Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_EASY_MAX_POINTS);
        }
        if (difficulty.contains("HARD")) {
            int omenCap = hardSecondaryPointCapForOmen(omen);
            int configCap = Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_HARD_MAX_POINTS);
            int max = Math.min(configCap, omenCap);
            return hardSecondaryPointCount(nativeWave, max);
        }
        int max = Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_NORMAL_MAX_POINTS);
        return normalSecondaryPointCount(nativeWave, total, max);
    }

    private static int nativeReinforcementSquadSize(int nativeWave) {
        if (nativeWave <= 3) {
            return Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_WAVE_1_TO_3_SIZE);
        }
        if (nativeWave <= 6) {
            return Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_WAVE_4_TO_6_SIZE);
        }
        return Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_WAVE_7_TO_8_SIZE);
    }

    private static List<String> nativeReinforcementCompositionFor(int nativeWave, int squadSize) {
        List<String> ids = new ArrayList<>();
        int size = Math.max(1, squadSize);
        int vindicators = nativeWave >= 7 ? 2 : 1;
        if (size <= 3) {
            vindicators = 1;
        }
        int pillagers = Math.max(1, size - vindicators);
        add(ids, "minecraft:pillager", pillagers);
        add(ids, "minecraft:vindicator", Math.min(vindicators, Math.max(0, size - pillagers)));
        while (ids.size() < size) {
            ids.add("minecraft:pillager");
        }
        return ids;
    }

    private static int[][] chooseNativeReinforcementAnchors(ServerLevel level, ExtraWaveState state, int nativeWave, int desired) {
        int count = Math.max(1, desired);
        List<int[]> anchors = new ArrayList<>();
        if (RaidEnhancementConfig.EXTRA_WAVE_PREFER_NATIVE_SPAWN_ANCHOR) {
            for (int[] nativeAnchor : findNativeRaidSpawnAnchors(state, nativeWave, count + 2)) {
                addAnchorIfDistinct(anchors, nativeAnchor, count);
            }
        }
        if (anchors.size() < count) {
            for (int[] fallback : fallbackNativeReinforcementAnchors(state, nativeWave, count)) {
                addAnchorIfDistinct(anchors, fallback, count);
            }
        }
        if (anchors.isEmpty()) {
            anchors.add(fallbackFocusedSpawnAnchor(state, nativeWave));
        }
        while (anchors.size() < count) {
            int[] base = anchors.get(anchors.size() - 1);
            int step = Math.max(12, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MIN_ANCHOR_DISTANCE + anchors.size() * 4);
            anchors.add(new int[]{base[0] - step, base[1], base[2] + step});
        }
        return anchors.toArray(new int[0][]);
    }

    private static List<int[]> fallbackNativeReinforcementAnchors(ExtraWaveState state, int nativeWave, int desired) {
        List<int[]> anchors = new ArrayList<>();
        int radius = Math.max(20, RaidEnhancementConfig.EXTRA_WAVE_SPAWN_ANCHOR_RADIUS - 8);
        int[][] offsets = {
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius},
                {radius, radius}, {-radius, -radius}, {radius, -radius}, {-radius, radius}
        };
        int seed = Math.abs((state.key == null ? 0 : state.key.hashCode()) + nativeWave * 97 + 17);
        for (int i = 0; i < offsets.length && anchors.size() < desired; i++) {
            int[] offset = offsets[Math.floorMod(seed + i, offsets.length)];
            anchors.add(new int[]{state.centerX + offset[0], state.centerY + 1, state.centerZ + offset[1]});
        }
        return anchors;
    }

    private static List<String> compositionFor(ExtraWaveState state, int logicalWave, boolean finalCustomWave) {
        int omen = clamp(state.omenLevel, 1, 5);
        int difficultyFactor = difficultyFactor(state.difficultyName);
        int extraIndex = Math.max(1, logicalWave - state.nativeSafeWaves);
        int pillagers = 3 + omen + difficultyFactor + extraIndex;
        int vindicators = 2 + difficultyFactor + (extraIndex / 2);
        int witches = logicalWave >= 8 ? 1 : 0;
        if (difficultyFactor >= 2 && logicalWave >= 9) {
            witches++;
        }
        int evokers = logicalWave >= 8 ? 1 : 0;
        if (difficultyFactor >= 2 && finalCustomWave && omen >= 4) {
            evokers++;
        }
        int ravagers = (logicalWave >= 8 && (finalCustomWave || difficultyFactor >= 2)) ? 1 : 0;
        if (difficultyFactor >= 2 && finalCustomWave && omen >= 5) {
            ravagers++;
        }

        List<String> ids = new ArrayList<>();
        add(ids, "minecraft:pillager", pillagers);
        add(ids, "minecraft:vindicator", vindicators);
        add(ids, "minecraft:witch", witches);
        add(ids, "minecraft:evoker", evokers);
        add(ids, "minecraft:ravager", ravagers);
        return ids;
    }

    private static void add(List<String> ids, String entityId, int count) {
        for (int i = 0; i < count; i++) {
            ids.add(entityId);
        }
    }

    private static int spawnCustomWaveWithMainAndReinforcements(ServerLevel level, ExtraWaveState state, int logicalWave, List<String> mainAttackIds) {
        if (mainAttackIds == null || mainAttackIds.isEmpty()) {
            return 0;
        }
        int[][] anchors = activeSpawnAnchors(state);
        int maxMobs = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_MAX_MOBS_PER_WAVE);
        int spawned = 0;

        // 0.5.1: anchor 0 is the main attack point. It keeps the full original
        // extra-wave composition instead of sharing it with the other anchors.
        for (int i = 0; i < mainAttackIds.size() && spawned < maxMobs; i++) {
            int[] pos = spawnPositionAtAnchor(level, state, logicalWave, 0, i);
            if (summon(level, mainAttackIds.get(i), pos[0], pos[1], pos[2], state, logicalWave,
                    RaiderCategory.VANILLA_MAIN_POINT)) {
                spawned++;
            }
        }

        if (!RaidEnhancementConfig.EXTRA_WAVE_SECONDARY_REINFORCEMENTS_ENABLED || anchors.length <= 1) {
            return spawned;
        }

        // Secondary anchors are additive reinforcement squads. They no longer take
        // bodies away from the main attack point, so the original custom wave size
        // remains intact while the multi-point pressure is visibly stronger.
        for (int anchorIndex = 1; anchorIndex < anchors.length && spawned < maxMobs; anchorIndex++) {
            List<String> reinforcements = secondaryReinforcementCompositionFor(state, logicalWave, anchorIndex);
            for (int localIndex = 0; localIndex < reinforcements.size() && spawned < maxMobs; localIndex++) {
                int[] pos = spawnPositionAtAnchor(level, state, logicalWave, anchorIndex, localIndex);
                if (summon(level, reinforcements.get(localIndex), pos[0], pos[1], pos[2], state, logicalWave,
                        RaiderCategory.VANILLA_SIDE_POINT)) {
                    spawned++;
                }
            }
        }
        return spawned;
    }

    private static List<String> secondaryReinforcementCompositionFor(ExtraWaveState state, int logicalWave, int anchorIndex) {
        int size = Math.max(6, RaidEnhancementConfig.EXTRA_WAVE_SECONDARY_REINFORCEMENT_SIZE);
        List<String> ids = new ArrayList<>();
        // Keep reinforcement squads aggressive and reliable: pillagers and vindicators
        // have stable raid AI/equipment fallback in this patch. A 4/2 split gives each
        // secondary point at least six active raiders without overloading utility mobs.
        int vindicators = Math.max(1, size / 3);
        int pillagers = Math.max(1, size - vindicators);
        add(ids, "minecraft:pillager", pillagers);
        add(ids, "minecraft:vindicator", vindicators);
        while (ids.size() < size) {
            ids.add("minecraft:pillager");
        }
        return ids;
    }

    private static boolean summon(ServerLevel level, String entityId, int x, int y, int z, ExtraWaveState state, int logicalWave) {
        return summon(level, entityId, x, y, z, state, logicalWave,
                RaidsEnhancedIds.isSpecialRaiderId(entityId) ? RaiderCategory.RAIDS_ENHANCED_SPECIAL : RaiderCategory.VANILLA_MAIN_POINT);
    }

    private static boolean summon(ServerLevel level, String entityId, int x, int y, int z,
                                  ExtraWaveState state, int logicalWave, RaiderCategory category) {
        // 0.9.1.8: direct creation is also the authoritative safety gate because it lets
        // the resolver inspect the concrete entity's real bounding box. When validation
        // is enabled, an unsafe/over-budget result must not fall through to command summon
        // at the original unchecked coordinates.
        if (spawnEntityDirect(level, entityId, x, y, z, state, logicalWave, category)) {
            return true;
        }
        if (RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_VALIDATION_ENABLED
                || !RaidEnhancementConfig.EXTRA_WAVE_USE_COMMAND_SUMMON) {
            return false;
        }
        return summonWithCommand(level, entityId, x, y, z);
    }

    private static boolean spawnEntityDirect(ServerLevel level, String entityId, int x, int y, int z,
                                             ExtraWaveState state, int logicalWave, RaiderCategory category) {
        try {
            Class<?> entityTypeClass = CachedReflection.findClass("net.minecraft.world.entity.EntityType");
            Method byString = CachedReflection.findMethod(entityTypeClass, "byString", entityId);
            if (byString == null) {
                return false;
            }
            Object optional = byString.invoke(null, entityId);
            Object entityType = null;
            if (optional instanceof Optional<?> opt && opt.isPresent()) {
                entityType = opt.get();
            }
            if (entityType == null) {
                return false;
            }
            Object created = createEntity(entityType, level);
            if (!(created instanceof Entity entity)) {
                return false;
            }

            double resolvedX = x + 0.5D;
            double resolvedY = y;
            double resolvedZ = z + 0.5D;
            if (RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_VALIDATION_ENABLED) {
                SafeRaidSpawnResolver.Resolution resolution = SafeRaidSpawnResolver.resolveGround(level, entity, x, y, z);
                if (!resolution.found()) {
                    reportSafeSpawnFailure(resolution);
                    return false;
                }
                resolvedX = resolution.x();
                resolvedY = resolution.y();
                resolvedZ = resolution.z();
            } else {
                moveEntity(entity, resolvedX, resolvedY, resolvedZ);
            }

            int blockX = (int) Math.floor(resolvedX);
            int blockY = (int) Math.floor(resolvedY);
            int blockZ = (int) Math.floor(resolvedZ);
            Object blockPos = blockPosAt(blockX, blockY, blockZ);
            // Vanilla-raider entities created through EntityType#create do not
            // automatically receive spawn equipment or raid AI. Finalize and join
            // only after the final safe position is known.
            if (RaidEnhancementConfig.EXTRA_WAVE_FINALIZE_SPAWN) {
                finalizeSpawnIfPossible(entity, level, blockPos);
            }
            if (RaidEnhancementConfig.EXTRA_WAVE_JOIN_NATIVE_RAID) {
                joinNativeRaidIfPossible(state, entity, blockPos, logicalWave);
            }
            if (RaidEnhancementConfig.EXTRA_WAVE_EQUIP_FALLBACK_WEAPONS) {
                equipFallbackWeapon(entity, entityId);
            }
            if (RaidEnhancementConfig.EXTRA_WAVE_SUMMON_PERSISTENT_RAIDERS) {
                invokeOptionalNoArg(entity, "setPersistenceRequired");
            }
            boolean added = addFreshEntity(level, entity);
            if (added) {
                RaiderCategory resolvedCategory = RaidsEnhancedIds.isSpecialRaiderId(entityId)
                        ? RaiderCategory.RAIDS_ENHANCED_SPECIAL
                        : category;
                rememberKnownRaiderFromEntity(state, entity, resolvedCategory, logicalWave, level.getGameTime());
                if (resolvedCategory == RaiderCategory.RAIDS_ENHANCED_SPECIAL) {
                    applySpecialRaiderGlowing(entity, state);
                }
            }
            return added;
        } catch (Throwable throwable) {
            if (!warnedDirectSpawnFailure) {
                warnedDirectSpawnFailure = true;
                System.out.println("[Raid Enhancement Patch] Direct extra-wave spawn failed once: " + throwable);
            }
            return false;
        }
    }

    private static void reportSafeSpawnFailure(SafeRaidSpawnResolver.Resolution resolution) {
        if (resolution == null) {
            return;
        }
        if (resolution.status() == SafeRaidSpawnResolver.Status.TICK_BUDGET_EXHAUSTED) {
            if (!warnedSpawnBudgetExhausted) {
                warnedSpawnBudgetExhausted = true;
                System.out.println("[Raid Enhancement Patch] Safe-spawn search reached its per-tick budget once; remaining patch-owned raiders will use the existing wave retry path instead of loading chunks or spawning inside blocks.");
            }
            return;
        }
        if (!warnedUnsafeSpawnFailure) {
            warnedUnsafeSpawnFailure = true;
            System.out.println("[Raid Enhancement Patch] No safe position was found for one patch-owned raid entity; the entity was skipped instead of being inserted into blocks or fluids.");
        }
    }

    private static Object createEntity(Object entityType, ServerLevel level) throws ReflectiveOperationException {
        if (entityType == null || level == null) {
            return null;
        }
        Method method = CachedReflection.findMethod(entityType.getClass(), "create", level);
        return method == null ? null : method.invoke(entityType, level);
    }

    private static void moveEntity(Object entity, double x, double y, double z) throws ReflectiveOperationException {
        Method moveTo = CachedReflection.findMethod(entity.getClass(), "moveTo", x, y, z, 0.0F, 0.0F);
        if (moveTo != null) {
            moveTo.invoke(entity, x, y, z, 0.0F, 0.0F);
            return;
        }
        Method setPos = CachedReflection.findMethod(entity.getClass(), "setPos", x, y, z);
        if (setPos == null) {
            throw new NoSuchMethodException(entity.getClass().getName() + ".moveTo/setPos");
        }
        setPos.invoke(entity, x, y, z);
    }

    private static boolean addFreshEntity(ServerLevel level, Object entity) throws ReflectiveOperationException {
        Method method = CachedReflection.findMethod(level.getClass(), "addFreshEntity", entity);
        if (method == null) {
            return false;
        }
        Object result = method.invoke(level, entity);
        return !(result instanceof Boolean bool) || bool;
    }



    private static void finalizeSpawnIfPossible(Object entity, ServerLevel level, Object blockPos) {
        if (entity == null || level == null) {
            return;
        }
        try {
            Object difficulty = null;
            if (blockPos != null) {
                try {
                    Method currentDifficulty = level.getClass().getMethod("getCurrentDifficultyAt", blockPos.getClass());
                    currentDifficulty.setAccessible(true);
                    difficulty = currentDifficulty.invoke(level, blockPos);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Some mappings use a superclass/interface parameter. Try by name below.
                    for (Method method : level.getClass().getMethods()) {
                        if (!method.getName().equals("getCurrentDifficultyAt") || method.getParameterCount() != 1) {
                            continue;
                        }
                        method.setAccessible(true);
                        difficulty = method.invoke(level, blockPos);
                        break;
                    }
                }
            }
            Object mobSpawnType = enumConstant("net.minecraft.world.entity.MobSpawnType", "EVENT");
            for (Method method : entity.getClass().getMethods()) {
                if (!method.getName().equals("finalizeSpawn") || method.getParameterCount() != 4) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(level.getClass())) {
                    continue;
                }
                if (difficulty != null && !types[1].isAssignableFrom(difficulty.getClass())) {
                    continue;
                }
                if (mobSpawnType != null && !types[2].isAssignableFrom(mobSpawnType.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(entity, level, difficulty, mobSpawnType, null);
                return;
            }
        } catch (Throwable throwable) {
            if (!warnedFinalizeFailure) {
                warnedFinalizeFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave finalizeSpawn failed once; weapon fallback will still run: " + throwable);
            }
        }
    }

    private static void joinNativeRaidIfPossible(ExtraWaveState state, Object entity, Object blockPos, int logicalWave) {
        if (state == null || state.nativeRaid == null || entity == null || isRaidFinished(state.nativeRaid)) {
            return;
        }
        try {
            int safeWave = Math.max(0, Math.min(logicalWave, state.effectiveNativeWaves()));
            for (Method method : state.nativeRaid.getClass().getMethods()) {
                if (!method.getName().equals("joinRaid") || method.getParameterCount() != 4) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[0] != int.class && types[0] != Integer.TYPE) {
                    continue;
                }
                if (!types[1].isAssignableFrom(entity.getClass())) {
                    continue;
                }
                if (blockPos != null && !types[2].isAssignableFrom(blockPos.getClass())) {
                    continue;
                }
                if (types[3] != boolean.class && types[3] != Boolean.TYPE) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(state.nativeRaid, safeWave, entity, blockPos, false);
                return;
            }
            // Compatibility fallback if joinRaid mapping is unavailable: give the raider a
            // raid pointer and wave number so its raid goals can activate in common mappings.
            invokeOptional(entity, "setCurrentRaid", state.nativeRaid);
            invokeOptional(entity, "setWave", safeWave);
            invokeOptional(entity, "setCanJoinRaid", Boolean.TRUE);
            invokeOptional(entity, "setTicksOutsideRaid", 0);
        } catch (Throwable throwable) {
            if (!warnedJoinRaidFailure) {
                warnedJoinRaidFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave joinRaid failed once; raiders may use fallback AI only: " + throwable);
            }
        }
    }

    private static void equipFallbackWeapon(Object entity, String entityId) {
        if (entity == null || entityId == null) {
            return;
        }
        String itemField = null;
        if (entityId.endsWith(":pillager")) {
            itemField = "CROSSBOW";
        } else if (entityId.endsWith(":vindicator")) {
            itemField = "IRON_AXE";
        }
        if (itemField == null) {
            return;
        }
        try {
            Object item = staticField("net.minecraft.world.item.Items", itemField);
            if (item == null) {
                return;
            }
            Object itemStack = createItemStack(item);
            Object mainHand = enumConstant("net.minecraft.world.entity.EquipmentSlot", "MAINHAND");
            if (itemStack == null || mainHand == null) {
                return;
            }
            for (Method method : entity.getClass().getMethods()) {
                if (!method.getName().equals("setItemSlot") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(mainHand.getClass()) || !types[1].isAssignableFrom(itemStack.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(entity, mainHand, itemStack);
                return;
            }
        } catch (Throwable throwable) {
            if (!warnedEquipFailure) {
                warnedEquipFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave fallback weapon equip failed once: " + throwable);
            }
        }
    }

    private static Object createItemStack(Object item) throws ReflectiveOperationException {
        Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
        for (Constructor<?> constructor : itemStackClass.getConstructors()) {
            if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isAssignableFrom(item.getClass())) {
                constructor.setAccessible(true);
                return constructor.newInstance(item);
            }
        }
        return null;
    }

    private static Object staticField(String className, String fieldName) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        Field field = type.getField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(String className, String constantName) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        return enumConstant(type, constantName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> type, String constantName) {
        if (type == null || constantName == null || !Enum.class.isAssignableFrom(type)) {
            return null;
        }
        try {
            return Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), constantName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void invokeOptional(Object target, String methodName, Object argument) {
        if (target == null) {
            return;
        }
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (argument != null && !wrap(type).isAssignableFrom(argument.getClass())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(target, argument);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional compatibility call.
        }
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static void invokeOptionalNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional compatibility call.
        }
    }

    private static boolean summonWithCommand(ServerLevel level, String entityId, int x, int y, int z) {
        String nbt = RaidEnhancementConfig.EXTRA_WAVE_SUMMON_PERSISTENT_RAIDERS ? " {PersistenceRequired:1b}" : "";
        String command = "summon " + entityId + " " + x + " " + y + " " + z + nbt;
        try {
            Object server = invokeNoArg(level, "getServer");
            Object commands = invokeNoArg(server, "getCommands");
            Object source = invokeNoArg(server, "createCommandSourceStack");
            if (commands == null || source == null) {
                return false;
            }
            Method method = findCommandMethod(commands.getClass(), "performPrefixedCommand");
            if (method == null) {
                method = findCommandMethod(commands.getClass(), "performCommand");
            }
            if (method == null) {
                return false;
            }
            method.setAccessible(true);
            Object result = method.invoke(commands, source, command);
            if (result instanceof Number number) {
                return number.intValue() >= 0;
            }
            return true;
        } catch (Throwable throwable) {
            if (!warnedCommandFailure) {
                warnedCommandFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave command summon failed once: " + throwable);
            }
            return false;
        }
    }

    private static Method findCommandMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types[1] == String.class) {
                return method;
            }
        }
        return null;
    }

    private static boolean isVillageSecurityVictory(ServerLevel level, ExtraWaveState state) {
        if (level == null || state == null) {
            return false;
        }
        try {
            if (state.nativeRaid != null && invokeBoolean(state.nativeRaid, "isLoss")) {
                return false;
            }
            int nearbyRaiders = countNearbyRaiders(level, state.centerX, state.centerY, state.centerZ,
                    RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS);
            if (nearbyRaiders > 0) {
                return false;
            }
            if (customExtraWavesNeeded(state) > 0) {
                return state.customWavesSpawned >= customExtraWavesNeeded(state);
            }
            if (state.nativeRaid != null) {
                return invokeBoolean(state.nativeRaid, "isVictory") || (isRaidFinished(state.nativeRaid) && !invokeBoolean(state.nativeRaid, "isLoss"));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void completeState(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (state == null || state.completed) {
            return;
        }
        boolean villageSecurityVictory = isVillageSecurityVictory(level, state);
        RaidKeyDiagnostics.logRaidDiscovery("completed", level, state.key, state.dimensionId,
                state.centerX, state.centerY, state.centerZ, raidIdForState(state), state.firstSeenGameTime, gameTime, state.nativeRaid);
        VillageSecurityController.complete(level, state.key, state.dimensionId, state.centerX, state.centerY, state.centerZ,
                state.difficultyName, villageSecurityVictory, gameTime);
        state.completed = true;
        state.completedGameTime = gameTime;
        state.currentWaveActive = false;
        rememberTerminatedRaidKey(state.key, gameTime);
        removeLifecycleSnapshot(state.key);
        RaidSessionManager.get(state.key).ifPresent(session -> session.markCompleted("raid_state_completed", gameTime));
        releaseManagedVillagerProtectionNearState(level, state);
        clearHudSnapshot(state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void releaseManagedVillagerProtectionNearState(ServerLevel level, ExtraWaveState state) {
        if (level == null || state == null || state.protectionReleased) {
            return;
        }
        state.protectionReleased = true;
        try {
            Class<?> villagerClass = Class.forName("net.minecraft.world.entity.npc.Villager");
            Class<?> aabbClass = Class.forName(AABB_CLASS_NAME);
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                    double.class, double.class, double.class);
            int radius = Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS,
                    RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS);
            Object box = constructor.newInstance(state.centerX - radius, state.centerY - radius, state.centerZ - radius,
                    state.centerX + radius, state.centerY + radius, state.centerZ + radius);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, aabbClass);
            Object result = getEntities.invoke(level, (Class) villagerClass, box);
            if (!(result instanceof List<?> villagers)) {
                return;
            }
            for (Object candidate : villagers) {
                if (!(candidate instanceof net.minecraft.world.entity.npc.Villager villager)) {
                    continue;
                }
                Optional<ProtectedVillagerState> protectedState = VillagerProtectionController.getState(villager.getUUID());
                if (protectedState.isEmpty()) {
                    continue;
                }
                String source = protectedState.get().source();
                if (RaidEnhancementConfig.EXTRA_WAVE_VILLAGER_PROTECTION_SOURCE.equals(source)
                        || RaidAutoVillagerProtector.AUTO_SOURCE.equals(source)) {
                    VillagerProtectionController.unprotect(villager, true);
                }
            }
        } catch (Throwable throwable) {
            if (!warnedProtectionFailure) {
                warnedProtectionFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave villager protection cleanup failed once: " + throwable);
            }
        }
    }

    private static void refreshNativeObservation(ServerLevel level, ExtraWaveState state, long gameTime) {
        if (level == null || state == null || state.completed) {
            return;
        }
        Object raid = state.nativeRaid;
        Object pos = blockPosAt(state.centerX, state.centerY, state.centerZ);
        Object currentRaidAtCenter = pos == null ? null : getRaidAt(level, pos);
        if (currentRaidAtCenter != null) {
            raid = currentRaidAtCenter;
            state.nativeRaid = currentRaidAtCenter;
            state.lastSeenGameTime = gameTime;
        }
        if (raid == null) {
            return;
        }
        int spawned = getGroupsSpawned(raid);
        int stablePlanTarget = RaidWaveAuthority.targetTotalWaves(state.difficultyName, state.omenLevel);
        reconcileObservedWaveTarget(state, raid, stablePlanTarget, spawned);
        state.maxObservedNativeGroupsSpawned = Math.max(state.maxObservedNativeGroupsSpawned, spawned);
        if (isRaidFinished(raid)) {
            state.nativeRaidFinishedObserved = true;
        }
    }

    private static boolean isTerminatedRaidKey(String key, long gameTime) {
        Long timestamp = TERMINATED_RAID_KEYS.get(key);
        if (timestamp == null) {
            return false;
        }
        if (gameTime - timestamp > TERMINATED_KEY_TTL_TICKS) {
            TERMINATED_RAID_KEYS.remove(key);
            return false;
        }
        return true;
    }

    private static void rememberTerminatedRaidKey(String key, long gameTime) {
        if (key != null) {
            TERMINATED_RAID_KEYS.put(key, gameTime);
        }
    }

    private static void pruneTerminatedRaidKeys(long gameTime) {
        if (TERMINATED_RAID_KEYS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = TERMINATED_RAID_KEYS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > TERMINATED_KEY_TTL_TICKS) {
                iterator.remove();
            }
        }
    }

    private static void clearHudSnapshotByKey(String key) {
        if (key != null) {
            LAST_HUD_SNAPSHOTS.remove(key);
        }
        RaidHudSnapshot snapshot = lastHudSnapshot;
        if (snapshot != null && key != null && snapshot.key().equals(key)) {
            lastHudSnapshot = null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int countNearbyRaiders(ServerLevel level, int x, int y, int z, int radius) {
        try {
            Class<?> raiderClass = Class.forName(RAIDER_CLASS_NAME);
            Class<?> aabbClass = Class.forName(AABB_CLASS_NAME);
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                    double.class, double.class, double.class);
            Object box = constructor.newInstance(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, aabbClass);
            Object result = getEntities.invoke(level, (Class) raiderClass, box);
            if (!(result instanceof List<?> list)) {
                return 0;
            }
            int alive = 0;
            for (Object candidate : list) {
                if (invokeBoolean(candidate, "isAlive")) {
                    alive++;
                }
            }
            return alive;
        } catch (Throwable throwable) {
            if (!warnedScanFailure) {
                warnedScanFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave raider scan failed once and will assume current wave is still active: " + throwable);
            }
            return 1;
        }
    }

    public static boolean isExtraWaveThreatNear(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return false;
        }
        String dimension = dimensionId(level);
        int ex = invokeInt(invokeNoArg(entity, BLOCK_POSITION_METHOD), "getX", 0);
        int ey = invokeInt(invokeNoArg(entity, BLOCK_POSITION_METHOD), "getY", 64);
        int ez = invokeInt(invokeNoArg(entity, BLOCK_POSITION_METHOD), "getZ", 0);
        int radius = Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS,
                RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS);
        long radiusSquared = (long) radius * (long) radius;
        for (ExtraWaveState state : STATES.values()) {
            if (state.completed || !state.dimensionId.equals(dimension)) {
                continue;
            }
            if (!(state.armedForExtraWaves || state.customWavesSpawned > 0)) {
                continue;
            }
            long dx = (long) ex - state.centerX;
            long dy = (long) ey - state.centerY;
            long dz = (long) ez - state.centerZ;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private static void protectVillagersNearState(ServerLevel level, ExtraWaveState state) {
        try {
            Class<?> villagerClass = Class.forName("net.minecraft.world.entity.npc.Villager");
            Class<?> aabbClass = Class.forName(AABB_CLASS_NAME);
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                    double.class, double.class, double.class);
            int radius = Math.max(RaidEnhancementConfig.EXTRA_WAVE_SCAN_RADIUS,
                    RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS);
            Object box = constructor.newInstance(state.centerX - radius, state.centerY - radius, state.centerZ - radius,
                    state.centerX + radius, state.centerY + radius, state.centerZ + radius);
            Method getEntities = level.getClass().getMethod("getEntitiesOfClass", Class.class, aabbClass);
            Object result = getEntities.invoke(level, villagerClass, box);
            if (!(result instanceof List<?> villagers)) {
                return;
            }
            int protectedCount = 0;
            for (Object candidate : villagers) {
                if (protectedCount >= RaidEnhancementConfig.MAX_PROTECTED_VILLAGERS_PER_RAID) {
                    break;
                }
                if (candidate instanceof net.minecraft.world.entity.npc.Villager villager && villager.isAlive()) {
                    if (VillagerProtectionController.protect(villager,
                            RaidEnhancementConfig.EXTRA_WAVE_VILLAGER_PROTECTION_DURATION_TICKS,
                            RaidEnhancementConfig.EXTRA_WAVE_VILLAGER_PROTECTION_SOURCE)) {
                        protectedCount++;
                    }
                }
            }
        } catch (Throwable throwable) {
            if (!warnedProtectionFailure) {
                warnedProtectionFailure = true;
                System.out.println("[Raid Enhancement Patch] Extra wave villager protection failed once: " + throwable);
            }
        }
    }

    private static boolean nativeRaidGoneOrFinished(ServerLevel level, ExtraWaveState state) {
        Object pos = blockPosAt(state.centerX, state.centerY, state.centerZ);
        Object raid = pos == null ? null : getRaidAt(level, pos);
        if (raid == null) {
            return true;
        }
        return isRaidFinished(raid);
    }

    private static Object blockPosAt(int x, int y, int z) {
        try {
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Constructor<?> constructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            return constructor.newInstance(x, y, z);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void prepareSpawnAnchors(ServerLevel level, ExtraWaveState state, int logicalWave) {
        int[][] anchors = chooseSpawnAnchors(level, state, logicalWave);
        if (anchors == null || anchors.length == 0) {
            anchors = new int[][]{chooseFocusedSpawnAnchor(level, state, logicalWave)};
        }
        state.spawnAnchors = anchors;
        state.spawnAnchorX = anchors[0][0];
        state.spawnAnchorY = anchors[0][1];
        state.spawnAnchorZ = anchors[0][2];
        state.spawnAnchorWave = logicalWave;
    }

    private static int[][] chooseSpawnAnchors(ServerLevel level, ExtraWaveState state, int logicalWave) {
        int desired = customSpawnPointCount(state, logicalWave);
        if (desired <= 1) {
            return new int[][]{chooseFocusedSpawnAnchor(level, state, logicalWave)};
        }

        List<int[]> anchors = new ArrayList<>();
        if (RaidEnhancementConfig.EXTRA_WAVE_PREFER_NATIVE_SPAWN_ANCHOR) {
            for (int[] nativeAnchor : findNativeRaidSpawnAnchors(state, logicalWave, desired)) {
                addAnchorIfDistinct(anchors, nativeAnchor, desired);
            }
        }
        for (int[] fallback : fallbackMultiSpawnAnchors(state, logicalWave, desired)) {
            addAnchorIfDistinct(anchors, fallback, desired);
        }
        if (anchors.isEmpty()) {
            anchors.add(chooseFocusedSpawnAnchor(level, state, logicalWave));
        }
        while (anchors.size() < desired) {
            int[] base = anchors.get(anchors.size() - 1);
            int step = RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MIN_ANCHOR_DISTANCE + anchors.size() * 3;
            anchors.add(new int[]{base[0] + step, base[1], base[2] - step});
        }
        return anchors.toArray(new int[0][]);
    }

    private static int customSpawnPointCount(ExtraWaveState state, int logicalWave) {
        if (state == null
                || !RaidEnhancementConfig.MULTI_SPAWN_POINTS_ENABLED
                || !RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_POINTS_ENABLED) {
            return 1;
        }
        if (logicalWave <= state.effectiveNativeWaves() || state.extraWavesNeeded() <= 0) {
            return 1;
        }
        int minTotalAnchors = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MIN_POINTS);
        int maxTotalAnchors = Math.max(minTotalAnchors, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MAX_POINTS);

        // 0.5.3: custom waves use the same difficulty-scaled secondary-point policy
        // as native-wave reinforcements. Return value is total anchors, so add one
        // main attack point to the secondary-point count.
        int secondaryPoints = sideReinforcementPointCount(state, logicalWave);
        if (secondaryPoints <= 0) {
            return 1;
        }
        return clamp(1 + secondaryPoints, Math.max(2, minTotalAnchors), maxTotalAnchors);
    }


    private static int sideReinforcementPointCount(ExtraWaveState state, int logicalWave) {
        if (state == null || logicalWave <= 0) {
            return 0;
        }
        String difficulty = difficultyKey(state.difficultyName);
        int total = Math.max(1, state.logicalTargetWaves);
        int omen = clamp(state.omenLevel, 1, 5);
        if (difficulty.contains("EASY")) {
            return 0;
        }
        if (difficulty.contains("HARD")) {
            int max = Math.min(Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_HARD_MAX_POINTS), hardSecondaryPointCapForOmen(omen));
            return hardSecondaryPointCount(logicalWave, max);
        }
        return normalSecondaryPointCount(logicalWave, total, Math.max(0, RaidEnhancementConfig.NATIVE_WAVE_REINFORCEMENT_NORMAL_MAX_POINTS));
    }

    private static int normalSecondaryPointCount(int logicalWave, int totalWaves, int maxPoints) {
        if (maxPoints <= 0 || logicalWave <= 0) {
            return 0;
        }
        int total = Math.max(1, totalWaves);
        int middleWave = (total + 1) / 2;
        if (logicalWave < middleWave) {
            return 0;
        }
        if (maxPoints <= 1) {
            return 1;
        }
        int secondPointStart = Math.min(total, middleWave + 2);
        return logicalWave >= secondPointStart ? Math.min(2, maxPoints) : 1;
    }

    private static int hardSecondaryPointCapForOmen(int omenLevel) {
        return switch (clamp(omenLevel, 1, 5)) {
            case 1 -> 2;
            case 2, 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    private static int hardSecondaryPointCount(int logicalWave, int maxPoints) {
        if (maxPoints <= 0 || logicalWave <= 0) {
            return 0;
        }
        int points = 1;
        if (logicalWave >= 3) {
            points = 2;
        }
        if (logicalWave >= 5) {
            points = 3;
        }
        if (logicalWave >= 7) {
            points = 4;
        }
        if (logicalWave >= 9) {
            points = 5;
        }
        return Math.min(maxPoints, points);
    }

    private static String difficultyKey(String difficultyName) {
        return difficultyName == null ? "NORMAL" : difficultyName.toUpperCase(java.util.Locale.ROOT);
    }

    private static void addAnchorIfDistinct(List<int[]> anchors, int[] candidate, int desired) {
        if (candidate == null || anchors.size() >= desired) {
            return;
        }
        int minDistance = Math.max(4, RaidEnhancementConfig.EXTRA_WAVE_MULTI_SPAWN_MIN_ANCHOR_DISTANCE);
        long minDistanceSquared = (long) minDistance * (long) minDistance;
        for (int[] existing : anchors) {
            long dx = (long) candidate[0] - existing[0];
            long dz = (long) candidate[2] - existing[2];
            if (dx * dx + dz * dz < minDistanceSquared) {
                return;
            }
        }
        anchors.add(candidate);
    }

    private static List<int[]> fallbackMultiSpawnAnchors(ExtraWaveState state, int logicalWave, int desired) {
        List<int[]> anchors = new ArrayList<>();
        int radius = Math.max(16, RaidEnhancementConfig.EXTRA_WAVE_SPAWN_ANCHOR_RADIUS);
        int[][] offsets = {
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius},
                {radius, radius}, {-radius, -radius}, {radius, -radius}, {-radius, radius}
        };
        int seed = Math.abs((state.key == null ? 0 : state.key.hashCode()) + logicalWave * 131);
        for (int i = 0; i < offsets.length && anchors.size() < desired; i++) {
            int[] offset = offsets[Math.floorMod(seed + i, offsets.length)];
            anchors.add(new int[]{state.centerX + offset[0], state.centerY + 1, state.centerZ + offset[1]});
        }
        return anchors;
    }

    private static int[] chooseFocusedSpawnAnchor(ServerLevel level, ExtraWaveState state, int logicalWave) {
        if (RaidEnhancementConfig.EXTRA_WAVE_PREFER_NATIVE_SPAWN_ANCHOR) {
            int[] nativeAnchor = findNativeRaidSpawnAnchor(state, logicalWave);
            if (nativeAnchor != null) {
                return nativeAnchor;
            }
        }
        return fallbackFocusedSpawnAnchor(state, logicalWave);
    }

    private static int[] findNativeRaidSpawnAnchor(ExtraWaveState state, int logicalWave) {
        if (state == null || state.nativeRaid == null || isRaidFinished(state.nativeRaid)) {
            return null;
        }
        int[][] attempts = {
                {Math.max(0, logicalWave - 1), 0},
                {Math.max(0, logicalWave - 1), 1},
                {Math.max(0, state.effectiveNativeWaves() - 1), 0},
                {Math.max(0, state.effectiveNativeWaves() - 1), 1}
        };
        Method method = CachedReflection.findMethod(state.nativeRaid.getClass(), "findRandomSpawnPos", attempts[0][0], attempts[0][1]);
        if (method == null) {
            return null;
        }
        try {
            for (int[] attempt : attempts) {
                Object result = method.invoke(state.nativeRaid, attempt[0], attempt[1]);
                int[] extracted = extractBlockPosFromOptional(result);
                if (extracted != null) {
                    return extracted;
                }
            }
        } catch (Throwable ignored) {
            // Fall back to a deterministic concentrated anchor below.
        }
        return null;
    }

    private static List<int[]> findNativeRaidSpawnAnchors(ExtraWaveState state, int logicalWave, int desired) {
        List<int[]> anchors = new ArrayList<>();
        if (state == null || state.nativeRaid == null || isRaidFinished(state.nativeRaid) || desired <= 0) {
            return anchors;
        }
        int[] waveAttempts = {
                Math.max(0, logicalWave - 1),
                Math.max(0, logicalWave),
                Math.max(0, state.effectiveNativeWaves() - 1),
                Math.max(0, state.effectiveNativeWaves())
        };
        Method method = CachedReflection.findMethod(state.nativeRaid.getClass(), "findRandomSpawnPos", waveAttempts[0], 0);
        if (method == null) {
            return anchors;
        }
        try {
            for (int waveAttempt : waveAttempts) {
                for (int attempt = 0; attempt < 8 && anchors.size() < desired; attempt++) {
                    Object result = method.invoke(state.nativeRaid, waveAttempt, attempt);
                    int[] extracted = extractBlockPosFromOptional(result);
                    addAnchorIfDistinct(anchors, extracted, desired);
                }
            }
        } catch (Throwable ignored) {
            // Fall back to deterministic multi-anchor candidates below.
        }
        return anchors;
    }

    private static int[] extractBlockPosFromOptional(Object optionalLike) {
        if (optionalLike instanceof Optional<?> optional && optional.isPresent()) {
            Object blockPos = optional.get();
            int x = invokeInt(blockPos, "getX", Integer.MIN_VALUE);
            int y = invokeInt(blockPos, "getY", Integer.MIN_VALUE);
            int z = invokeInt(blockPos, "getZ", Integer.MIN_VALUE);
            if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE && z != Integer.MIN_VALUE) {
                return new int[]{x, y, z};
            }
        }
        return null;
    }

    private static int[] fallbackFocusedSpawnAnchor(ExtraWaveState state, int logicalWave) {
        int radius = Math.max(16, RaidEnhancementConfig.EXTRA_WAVE_SPAWN_ANCHOR_RADIUS);
        int[][] offsets = {
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius},
                {radius, radius}, {radius, -radius}, {-radius, radius}, {-radius, -radius}
        };
        int seed = Math.abs((state.key == null ? 0 : state.key.hashCode()) + logicalWave * 31);
        int[] offset = offsets[Math.floorMod(seed, offsets.length)];
        return new int[]{state.centerX + offset[0], state.centerY + 1, state.centerZ + offset[1]};
    }

    private static int[] spawnPosition(ServerLevel level, ExtraWaveState state, int logicalWave, int index) {
        int[][] anchors = activeSpawnAnchors(state);
        int anchorIndex = Math.floorMod(index, anchors.length);
        int localIndex = Math.max(0, index / Math.max(1, anchors.length));
        return spawnPositionAtAnchor(level, state, logicalWave, anchorIndex, localIndex);
    }

    private static int[] spawnPositionAtSpecificAnchor(int[] anchor, int localIndex) {
        int radius = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_CLUSTER_RADIUS);
        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius}
        };
        int[] offset = offsets[Math.floorMod(localIndex, offsets.length)];
        int spreadRing = localIndex / offsets.length;
        return new int[]{anchor[0] + offset[0] + spreadRing, anchor[1], anchor[2] + offset[1] - spreadRing};
    }

    private static int[] spawnPositionAtAnchor(ServerLevel level, ExtraWaveState state, int logicalWave, int anchorIndex, int localIndex) {
        if (state.spawnAnchorWave != logicalWave || state.spawnAnchors == null || state.spawnAnchors.length == 0) {
            prepareSpawnAnchors(level, state, logicalWave);
        }
        int[][] anchors = activeSpawnAnchors(state);
        int[] anchor = anchors[Math.floorMod(anchorIndex, anchors.length)];
        int radius = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_CLUSTER_RADIUS);
        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                {1, 2}, {-1, 2}, {1, -2}, {-1, -2},
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius}
        };
        int[] offset = offsets[Math.floorMod(localIndex, offsets.length)];
        int spreadRing = localIndex / offsets.length;
        return new int[]{anchor[0] + offset[0] + spreadRing, anchor[1], anchor[2] + offset[1] - spreadRing};
    }

    private static int[][] activeSpawnAnchors(ExtraWaveState state) {
        if (state == null || state.spawnAnchors == null || state.spawnAnchors.length == 0) {
            return new int[][]{{state == null ? 0 : state.spawnAnchorX, state == null ? 64 : state.spawnAnchorY, state == null ? 0 : state.spawnAnchorZ}};
        }
        return state.spawnAnchors;
    }

    private static int[] centerOf(Object raid, Object fallbackPos) {
        Object center = invokeNoArg(raid, "getCenter");
        if (center == null) {
            center = fallbackPos;
        }
        int x = invokeInt(center, "getX", 0);
        int y = invokeInt(center, "getY", 64);
        int z = invokeInt(center, "getZ", 0);
        return new int[]{x, y, z};
    }

    private static Object getRaidAt(ServerLevel level, Object blockPos) {
        if (blockPos == null) {
            return null;
        }
        try {
            Method getRaidAt = level.getClass().getMethod(GET_RAID_AT_METHOD, blockPos.getClass());
            return getRaidAt.invoke(level, blockPos);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object offsetPosition(Object blockPos, int x, int y, int z) {
        if (blockPos == null) {
            return null;
        }
        try {
            Method offset = blockPos.getClass().getMethod("offset", int.class, int.class, int.class);
            return offset.invoke(blockPos, x, y, z);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static int[][] sampleOffsets() {
        int radius = Math.max(0, RaidEnhancementConfig.WAVE_EXPANSION_POSITION_SAMPLE_RADIUS);
        if (radius <= 0) {
            return new int[][]{{0, 0, 0}};
        }
        return new int[][]{
                {0, 0, 0},
                {radius, 0, 0}, {-radius, 0, 0}, {0, 0, radius}, {0, 0, -radius},
                {radius, 0, radius}, {radius, 0, -radius}, {-radius, 0, radius}, {-radius, 0, -radius}
        };
    }

    private static String stateKey(ServerLevel level, Object raid) {
        // 0.4.17: use the raid center as the primary key instead of object identity.
        // During the vanilla victory / extra-wave bridge, getRaidAt can expose a raid
        // object whose identity is not stable enough for this lightweight controller.
        // Identity-keyed states let the same village raid be rediscovered as a fresh
        // state, which can overwrite the HUD back to 8/9 and arm another wave-9 pack.
        Object center = invokeNoArg(raid, "getCenter");
        if (center != null) {
            int x = invokeInt(center, "getX", Integer.MIN_VALUE);
            int y = invokeInt(center, "getY", Integer.MIN_VALUE);
            int z = invokeInt(center, "getZ", Integer.MIN_VALUE);
            if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE && z != Integer.MIN_VALUE) {
                return dimensionId(level) + "@center:" + x + "," + y + "," + z;
            }
        }
        return dimensionId(level) + "@identity:" + System.identityHashCode(raid);
    }

    private static String dimensionId(ServerLevel level) {
        try {
            return level.dimension().location().toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static String runtimeDifficultyName(ServerLevel level) {
        Object difficulty = invokeNoArg(level, LEVEL_DIFFICULTY_METHOD);
        if (difficulty instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return difficulty == null ? "NORMAL" : difficulty.toString();
    }

    private static int getGroupsSpawned(Object raid) {
        Field groupsSpawned = findField(raid.getClass(), RAID_GROUPS_SPAWNED_FIELD);
        if (groupsSpawned == null) {
            return 0;
        }
        try {
            groupsSpawned.setAccessible(true);
            return groupsSpawned.getInt(raid);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int getNativeNumGroups(Object raid) {
        Field numGroups = findField(raid.getClass(), RAID_NUM_GROUPS_FIELD);
        if (numGroups == null) {
            return 0;
        }
        try {
            numGroups.setAccessible(true);
            return numGroups.getInt(raid);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int authoritativeLogicalTarget(int planTarget, int spawned) {
        // 0.8.9.8.5: target is authoritative plan only. groupsSpawned is observed
        // progress and must never raise HUD/bridge/settlement target by itself.
        return Math.max(1, planTarget);
    }

    private static void reconcileObservedWaveTarget(ExtraWaveState state, Object raid, int planTarget, int spawned) {
        if (state == null) {
            return;
        }
        int nativeNumGroups = getNativeNumGroups(raid);
        if (nativeNumGroups > 0) {
            state.observedNativeTargetWaves = Math.max(state.observedNativeTargetWaves, nativeNumGroups);
        }
        state.logicalTargetWaves = Math.max(1, authoritativeLogicalTarget(planTarget, spawned));
        state.nativeSafeWaves = nativeSafeMaxWaves();
        if (!state.hasCustomExtraWaves()) {
            state.armedForExtraWaves = false;
            state.currentWaveActive = false;
            state.bridgeHoldActive = false;
            state.customWavesSpawned = 0;
            state.customBaseNativeWaves = 0;
            state.plannedCustomExtraWaves = 0;
            state.activeCustomLogicalWave = -1;
        }
    }

    private static boolean isRaidFinished(Object raid) {
        boolean loss = invokeBoolean(raid, "isLoss");
        boolean victory = invokeBoolean(raid, "isVictory");
        if (victory && shouldSuppressVictory(raid)) {
            return loss;
        }
        return invokeBoolean(raid, "isStopped")
                || victory
                || loss;
    }

    private static int nativeSafeMaxWaves() {
        return RaidWaveAuthority.NATIVE_WAVE_LIMIT;
    }

    public static RaidHudSnapshot latestHudSnapshot(String dimensionId) {
        RaidHudSnapshot snapshot = lastHudSnapshot;
        if (snapshot == null) {
            return null;
        }
        if (dimensionId != null && !dimensionId.equals(snapshot.dimensionId())) {
            return null;
        }
        return snapshot;
    }

    public static RaidHudSnapshot latestHudSnapshot(String dimensionId, int playerX, int playerY, int playerZ, long gameTime) {
        RaidHudSnapshot best = null;
        long bestDistanceSquared = Long.MAX_VALUE;
        int clearRadius = Math.max(RaidEnhancementConfig.RAID_WAVE_HUD_VISIBLE_RADIUS,
                RaidEnhancementConfig.RAID_WAVE_HUD_CLEAR_RADIUS);
        long clearRadiusSquared = (long) clearRadius * (long) clearRadius;
        long staleTicks = Math.max(RaidEnhancementConfig.RAID_WAVE_HUD_STALE_TICKS,
                RaidEnhancementConfig.RAID_WAVE_HUD_CLIENT_STALE_TICKS);
        for (RaidHudSnapshot snapshot : List.copyOf(LAST_HUD_SNAPSHOTS.values())) {
            if (snapshot == null || snapshot.totalWaves() <= 0 || snapshot.currentWave() <= 0) {
                continue;
            }
            if (dimensionId != null && !dimensionId.equals(snapshot.dimensionId())) {
                continue;
            }
            if (gameTime >= 0L && snapshot.gameTime() >= 0L && gameTime - snapshot.gameTime() > staleTicks) {
                continue;
            }
            long distanceSquared = distanceSquared(playerX, playerY, playerZ,
                    snapshot.centerX(), snapshot.centerY(), snapshot.centerZ());
            if (distanceSquared > clearRadiusSquared) {
                continue;
            }
            if (!RaidEnhancementConfig.RAID_WAVE_HUD_SELECT_NEAREST_RAID) {
                return snapshot;
            }
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = snapshot;
            }
        }
        return best;
    }

    private static void publishHudSnapshot(ExtraWaveState state, long gameTime) {
        if (!RaidEnhancementConfig.RAID_WAVE_HUD_ENABLED || state == null || state.completed) {
            return;
        }
        int currentWave;
        if (state.activeCustomLogicalWave > 0 && (state.currentWaveActive || state.customWavesSpawned > 0)) {
            currentWave = state.activeCustomLogicalWave;
        } else if (state.customWavesSpawned > 0) {
            currentWave = customBaseNativeWaves(state) + state.customWavesSpawned;
        } else if (state.bridgeHoldActive || state.armedForExtraWaves || state.nativeRaidFinishedObserved) {
            currentWave = Math.max(state.effectiveNativeWaves(), state.maxObservedNativeGroupsSpawned);
        } else {
            currentWave = Math.max(1, state.maxObservedNativeGroupsSpawned);
        }
        int totalWaves = displayTotalWaves(state);
        Optional<RaidSession> session = RaidSessionManager.get(state.key);
        if (session.isPresent()) {
            RaidSession raidSession = session.get();
            int sessionWave = raidSession.currentWave();
            if (sessionWave > 0 && sessionWave <= totalWaves
                    && (raidSession.currentWaveClearStarted() || state.knownRaiderCacheWave == sessionWave)) {
                // 0.8.8.3: after lifecycle recovery or a return from a paused battle,
                // ExtraWaveState can briefly report the bridge/native wave while the
                // runtime RaidSession still owns the active clear timer. Prefer the
                // session wave so the HUD keeps showing the clear timer instead of
                // degrading to wave-only text.
                currentWave = sessionWave;
            }
        }
        currentWave = clamp(currentWave, 1, totalWaves);
        boolean totalAssault = totalWaves >= 9 && currentWave > RaidWaveAuthority.NATIVE_WAVE_LIMIT;

        boolean clearTimerAvailable = false;
        boolean clearWaitingForRaiders = RaidEnhancementConfig.WAVE_TIME_DISPLAY_ENABLED;
        boolean clearBudgetLocked = false;
        boolean clearBudgetCollecting = false;
        boolean clearTimedOut = false;
        boolean clearClockPaused = false;
        int clearBudgetSeconds = 0;
        int clearPostLockExtraSeconds = 0;
        long clearTimeoutGameTime = 0L;
        long clearCollectionEndGameTime = 0L;
        if (session.isPresent()) {
            RaidSession raidSession = session.get();
            boolean sameWave = raidSession.currentWave() == currentWave;
            clearWaitingForRaiders = RaidEnhancementConfig.WAVE_TIME_DISPLAY_ENABLED
                    && sameWave
                    && !raidSession.currentWaveClearStarted();
            clearTimerAvailable = RaidEnhancementConfig.WAVE_TIME_DISPLAY_ENABLED
                    && sameWave
                    && raidSession.currentWaveClearStarted()
                    && raidSession.currentWaveBudgetSeconds() > 0;
            clearBudgetLocked = raidSession.currentWaveBudgetLocked();
            clearBudgetCollecting = clearTimerAvailable && raidSession.currentWaveBudgetCollecting(gameTime);
            clearTimedOut = raidSession.currentWaveTimedOut();
            clearClockPaused = raidSession.currentWaveClockPausedAfterGrace();
            clearBudgetSeconds = Math.max(0, raidSession.currentWaveBudgetSeconds());
            clearPostLockExtraSeconds = Math.max(0, raidSession.currentWavePostLockExtraSeconds());
            clearTimeoutGameTime = raidSession.currentWaveTimeoutGameTime();
            clearCollectionEndGameTime = raidSession.waveStartedGameTime()
                    + Math.max(0L, RaidEnhancementConfig.WAVE_TIME_BUDGET_COLLECTION_TICKS)
                    + raidSession.currentWaveTotalPausedTicks(gameTime);
        }
        RaidHudSnapshot snapshot = new RaidHudSnapshot(state.key, state.dimensionId, state.centerX, state.centerY, state.centerZ,
                currentWave, totalWaves, totalAssault, gameTime, clearTimerAvailable, clearWaitingForRaiders,
                clearBudgetLocked, clearBudgetCollecting, clearTimedOut, clearClockPaused, clearBudgetSeconds, clearPostLockExtraSeconds,
                clearTimeoutGameTime, clearCollectionEndGameTime);
        lastHudSnapshot = snapshot;
        LAST_HUD_SNAPSHOTS.put(state.key, snapshot);
        RaidEncounterAuthority.publish(new RaidEncounterSnapshot(state.key, state.dimensionId, state.centerX, state.centerY, state.centerZ,
                state.difficultyName, state.omenLevel, currentWave, totalWaves,
                RaidWaveAuthority.nativeWaveLimitForTarget(totalWaves), RaidWaveAuthority.customWaveCount(totalWaves),
                Math.max(0, state.customWavesSpawned), totalAssault,
                state.bridgeHoldActive || state.armedForExtraWaves, state.nativeRaidFinishedObserved, gameTime,
                clearTimerAvailable, clearTimedOut, clearClockPaused, clearTimeoutGameTime));
        updateNativeRaidBossbarTitle(state, currentWave, totalWaves, totalAssault, clearTimerTextForBossbar(snapshot, gameTime));
        persistLifecycleSnapshot(state, gameTime, false);
    }

    private static void sendRaidWaveHudActionBar(ServerLevel level, ExtraWaveState state, long gameTime) {
        // 0.8.9.8.6: bottom actionbar fallback removed at user request.
        // Wave information is now pushed into the vanilla top raid bossbar title.
    }

    private static String clearTimerTextForBossbar(RaidHudSnapshot snapshot, long gameTime) {
        if (snapshot == null || !snapshot.clearTimerAvailable()) {
            return "";
        }
        if (snapshot.clearClockPaused()) {
            return "暂停";
        }
        if (snapshot.clearTimedOut()) {
            return "超时";
        }
        if (snapshot.clearTimeoutGameTime() <= 0L) {
            return "";
        }
        long remainingTicks = snapshot.clearTimeoutGameTime() - gameTime;
        int remainingSeconds = (int) Math.max(0L, (remainingTicks + 19L) / 20L);
        if (remainingSeconds <= 0) {
            return "超时";
        }
        return formatClockDuration(remainingSeconds);
    }

    private static void updateNativeRaidBossbarTitle(ExtraWaveState state, int currentWave, int totalWaves,
                                                     boolean totalAssault, String clearTimerText) {
        if (state == null || state.nativeRaid == null || totalWaves <= 0 || currentWave <= 0) {
            return;
        }
        try {
            Object bossEvent = findServerBossEvent(state.nativeRaid);
            if (bossEvent == null) {
                return;
            }
            String text = RaidBossbarTitleFormatter.format(currentWave, totalWaves, totalAssault, clearTimerText);
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            Object component = literal.invoke(null, text);
            Method setName = null;
            for (Method method : bossEvent.getClass().getMethods()) {
                if (!method.getName().equals("setName") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if (parameterType.isAssignableFrom(componentClass) || componentClass.isAssignableFrom(parameterType)) {
                    setName = method;
                    break;
                }
            }
            if (setName != null) {
                setName.setAccessible(true);
                setName.invoke(bossEvent, component);
            }
        } catch (Throwable ignored) {
            // Top bossbar title is display-only. Never affect raid logic.
        }
    }

    private static Object findServerBossEvent(Object raid) {
        if (raid == null) {
            return null;
        }
        Class<?> current = raid.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(raid);
                    if (value == null) {
                        continue;
                    }
                    String className = value.getClass().getName();
                    if ("net.minecraft.server.level.ServerBossEvent".equals(className)
                            || className.endsWith("ServerBossEvent")) {
                        return value;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try the next field.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static int displayTotalWaves(ExtraWaveState state) {
        if (state == null) {
            return 1;
        }
        return Math.max(1, state.logicalTargetWaves);
    }

    private static void clearHudSnapshot(ExtraWaveState state) {
        RaidHudSnapshot snapshot = lastHudSnapshot;
        if (state != null) {
            LAST_HUD_SNAPSHOTS.remove(state.key);
            RaidEncounterAuthority.remove(state.key);
        }
        if (snapshot != null && state != null && snapshot.key().equals(state.key)) {
            lastHudSnapshot = null;
        }
    }

    private static void pruneStaleHudSnapshots(ServerLevel level, long gameTime) {
        if (level == null || LAST_HUD_SNAPSHOTS.isEmpty()) {
            return;
        }
        String dimension = dimensionId(level);
        long staleTicks = Math.max(RaidEnhancementConfig.RAID_WAVE_HUD_STALE_TICKS,
                RaidEnhancementConfig.RAID_WAVE_HUD_CLIENT_STALE_TICKS);
        LAST_HUD_SNAPSHOTS.entrySet().removeIf(entry -> {
            RaidHudSnapshot snapshot = entry.getValue();
            if (snapshot == null) {
                return true;
            }
            // Step 8.6: do not remove a server snapshot merely because no player is
            // currently near it. The client is responsible for range-based hiding.
            // Deleting here made the server publish path and client render path fight,
            // causing visible HUD flicker when player detection was briefly empty.
            return dimension.equals(snapshot.dimensionId()) && gameTime - snapshot.gameTime() > staleTicks;
        });
        RaidHudSnapshot snapshot = lastHudSnapshot;
        if (snapshot != null && !LAST_HUD_SNAPSHOTS.containsKey(snapshot.key())) {
            lastHudSnapshot = null;
        }
    }

    private static boolean hasAnyPlayerNearSnapshot(ServerLevel level, RaidHudSnapshot snapshot, int radius) {
        if (level == null || snapshot == null) {
            return false;
        }
        long radiusSquared = (long) Math.max(1, radius) * (long) Math.max(1, radius);
        for (Object playerObject : playersSnapshot(level)) {
            if (!(playerObject instanceof Entity player) || !player.isAlive()) {
                continue;
            }
            int[] pos = entityBlockPosition(player, snapshot.centerX(), snapshot.centerY(), snapshot.centerZ());
            if (distanceSquared(pos[0], pos[1], pos[2], snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private static long distanceSquared(int ax, int ay, int az, int bx, int by, int bz) {
        long dx = (long) ax - bx;
        long dy = (long) ay - by;
        long dz = (long) az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void ensureLifecycleSnapshotsLoaded() {
        if (lifecycleSnapshotsLoaded || !RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED) {
            lifecycleSnapshotsLoaded = true;
            return;
        }
        lifecycleSnapshotsLoaded = true;
        Path file = lifecyclePersistencePath();
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
            PERSISTED_LIFECYCLE_SNAPSHOTS.clear();
            String encodedKeys = properties.getProperty("keys", "").trim();
            if (!encodedKeys.isBlank()) {
                for (String encodedKey : encodedKeys.split(",")) {
                    String safeKey = encodedKey.trim();
                    if (safeKey.isBlank()) {
                        continue;
                    }
                    PersistedLifecycleSnapshot snapshot = PersistedLifecycleSnapshot.fromProperties(properties, safeKey);
                    if (snapshot != null && snapshot.key() != null && !snapshot.key().isBlank()) {
                        PERSISTED_LIFECYCLE_SNAPSHOTS.put(snapshot.key(), snapshot);
                    }
                }
            }
            if (!announcedLifecyclePersistence && !PERSISTED_LIFECYCLE_SNAPSHOTS.isEmpty()) {
                announcedLifecyclePersistence = true;
                System.out.println("[Raid Enhancement Patch] Loaded " + PERSISTED_LIFECYCLE_SNAPSHOTS.size()
                        + " raid lifecycle snapshot(s) for Step 8.5 metadata recovery.");
            }
        } catch (IOException | RuntimeException exception) {
            warnLifecyclePersistence("load", exception);
        }
    }

    private static void applyPersistedLifecycleSnapshotIfFresh(ExtraWaveState state, long gameTime) {
        if (state == null || !RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED) {
            return;
        }
        ensureLifecycleSnapshotsLoaded();
        PersistedLifecycleSnapshot snapshot = PERSISTED_LIFECYCLE_SNAPSHOTS.get(state.key);
        if (snapshot == null || snapshot.completed()) {
            return;
        }
        long ttl = Math.max(1L, RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_RESTORE_TTL_TICKS);
        if (snapshot.lastSeenGameTime() > 0L && gameTime - snapshot.lastSeenGameTime() > ttl) {
            PERSISTED_LIFECYCLE_SNAPSHOTS.remove(state.key);
            return;
        }

        int currentPlanTarget = RaidWaveAuthority.targetTotalWaves(state.difficultyName, state.omenLevel);
        int snapshotPlanTarget = RaidWaveExpansionPlan.forRuntimeDifficulty(snapshot.difficultyName(), snapshot.omenLevel()).totalWaves();
        boolean samePlan = sameText(state.difficultyName, snapshot.difficultyName())
                && state.omenLevel == snapshot.omenLevel()
                && currentPlanTarget == snapshotPlanTarget
                && Math.max(1, snapshot.logicalTargetWaves()) == Math.max(1, currentPlanTarget);
        boolean freshNativeRaidJustStarted = state.nativeRaid != null
                && !isRaidFinished(state.nativeRaid)
                && getGroupsSpawned(state.nativeRaid) <= 1;
        boolean snapshotWouldArmNonCustomRaid = currentPlanTarget <= RaidWaveAuthority.NATIVE_WAVE_LIMIT
                && (snapshot.armedForExtraWaves() || snapshot.customWavesSpawned() > 0
                || snapshot.bridgeHoldActive() || snapshot.currentWaveActive());
        if (!samePlan || freshNativeRaidJustStarted || snapshotWouldArmNonCustomRaid) {
            // 0.8.9.8.4: lifecycle snapshots are keyed by village center. A new raid in
            // the same village must not inherit the previous raid's omen/target/custom
            // progress, otherwise an 8-wave raid can spawn a stale 9-11 chain.
            PERSISTED_LIFECYCLE_SNAPSHOTS.remove(state.key);
            removeLifecycleSnapshot(state.key);
            return;
        }

        state.difficultyName = nonBlank(snapshot.difficultyName(), state.difficultyName);
        state.omenLevel = Math.max(1, snapshot.omenLevel());
        state.nativeSafeWaves = Math.max(nativeSafeMaxWaves(), snapshot.nativeSafeWaves());
        state.logicalTargetWaves = Math.max(1, currentPlanTarget);
        state.observedNativeTargetWaves = Math.max(state.observedNativeTargetWaves, snapshot.observedNativeTargetWaves());
        state.maxObservedNativeGroupsSpawned = Math.max(state.maxObservedNativeGroupsSpawned, snapshot.maxObservedNativeGroupsSpawned());
        state.customBaseNativeWaves = snapshot.customBaseNativeWaves() > 0
                ? Math.min(Math.max(1, currentPlanTarget), snapshot.customBaseNativeWaves())
                : state.customBaseNativeWaves;
        state.customWavesSpawned = Math.min(Math.max(0, customExtraWavesNeeded(state)), Math.max(0, snapshot.customWavesSpawned()));
        state.activeCustomLogicalWave = snapshot.activeCustomLogicalWave() > 0
                ? Math.min(Math.max(1, currentPlanTarget), snapshot.activeCustomLogicalWave())
                : state.activeCustomLogicalWave;
        state.plannedCustomExtraWaves = customExtraWavesNeeded(state);
        state.armedForExtraWaves = state.armedForExtraWaves || snapshot.armedForExtraWaves();
        state.currentWaveActive = state.currentWaveActive || snapshot.currentWaveActive();
        state.nativeRaidFinishedObserved = state.nativeRaidFinishedObserved || snapshot.nativeRaidFinishedObserved();
        state.bridgeHoldActive = state.bridgeHoldActive || snapshot.bridgeHoldActive();
        state.firstSeenGameTime = Math.min(state.firstSeenGameTime, snapshot.firstSeenGameTime() <= 0L
                ? state.firstSeenGameTime : snapshot.firstSeenGameTime());
        state.lastSeenGameTime = Math.max(state.lastSeenGameTime, gameTime);
        copyEncodedIntSet(snapshot.nativeReinforcedWaves(), state.nativeReinforcedWaves);
        copyEncodedIntSet(snapshot.specialReinforcedWaves(), state.specialReinforcedWaves);
        state.normalRaidsEnhancedSpecialDone = state.normalRaidsEnhancedSpecialDone || snapshot.normalRaidsEnhancedSpecialDone();
        state.knownRaiderCacheWave = Math.max(state.knownRaiderCacheWave, snapshot.knownRaiderCacheWave());
        state.restoredFromLifecycleSnapshot = true;
        if (!announcedLifecyclePersistence) {
            announcedLifecyclePersistence = true;
            System.out.println("[Raid Enhancement Patch] Restored raid lifecycle metadata for " + state.key
                    + ": difficulty=" + state.difficultyName + ", omen=" + state.omenLevel
                    + ", totalWaves=" + state.logicalTargetWaves + ".");
        }
    }

    private static void persistLifecycleSnapshot(ExtraWaveState state, long gameTime, boolean force) {
        if (state == null || state.completed || !RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED) {
            return;
        }
        long interval = Math.max(1L, RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSIST_INTERVAL_TICKS);
        if (!force && state.lastLifecyclePersistGameTime > 0L && gameTime - state.lastLifecyclePersistGameTime < interval) {
            return;
        }
        state.lastLifecyclePersistGameTime = gameTime;
        PERSISTED_LIFECYCLE_SNAPSHOTS.put(state.key, PersistedLifecycleSnapshot.fromState(state, gameTime));
        saveLifecycleSnapshots();
    }

    private static void removeLifecycleSnapshot(String key) {
        if (key == null || key.isBlank() || !RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED) {
            return;
        }
        ensureLifecycleSnapshotsLoaded();
        if (PERSISTED_LIFECYCLE_SNAPSHOTS.remove(key) != null) {
            saveLifecycleSnapshots();
        }
    }

    private static void pruneStalePersistedLifecycleSnapshots(long gameTime) {
        if (!RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED || PERSISTED_LIFECYCLE_SNAPSHOTS.isEmpty()) {
            return;
        }
        long ttl = Math.max(1L, RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_RESTORE_TTL_TICKS);
        int before = PERSISTED_LIFECYCLE_SNAPSHOTS.size();
        PERSISTED_LIFECYCLE_SNAPSHOTS.entrySet().removeIf(entry -> {
            PersistedLifecycleSnapshot snapshot = entry.getValue();
            return snapshot == null || snapshot.completed()
                    || (snapshot.lastSeenGameTime() > 0L && gameTime - snapshot.lastSeenGameTime() > ttl);
        });
        if (before != PERSISTED_LIFECYCLE_SNAPSHOTS.size()) {
            saveLifecycleSnapshots();
        }
    }

    private static void saveLifecycleSnapshots() {
        if (!RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED) {
            return;
        }
        try {
            Path file = lifecyclePersistencePath();
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Properties properties = new Properties();
            StringBuilder keys = new StringBuilder();
            for (PersistedLifecycleSnapshot snapshot : PERSISTED_LIFECYCLE_SNAPSHOTS.values()) {
                if (snapshot == null || snapshot.completed()) {
                    continue;
                }
                String safeKey = encodeLifecycleKey(snapshot.key());
                if (keys.length() > 0) {
                    keys.append(',');
                }
                keys.append(safeKey);
                snapshot.writeTo(properties, safeKey);
            }
            properties.setProperty("keys", keys.toString());
            try (OutputStream outputStream = Files.newOutputStream(file)) {
                properties.store(outputStream, "Raid Enhancement Patch 0.8.6 raid session lifecycle metadata");
            }
        } catch (IOException | RuntimeException exception) {
            warnLifecyclePersistence("save", exception);
        }
    }

    private static Path lifecyclePersistencePath() {
        String path = RaidEnhancementConfig.RAID_SESSION_LIFECYCLE_PERSISTENCE_FILE;
        if (path == null || path.isBlank()) {
            path = "config/raid_enhancement_patch/raid_session_lifecycle.properties";
        }
        return Path.of(path);
    }

    private static String encodeLifecycleKey(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((key == null ? "unknown" : key)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String decodeLifecycleKey(String key) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(key);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }


    private static boolean sameText(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equalsIgnoreCase(second);
    }
    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void warnLifecyclePersistence(String phase, Throwable throwable) {
        if (!warnedLifecyclePersistenceFailure) {
            warnedLifecyclePersistenceFailure = true;
            System.out.println("[Raid Enhancement Patch] Raid lifecycle metadata " + phase
                    + " failed once and was suppressed: " + throwable);
        }
    }

    private static int normalizedOmenLevel(int rawOmenLevel) {
        int normalized = rawOmenLevel - Math.max(0, RaidEnhancementConfig.RAID_OMEN_RUNTIME_LEVEL_OFFSET);
        if (normalized < 1) {
            return 1;
        }
        if (normalized > 5) {
            return 5;
        }
        return normalized;
    }

    public record RaidHudSnapshot(
            String key,
            String dimensionId,
            int centerX,
            int centerY,
            int centerZ,
            int currentWave,
            int totalWaves,
            boolean totalAssault,
            long gameTime,
            boolean clearTimerAvailable,
            boolean clearWaitingForRaiders,
            boolean clearBudgetLocked,
            boolean clearBudgetCollecting,
            boolean clearTimedOut,
            boolean clearClockPaused,
            int clearBudgetSeconds,
            int clearPostLockExtraSeconds,
            long clearTimeoutGameTime,
            long clearCollectionEndGameTime
    ) {
    }

    private static void copyEncodedIntSet(String encoded, Set<Integer> target) {
        if (encoded == null || encoded.isBlank() || target == null) {
            return;
        }
        for (String part : encoded.split(",")) {
            try {
                if (!part.isBlank()) {
                    target.add(Integer.parseInt(part.trim()));
                }
            } catch (NumberFormatException ignored) {
                // Ignore one corrupt entry, keep the rest of the lifecycle snapshot.
            }
        }
    }

    private record PersistedLifecycleSnapshot(
            String key,
            String dimensionId,
            int centerX,
            int centerY,
            int centerZ,
            String difficultyName,
            int omenLevel,
            int nativeSafeWaves,
            int logicalTargetWaves,
            int observedNativeTargetWaves,
            int maxObservedNativeGroupsSpawned,
            int customWavesSpawned,
            int customBaseNativeWaves,
            int activeCustomLogicalWave,
            boolean armedForExtraWaves,
            boolean currentWaveActive,
            boolean nativeRaidFinishedObserved,
            boolean bridgeHoldActive,
            boolean completed,
            long firstSeenGameTime,
            long lastSeenGameTime,
            String nativeReinforcedWaves,
            String specialReinforcedWaves,
            boolean normalRaidsEnhancedSpecialDone,
            int knownRaiderCacheWave
    ) {
        static PersistedLifecycleSnapshot fromState(ExtraWaveState state, long gameTime) {
            return new PersistedLifecycleSnapshot(state.key, state.dimensionId, state.centerX, state.centerY, state.centerZ,
                    state.difficultyName, state.omenLevel, state.nativeSafeWaves, state.logicalTargetWaves,
                    state.observedNativeTargetWaves, state.maxObservedNativeGroupsSpawned, state.customWavesSpawned,
                    state.customBaseNativeWaves, state.activeCustomLogicalWave, state.armedForExtraWaves, state.currentWaveActive,
                    state.nativeRaidFinishedObserved, state.bridgeHoldActive, state.completed,
                    state.firstSeenGameTime, Math.max(state.lastSeenGameTime, gameTime),
                    encodeIntSet(state.nativeReinforcedWaves), encodeIntSet(state.specialReinforcedWaves),
                    state.normalRaidsEnhancedSpecialDone, state.knownRaiderCacheWave);
        }

        static PersistedLifecycleSnapshot fromProperties(Properties properties, String safeKey) {
            if (properties == null || safeKey == null || safeKey.isBlank()) {
                return null;
            }
            String prefix = "session." + safeKey + ".";
            String decodedKey = decodeLifecycleKey(safeKey);
            String key = properties.getProperty(prefix + "key", decodedKey);
            if (key == null || key.isBlank()) {
                return null;
            }
            return new PersistedLifecycleSnapshot(
                    key,
                    properties.getProperty(prefix + "dimension", "unknown"),
                    intProperty(properties, prefix + "centerX", 0),
                    intProperty(properties, prefix + "centerY", 64),
                    intProperty(properties, prefix + "centerZ", 0),
                    properties.getProperty(prefix + "difficulty", "NORMAL"),
                    intProperty(properties, prefix + "omen", 1),
                    intProperty(properties, prefix + "nativeSafeWaves", 7),
                    intProperty(properties, prefix + "logicalTargetWaves", 1),
                    intProperty(properties, prefix + "observedNativeTargetWaves", 0),
                    intProperty(properties, prefix + "maxObservedNativeGroupsSpawned", 0),
                    intProperty(properties, prefix + "customWavesSpawned", 0),
                    intProperty(properties, prefix + "customBaseNativeWaves", 0),
                    intProperty(properties, prefix + "activeCustomLogicalWave", -1),
                    booleanProperty(properties, prefix + "armedForExtraWaves", false),
                    booleanProperty(properties, prefix + "currentWaveActive", false),
                    booleanProperty(properties, prefix + "nativeRaidFinishedObserved", false),
                    booleanProperty(properties, prefix + "bridgeHoldActive", false),
                    booleanProperty(properties, prefix + "completed", false),
                    longProperty(properties, prefix + "firstSeenGameTime", 0L),
                    longProperty(properties, prefix + "lastSeenGameTime", 0L),
                    properties.getProperty(prefix + "nativeReinforcedWaves", ""),
                    properties.getProperty(prefix + "specialReinforcedWaves", ""),
                    booleanProperty(properties, prefix + "normalRaidsEnhancedSpecialDone", false),
                    intProperty(properties, prefix + "knownRaiderCacheWave", 0)
            );
        }

        void writeTo(Properties properties, String safeKey) {
            String prefix = "session." + safeKey + ".";
            properties.setProperty(prefix + "key", key == null ? "unknown" : key);
            properties.setProperty(prefix + "dimension", dimensionId == null ? "unknown" : dimensionId);
            properties.setProperty(prefix + "centerX", Integer.toString(centerX));
            properties.setProperty(prefix + "centerY", Integer.toString(centerY));
            properties.setProperty(prefix + "centerZ", Integer.toString(centerZ));
            properties.setProperty(prefix + "difficulty", difficultyName == null ? "NORMAL" : difficultyName);
            properties.setProperty(prefix + "omen", Integer.toString(omenLevel));
            properties.setProperty(prefix + "nativeSafeWaves", Integer.toString(nativeSafeWaves));
            properties.setProperty(prefix + "logicalTargetWaves", Integer.toString(logicalTargetWaves));
            properties.setProperty(prefix + "observedNativeTargetWaves", Integer.toString(observedNativeTargetWaves));
            properties.setProperty(prefix + "maxObservedNativeGroupsSpawned", Integer.toString(maxObservedNativeGroupsSpawned));
            properties.setProperty(prefix + "customWavesSpawned", Integer.toString(customWavesSpawned));
            properties.setProperty(prefix + "customBaseNativeWaves", Integer.toString(customBaseNativeWaves));
            properties.setProperty(prefix + "activeCustomLogicalWave", Integer.toString(activeCustomLogicalWave));
            properties.setProperty(prefix + "armedForExtraWaves", Boolean.toString(armedForExtraWaves));
            properties.setProperty(prefix + "currentWaveActive", Boolean.toString(currentWaveActive));
            properties.setProperty(prefix + "nativeRaidFinishedObserved", Boolean.toString(nativeRaidFinishedObserved));
            properties.setProperty(prefix + "bridgeHoldActive", Boolean.toString(bridgeHoldActive));
            properties.setProperty(prefix + "completed", Boolean.toString(completed));
            properties.setProperty(prefix + "firstSeenGameTime", Long.toString(firstSeenGameTime));
            properties.setProperty(prefix + "lastSeenGameTime", Long.toString(lastSeenGameTime));
            properties.setProperty(prefix + "nativeReinforcedWaves", nativeReinforcedWaves == null ? "" : nativeReinforcedWaves);
            properties.setProperty(prefix + "specialReinforcedWaves", specialReinforcedWaves == null ? "" : specialReinforcedWaves);
            properties.setProperty(prefix + "normalRaidsEnhancedSpecialDone", Boolean.toString(normalRaidsEnhancedSpecialDone));
            properties.setProperty(prefix + "knownRaiderCacheWave", Integer.toString(knownRaiderCacheWave));
        }

        private static String encodeIntSet(Set<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Integer value : values) {
            if (value == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static void copyEncodedIntSet(String encoded, Set<Integer> target) {
        if (encoded == null || encoded.isBlank() || target == null) {
            return;
        }
        for (String part : encoded.split(",")) {
            try {
                if (!part.isBlank()) {
                    target.add(Integer.parseInt(part.trim()));
                }
            } catch (NumberFormatException ignored) {
                // Ignore one corrupt entry, keep the rest of the lifecycle snapshot.
            }
        }
    }

    private static int intProperty(Properties properties, String key, int fallback) {
            try {
                return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
            } catch (RuntimeException exception) {
                return fallback;
            }
        }

        private static long longProperty(Properties properties, String key, long fallback) {
            try {
                return Long.parseLong(properties.getProperty(key, Long.toString(fallback)).trim());
            } catch (RuntimeException exception) {
                return fallback;
            }
        }

        private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return Boolean.parseBoolean(value.trim());
        }
    }

    private static int difficultyFactor(String difficultyName) {
        String difficulty = difficultyName == null ? "NORMAL" : difficultyName.toUpperCase(java.util.Locale.ROOT);
        if (difficulty.contains("HARD")) {
            return 2;
        }
        if (difficulty.contains("EASY")) {
            return 0;
        }
        return 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int invokeInt(Object target, String methodName, int fallback) {
        Object result = invokeNoArg(target, methodName);
        if (result instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static boolean invokeBoolean(Object target, String methodName) {
        Object result = invokeNoArg(target, methodName);
        return result instanceof Boolean bool && bool;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static List<?> playersSnapshot(ServerLevel level) {
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object result = playersMethod.invoke(level);
            if (result instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fail closed.
        }
        return List.of();
    }

    private static final class TimeoutAuditCounts {
        int remaining;
        int normal;
        int far;
        int stuck;
        int missing;
        int special;
        int located;
    }

    private static final class TimeoutAuditMovementRecord {
        int lastX;
        int lastY;
        int lastZ;
        long stillSinceGameTime;
        long lastAuditGameTime;

        TimeoutAuditMovementRecord(int lastX, int lastY, int lastZ, long gameTime) {
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
            this.stillSinceGameTime = gameTime;
            this.lastAuditGameTime = gameTime;
        }
    }

    private static final class ExtraWaveState {
        final String key;
        final String dimensionId;
        int centerX;
        int centerY;
        int centerZ;
        String difficultyName;
        int omenLevel;
        int nativeSafeWaves;
        int logicalTargetWaves;
        int observedNativeTargetWaves;
        int maxObservedNativeGroupsSpawned;
        int customWavesSpawned;
        int customBaseNativeWaves;
        int plannedCustomExtraWaves;
        int lastKnownNearbyRaiders;
        int spawnAnchorX;
        int spawnAnchorY;
        int spawnAnchorZ;
        int spawnAnchorWave = -1;
        int[][] spawnAnchors = new int[0][];
        int activeCustomLogicalWave = -1;
        final Set<Integer> nativeReinforcedWaves = new HashSet<>();
        final Set<Integer> specialReinforcedWaves = new HashSet<>();
        final Set<Integer> lowCountGlowingWaves = new HashSet<>();
        final Set<UUID> longGlowingSpecialEntityIds = new HashSet<>();
        boolean normalRaidsEnhancedSpecialDone;
        int knownRaiderCacheWave = -1;
        int lastKnownRaiderCacheCount;
        long lastKnownRaiderCacheScanGameTime;
        int lastWaveBudgetSeconds;
        int lastWaveRaiderWeightSeconds;
        long lastWaveTimeoutGameTime;
        long lastWaveTimeBudgetProcessGameTime;
        final Set<Integer> waveTimeoutWarnings = new HashSet<>();
        final Set<String> waveTimeDisplayMessageLocks = new HashSet<>();
        long lastWaveTimeDisplayActionbarGameTime;
        long lastWaveHudActionbarGameTime;
        long lastTimeoutRaiderAuditGameTime;
        long lastTimeoutRaiderAuditGlowGameTime;
        long lastTimeoutRaiderAuditMessageGameTime;
        long lastRaiderGlowingLocatorCheckGameTime;
        long lastSpecialRaiderIdentityScanGameTime;
        final Map<UUID, Boolean> specialRaiderIdentityCache = new LinkedHashMap<>();
        final Map<UUID, TimeoutAuditMovementRecord> timeoutAuditMovementRecords = new LinkedHashMap<>();
        int activeCombatPlayerCount;
        boolean clearClockPaused;
        long lastLifecyclePersistGameTime;
        boolean restoredFromLifecycleSnapshot;
        Object nativeRaid;
        boolean armedForExtraWaves;
        boolean currentWaveActive;
        boolean completed;
        boolean nativeRaidFinishedObserved;
        boolean protectionReleased;
        boolean bridgeHoldActive;
        int failedSpawnRetries;
        long firstSeenGameTime;
        long lastSeenGameTime;
        long nextActionGameTime;
        long lastCustomWaveSpawnedGameTime;
        long completedGameTime;
        long bridgeHoldStartedGameTime;
        long returnSensitiveFrozenUntilGameTime;

        ExtraWaveState(String key, String dimensionId, int centerX, int centerY, int centerZ,
                       String difficultyName, int omenLevel, int nativeSafeWaves, int logicalTargetWaves,
                       long gameTime) {
            this.key = key;
            this.dimensionId = dimensionId;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.difficultyName = difficultyName;
            this.omenLevel = omenLevel;
            this.nativeSafeWaves = nativeSafeWaves;
            this.logicalTargetWaves = logicalTargetWaves;
            this.observedNativeTargetWaves = 0;
            this.firstSeenGameTime = gameTime;
            this.lastSeenGameTime = gameTime;
        }

        int effectiveNativeWaves() {
            return RaidWaveAuthority.nativeWaveLimitForTarget(logicalTargetWaves);
        }

        boolean hasCustomExtraWaves() {
            return RaidWaveAuthority.hasCustomExtraWaves(logicalTargetWaves);
        }

        int extraWavesNeeded() {
            return RaidWaveAuthority.customWaveCount(logicalTargetWaves);
        }
    }
}
