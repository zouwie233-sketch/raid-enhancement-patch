package com.noah.raidenhancement.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Static configuration defaults plus a lightweight file loader for stage 0.8.9.6.
 *
 * Only the village-security layer is runtime-configurable in this stage. Older
 * raid expansion, HUD and extra-wave values remain fixed to avoid destabilizing
 * the tested bridge/return logic. Stage 0.8.9.6 keeps the hotfix baseline, then adds configurable battlefield deployment doctrine for village-security golems.
 */
public final class RaidEnhancementConfig {
    private RaidEnhancementConfig() {
    }

    public static final int SCHEMA_VERSION = 2;
    public static final boolean RAID_SESSION_ENABLED = true;
    public static final int SESSION_CLEANUP_INTERVAL_TICKS = 200;
    public static final int MAX_TRACKED_RAIDERS_PER_RAID = 256;
    public static final int MAX_PROTECTED_VILLAGERS_PER_RAID = 64;

    // Step 8.1: runtime known-raider cache. This is data collection only.
    // It must not teleport, clear, fail, or directly mutate vanilla Raid state.
    public static final boolean KNOWN_RAIDER_CACHE_ENABLED = true;
    public static final int KNOWN_RAIDER_CACHE_SCAN_INTERVAL_TICKS = 20;
    public static final int KNOWN_RAIDER_CACHE_SCAN_RADIUS = 128;
    public static final int KNOWN_RAIDER_CACHE_STALE_RECORD_TTL_TICKS = 2400;
    public static final int KNOWN_RAIDER_CACHE_INACTIVE_SESSION_TTL_TICKS = 12000;


    // Step 8.2: raider time weights and dynamic per-wave clear budget.
    // This stage records time budget and timeout state only. It does not teleport,
    // clear, fail, damage, or directly mutate the native Raid object.
    public static final boolean RAIDER_TIME_WEIGHT_ENABLED = true;
    public static final int RAIDER_TIME_WEIGHT_PILLAGER_SECONDS = 8;
    public static final int RAIDER_TIME_WEIGHT_VINDICATOR_SECONDS = 10;
    public static final int RAIDER_TIME_WEIGHT_WITCH_SECONDS = 18;
    public static final int RAIDER_TIME_WEIGHT_EVOKER_SECONDS = 28;
    public static final int RAIDER_TIME_WEIGHT_RAVAGER_SECONDS = 45;
    public static final int RAIDER_TIME_WEIGHT_RAID_CAPTAIN_BONUS_SECONDS = 8;
    public static final int RAIDER_TIME_WEIGHT_ZAPPER_SECONDS = 35;
    public static final int RAIDER_TIME_WEIGHT_GOLEM_OF_LAST_RESORT_SECONDS = 75;
    public static final int RAIDER_TIME_WEIGHT_RAID_BLIMP_SECONDS = 90;
    public static final int RAIDER_TIME_WEIGHT_RAID_DRILL_SECONDS = 110;
    public static final int RAIDER_TIME_WEIGHT_UNKNOWN_RAIDS_ENHANCED_SPECIAL_SECONDS = 60;
    public static final int RAIDER_TIME_WEIGHT_UNKNOWN_RAIDER_SECONDS = 10;

    public static final boolean WAVE_TIME_BUDGET_ENABLED = true;
    public static final int WAVE_TIME_BUDGET_RECALCULATE_INTERVAL_TICKS = 20;
    public static final int WAVE_TIME_BUDGET_TIMEOUT_EVALUATION_GRACE_TICKS = 1200;
    // Step 8.4: collect the current wave for a short window before locking the
    // initial budget. This prevents late cache discoveries of already-spawned
    // raiders from being treated as mid-wave reinforcements.
    public static final int WAVE_TIME_BUDGET_COLLECTION_TICKS = 160;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_1_TO_3 = 60;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_4_TO_6 = 90;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_7_TO_8 = 120;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_9 = 150;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_10 = 180;
    public static final int WAVE_TIME_BUDGET_POST_LOCK_EXTRA_SECONDS_WAVE_11_PLUS = 210;
    public static final boolean WAVE_TIME_BUDGET_TIMEOUT_CHAT_WARNING_ENABLED = true;
    public static final String WAVE_TIME_BUDGET_TIMEOUT_MESSAGE = "当前袭击波次清剿超时，已进入超时监控。";
    public static final int WAVE_TIME_BUDGET_SIDE_ATTACK_POINT_SECONDS = 20;
    public static final int WAVE_TIME_BUDGET_EXTRA_WAVE_9_SECONDS = 60;
    public static final int WAVE_TIME_BUDGET_EXTRA_WAVE_10_SECONDS = 90;
    public static final int WAVE_TIME_BUDGET_EXTRA_WAVE_11_PLUS_SECONDS = 120;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_1_TO_3 = 120;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_4_TO_6 = 150;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_7_TO_8 = 180;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_9 = 210;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_10 = 240;
    public static final int WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_11_PLUS = 270;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_1_TO_3 = 300;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_4_TO_6 = 420;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_7_TO_8 = 600;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_9 = 720;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_10 = 840;
    public static final int WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_11_PLUS = 960;
    public static final boolean WAVE_TIME_BUDGET_ENFORCE_FAILURE = false;




    // Step 8.8: active-combat clock gate. Clear time should measure active
    // battlefield participation, not time spent while every player is far away.
    // This layer pauses only the modded clear timer/audit workload after a grace
    // window; it does not pause vanilla Raid itself and never fails, teleports,
    // clears, damages, or mutates vanilla Raid state.
    public static final boolean ACTIVE_COMBAT_CLOCK_ENABLED = true;
    public static final int ACTIVE_COMBAT_CLOCK_RADIUS = 160;
    public static final int ACTIVE_COMBAT_CLOCK_ABSENCE_GRACE_TICKS = 600;
    public static final int ACTIVE_COMBAT_CLOCK_NO_PLAYER_CACHE_SCAN_INTERVAL_TICKS = 200;
    public static final boolean ACTIVE_COMBAT_CLOCK_CHAT_RESUME_ENABLED = true;

    // Step 8.7: timeout raider audit. This stage only classifies and reports
    // remaining raiders after a wave times out. It must not teleport, clear,
    // fail, damage, or directly mutate vanilla Raid state.
    public static final boolean TIMEOUT_RAIDER_AUDIT_ENABLED = true;
    public static final int TIMEOUT_RAIDER_AUDIT_INTERVAL_TICKS = 200;
    public static final int TIMEOUT_RAIDER_AUDIT_MESSAGE_INTERVAL_TICKS = 600;
    public static final int TIMEOUT_RAIDER_AUDIT_GLOW_INTERVAL_TICKS = 200;
    public static final int TIMEOUT_RAIDER_AUDIT_NORMAL_RADIUS = 128;
    public static final int TIMEOUT_RAIDER_AUDIT_FAR_RADIUS = 192;
    public static final int TIMEOUT_RAIDER_AUDIT_MISSING_GRACE_TICKS = 400;
    public static final int TIMEOUT_RAIDER_AUDIT_STUCK_STILL_TICKS = 600;
    public static final int TIMEOUT_RAIDER_AUDIT_STUCK_MOVEMENT_THRESHOLD_BLOCKS = 2;

    // Step 8.3: visible clear-timer display. Step 8.4 merges the timer into the top HUD by default; the actionbar path is retained but disabled. This is display-only and does not
    // enforce failure, teleport, clear, damage, or mutate the native Raid object.
    public static final boolean WAVE_TIME_DISPLAY_ENABLED = true;
    public static final boolean WAVE_TIME_DISPLAY_ACTIONBAR_ENABLED = false;
    public static final boolean WAVE_TIME_DISPLAY_WAVE_START_CHAT_ENABLED = false;
    public static final boolean WAVE_TIME_DISPLAY_WARNING_CHAT_ENABLED = false;
    public static final int WAVE_TIME_DISPLAY_ACTIONBAR_INTERVAL_TICKS = 20;
    public static final int WAVE_TIME_DISPLAY_WAVE_START_DELAY_TICKS = 180;
    public static final int WAVE_TIME_DISPLAY_RADIUS = 128;
    public static final int[] WAVE_TIME_DISPLAY_WARNING_THRESHOLDS_SECONDS = {60, 30, 10};



    // Step 8.8.8-0.8.9.1: village security.
    // Security golems are temporary per-wave village-defense units. This layer
    // does not replace vanilla raid failure and does not clear/teleport raiders.
    // 0.8.9.1 makes only this village-security layer configurable through:
    // config/raid_enhancement_patch/village_security.properties
    public static final String VILLAGE_SECURITY_CONFIG_STAGE = "0.8.9.6-village-security-deployment-doctrine";
    public static boolean VILLAGE_SECURITY_ENABLED = true;
    public static int VILLAGE_SECURITY_TICK_INTERVAL_TICKS = 20;
    public static boolean VILLAGE_SECURITY_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_INITIAL_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_REINFORCEMENT_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_REPAIR_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_WAVE_FAILURE_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_VICTORY_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_FAILURE_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_VICTORY_GRADE_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_ENABLED = true;
    public static float VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE = 2.0F;
    public static float VILLAGE_SECURITY_TIMEOUT_PENALTY_MIN_VILLAGER_HEALTH = 2.0F;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_ONCE_PER_WAVE = true;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_SKIP_IF_WAVE_DEFENSE_ALREADY_FAILED = false;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_REQUIRE_KNOWN_LIVING_RAIDERS = true;
    public static boolean VILLAGE_SECURITY_TIMEOUT_PENALTY_INCLUDE_IN_BATTLE_SUMMARY = true;
    public static boolean VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED = true;
    public static int VILLAGE_SECURITY_VILLAGER_CACHE_REFRESH_INTERVAL_TICKS = 100;
    public static int VILLAGE_SECURITY_GOLEM_GLOW_REFRESH_INTERVAL_TICKS = 80;
    public static int PERFORMANCE_RAIDER_CACHE_SCAN_INTERVAL_TICKS = 20;
    public static int PERFORMANCE_STRAGGLER_GLOW_INTERVAL_TICKS = 40;
    public static int PERFORMANCE_RAIDER_AUDIT_INTERVAL_TICKS = 200;
    public static int PERFORMANCE_SPECIAL_RAIDER_SCAN_INTERVAL_TICKS = 100;
    public static int PERFORMANCE_TIMEOUT_PENALTY_CHECK_INTERVAL_TICKS = 20;
    public static int PERFORMANCE_SECURITY_GOLEM_RETURN_GRACE_TICKS = 200;
    public static int PERFORMANCE_SECURITY_GOLEM_MISSING_PRUNE_GRACE_TICKS = 1200;
    public static boolean PERFORMANCE_ACTIVE_SESSION_FAST_CHECK_ENABLED = true;
    public static boolean PERFORMANCE_CLEANUP_ENDED_SESSIONS_AGGRESSIVELY = true;
    public static int VILLAGE_SECURITY_RADIUS = 96;
    public static int VILLAGE_SECURITY_VILLAGER_PENALTY_RADIUS = 128;
    public static int VILLAGE_SECURITY_EASY_VILLAGERS_PER_GOLEM = 5;
    public static int VILLAGE_SECURITY_NORMAL_VILLAGERS_PER_GOLEM = 8;
    public static int VILLAGE_SECURITY_HARD_VILLAGERS_PER_GOLEM = 10;
    public static int VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE = 1;
    public static int VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE = 5;
    public static float VILLAGE_SECURITY_BREACH_DAMAGE = 4.0F;
    public static int VILLAGE_SECURITY_GOLEM_GLOW_DURATION_TICKS = 160;
    public static int VILLAGE_SECURITY_GOLEM_SPAWN_RING_RADIUS = 12;
    public static int VILLAGE_SECURITY_GOLEM_SPAWN_POINT_COUNT = 8;
    public static boolean VILLAGE_SECURITY_REDEPLOY_EXISTING_GOLEMS_ON_WAVE_START = true;
    public static int VILLAGE_SECURITY_GOLEM_REDEPLOY_RING_RADIUS = 14;
    public static boolean VILLAGE_SECURITY_REPAIR_SURVIVORS_ON_WAVE_START = true;
    public static boolean VILLAGE_SECURITY_SURVIVOR_REPAIR_TO_FULL = true;
    public static float VILLAGE_SECURITY_SURVIVOR_REPAIR_HEALTH = 10.0F;
    public static boolean VILLAGE_SECURITY_GOLEM_RESISTANCE_ENABLED = true;
    public static int VILLAGE_SECURITY_GOLEM_RESISTANCE_LEVEL = 1;
    public static int VILLAGE_SECURITY_GOLEM_RESISTANCE_DURATION_TICKS = 1200;
    public static boolean VILLAGE_SECURITY_REDEPLOYMENT_MESSAGES_ENABLED = true;
    public static boolean VILLAGE_SECURITY_COMBAT_BUFF_MESSAGES_ENABLED = true;
    public static int VILLAGE_SECURITY_VICTORY_GOLEMS_EASY = 1;
    public static int VILLAGE_SECURITY_VICTORY_GOLEMS_NORMAL = 2;
    public static int VILLAGE_SECURITY_VICTORY_GOLEMS_HARD = 3;
    public static boolean VILLAGE_SECURITY_DISABLE_OLD_REGEN_ABSORPTION = true;
    public static boolean VILLAGE_SECURITY_ALLOW_PLAYER_DAMAGE_TO_VILLAGE_GOLEMS_DURING_RAID = false;

    // Step 8.9.2 polish/debug switches. Detailed debug logs are disabled by
    // default so normal gameplay only receives chat feedback, not console spam.
    public static boolean VILLAGE_SECURITY_DEBUG_LOGS_ENABLED = false;
    public static boolean VILLAGE_SECURITY_DEBUG_WAVE_PREPARATION = false;
    public static boolean VILLAGE_SECURITY_DEBUG_BREACH = false;
    public static boolean VILLAGE_SECURITY_DEBUG_COMPLETION = false;
    public static boolean VILLAGE_SECURITY_DEBUG_GOLEM_TRIM = false;
    public static boolean VILLAGE_SECURITY_DEBUG_TIMEOUT_PENALTY = false;
    public static boolean VILLAGE_SECURITY_DEBUG_PERFORMANCE = false;

    // Step 8.8.9 strict village-security rules. These two layers preserve the
    // tested security-golem pressure model while preventing outside healing/extra
    // ordinary golems from erasing that pressure.
    public static boolean VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED = true;
    public static int VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS = 24000;
    public static boolean VILLAGE_SECURITY_TRIM_ORDINARY_GOLEMS_ON_RAID_START = true;
    public static int VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_EASY = 3;
    public static int VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_NORMAL = 2;
    public static int VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_HARD = 1;

    public static final boolean VILLAGER_PROTECTION_ENABLED = true;
    public static final boolean VILLAGER_PROTECTION_DEBUG_COMMANDS_ENABLED = false;
    public static final int VILLAGER_PROTECTION_REFRESH_INTERVAL_TICKS = 100;
    public static final int VILLAGER_PROTECTION_SWEEP_INTERVAL_TICKS = 200;
    public static final int VILLAGER_PROTECTION_DEFAULT_DURATION_SECONDS = 180;
    public static final int VILLAGER_PROTECTION_DEFAULT_RADIUS = 48;
    public static final int VILLAGER_PROTECTION_MAX_RADIUS = 128;

    // Vanilla effect levels are zero-based amplifiers. 4 = Resistance V.
    public static final int PROTECTED_VILLAGER_RESISTANCE_AMPLIFIER = 4;
    public static final int PROTECTED_VILLAGER_REGENERATION_AMPLIFIER = 2;
    public static final int PROTECTED_VILLAGER_ABSORPTION_AMPLIFIER = 19;
    public static final int PROTECTED_VILLAGER_EFFECT_DURATION_TICKS = 300;

    // Stage 0.3.0 makes protected villagers fully invulnerable while protected.
    // Player attacks are also cancelled earlier through AttackEntityEvent.
    public static final boolean PROTECTED_VILLAGERS_CANCEL_ALL_DAMAGE = true;

    // 0.3.9 regression fix: restore the user-tested visible backup effects,
    // but keep them behind explicit constants so later builds can disable them if needed.
    public static final boolean PROTECTED_VILLAGER_EFFECTS_ENABLED = true;
    public static final boolean PROTECTED_VILLAGER_REMOVE_EFFECTS_ON_RELEASE = true;

    public static final boolean AUTO_RAID_VILLAGER_PROTECTION_ENABLED = true;
    public static final int AUTO_RAID_VILLAGER_SCAN_INTERVAL_TICKS = 100;
    public static final int AUTO_RAID_VILLAGER_RELEASE_INTERVAL_TICKS = 100;
    public static final int AUTO_RAID_VILLAGER_SCAN_RADIUS = 96;
    public static final int AUTO_RAID_VILLAGER_PROTECTION_DURATION_TICKS = 400;
    public static final int AUTO_RAID_MAX_VILLAGERS_SCANNED_PER_SWEEP = 256;
    public static final int AUTO_RAID_RAIDER_FALLBACK_RADIUS = 96;

    public static final boolean WAVE_EXPANSION_ENABLED = true;
    public static final int WAVE_EXPANSION_REFRESH_INTERVAL_TICKS = 1;
    public static final int WAVE_EXPANSION_POSITION_SAMPLE_RADIUS = 64;
    public static final int WAVE_EXPANSION_MAX_RAIDS_PER_SWEEP = 16;

    // Vanilla 1.21.1 raid spawn-count tables contain 8 public wave slots.
    // 0.8.9.8.4: keep native waves 1-8 native and reserve the custom bridge only
    // for logical waves 9-11. The RaidWaveIndexClampMixin remains as the crash guard
    // if a runtime path passes a table index beyond the vanilla array.
    public static final boolean WAVE_EXPANSION_CAP_NATIVE_RAID_WAVES = true;
    public static final int WAVE_EXPANSION_NATIVE_RAID_SAFE_MAX_WAVES = 8;
    public static final boolean MULTI_SPAWN_POINTS_ENABLED = true;
    public static final boolean EXTRA_WAVE_LAYER_ENABLED = true;
    public static final int EXTRA_WAVE_CHECK_INTERVAL_TICKS = 1;
    public static final int EXTRA_WAVE_PROCESS_INTERVAL_TICKS = 1;

    // 0.4.21 performance throttle: keep the 0.4.20 fast response only when it matters.
    // Idle worlds do not need a full raid discovery sweep every tick. Once a raid is
    // discovered, the first few seconds and the continuous-assault bridge stay fast;
    // stable middle waves use a lighter cadence.
    public static final int EXTRA_WAVE_IDLE_DISCOVERY_INTERVAL_TICKS = 20;
    public static final int EXTRA_WAVE_STABLE_DISCOVERY_INTERVAL_TICKS = 5;
    public static final int EXTRA_WAVE_STARTUP_FAST_DISCOVERY_INTERVAL_TICKS = 1;
    public static final int EXTRA_WAVE_CONTINUOUS_ASSAULT_DISCOVERY_INTERVAL_TICKS = 2;
    public static final int EXTRA_WAVE_STARTUP_FAST_DURATION_TICKS = 100;
    public static final int EXTRA_WAVE_STABLE_PROCESS_INTERVAL_TICKS = 5;
    public static final int EXTRA_WAVE_CONTINUOUS_ASSAULT_PROCESS_INTERVAL_TICKS = 1;
    public static final int EXTRA_WAVE_FIRST_DELAY_TICKS = 0;
    public static final int EXTRA_WAVE_BETWEEN_DELAY_TICKS = 140;
    public static final int EXTRA_WAVE_MIN_ACTIVE_TICKS = 100;
    public static final int EXTRA_WAVE_SCAN_RADIUS = 112;
    public static final int EXTRA_WAVE_MAX_ACTIVE_SESSIONS = 16;
    public static final int EXTRA_WAVE_MAX_MOBS_PER_WAVE = 64;
    public static final int EXTRA_WAVE_SPAWN_RING_RADIUS = 36;
    public static final int EXTRA_WAVE_CLUSTER_RADIUS = 4;
    public static final int EXTRA_WAVE_SPAWN_ANCHOR_RADIUS = 56;

    // Step 5 preview: only the custom extra waves use multi-spawn points.
    // Vanilla native waves 1-8 remain untouched. Each point is still a small
    // concentrated squad, and every raider still goes through finalizeSpawn + joinRaid.
    public static final boolean EXTRA_WAVE_MULTI_SPAWN_POINTS_ENABLED = true;
    public static final int EXTRA_WAVE_MULTI_SPAWN_MIN_POINTS = 2;
    public static final int EXTRA_WAVE_MULTI_SPAWN_MAX_POINTS = 6;
    public static final int EXTRA_WAVE_MULTI_SPAWN_MIN_ANCHOR_DISTANCE = 18;

    // 0.5.1: the main attack point keeps the full original extra-wave composition.
    // Secondary points no longer split that composition; each secondary point adds
    // an independent reinforcement squad on top of the original wave size.
    public static final boolean EXTRA_WAVE_SECONDARY_REINFORCEMENTS_ENABLED = true;
    public static final int EXTRA_WAVE_SECONDARY_REINFORCEMENT_SIZE = 6;

    // 0.5.3: difficulty-scaled side reinforcement points. Vanilla native waves
    // remain under native Raid control; these are additive side squads only.
    // Easy has no side points. Normal starts from the middle wave for each omen
    // target and caps at 2 secondary points. Hard starts at wave 1 and scales up
    // to as many as 5 secondary points on high-omen raids. Each wave still has
    // an independent lock in RaidExtraWaveController so squads cannot repeat.
    public static final boolean NATIVE_WAVE_REINFORCEMENT_POINTS_ENABLED = true;
    public static final int NATIVE_WAVE_REINFORCEMENT_MIN_WAVE = 1;
    public static final int NATIVE_WAVE_REINFORCEMENT_MAX_WAVE = 8;
    public static final int NATIVE_WAVE_REINFORCEMENT_EASY_MAX_POINTS = 0;
    public static final int NATIVE_WAVE_REINFORCEMENT_NORMAL_MAX_POINTS = 2;
    public static final int NATIVE_WAVE_REINFORCEMENT_HARD_MAX_POINTS = 5;
    public static final int NATIVE_WAVE_REINFORCEMENT_WAVE_1_TO_3_SIZE = 3;
    public static final int NATIVE_WAVE_REINFORCEMENT_WAVE_4_TO_6_SIZE = 4;
    public static final int NATIVE_WAVE_REINFORCEMENT_WAVE_7_TO_8_SIZE = 5;

    public static final boolean EXTRA_WAVE_PREFER_NATIVE_SPAWN_ANCHOR = true;
    public static final int RAID_OMEN_RUNTIME_LEVEL_OFFSET = 0;
    public static final boolean RAID_WAVE_HUD_ENABLED = true;
    public static final int RAID_WAVE_HUD_TOP_Y = 22;
    public static final int RAID_WAVE_HUD_STALE_TICKS = 400;
    // Step 8.6: HUD lifecycle. The server keeps active raid snapshots stable;
    // the client hides them by dimension/range and self-clears if no fresh matching
    // snapshot arrives. The clear radius is slightly larger than the visible radius
    // to avoid edge flicker.
    public static final int RAID_WAVE_HUD_VISIBLE_RADIUS = 128;
    public static final int RAID_WAVE_HUD_CLEAR_RADIUS = 144;
    public static final int RAID_WAVE_HUD_CLIENT_STALE_TICKS = 120;
    public static final boolean RAID_WAVE_HUD_SELECT_NEAREST_RAID = true;

    // Step 8.5: lightweight lifecycle recovery. This sidecar file is intentionally
    // conservative: it only restores stable session metadata such as difficulty,
    // omen level, total waves and extra-wave progress. It does not restore mobs,
    // teleport anything, clear anything, or mutate the vanilla Raid failure state.
    public static final boolean RAID_SESSION_LIFECYCLE_PERSISTENCE_ENABLED = true;
    public static final int RAID_SESSION_LIFECYCLE_PERSIST_INTERVAL_TICKS = 200;
    public static final int RAID_SESSION_LIFECYCLE_RESTORE_TTL_TICKS = 24000;
    public static final String RAID_SESSION_LIFECYCLE_PERSISTENCE_FILE = "config/raid_enhancement_patch/raid_session_lifecycle.properties";

    public static final int EXTRA_WAVE_BRIDGE_HUD_HOLD_TICKS = 1200;
    public static final int EXTRA_WAVE_FAILED_SPAWN_RETRY_DELAY_TICKS = 20;
    public static final int EXTRA_WAVE_FAILED_SPAWN_MAX_RETRIES = 12;
    public static final boolean EXTRA_WAVE_USE_COMMAND_SUMMON = false;
    public static final boolean EXTRA_WAVE_SUMMON_PERSISTENT_RAIDERS = true;
    public static final boolean EXTRA_WAVE_JOIN_NATIVE_RAID = true;
    public static final boolean EXTRA_WAVE_FINALIZE_SPAWN = true;
    public static final boolean EXTRA_WAVE_EQUIP_FALLBACK_WEAPONS = true;
    public static final boolean EXTRA_WAVE_COUNT_NATIVE_OMINOUS_BONUS_WAVE = true;
    public static final int EXTRA_WAVE_ARM_AFTER_NATIVE_DONE_GRACE_TICKS = 20;
    public static final int EXTRA_WAVE_VILLAGER_PROTECTION_DURATION_TICKS = 500;
    public static final String EXTRA_WAVE_VILLAGER_PROTECTION_SOURCE = "extra_wave_protection";

    public static final boolean RAIDS_ENHANCED_SPECIALS_ENABLED = true;

    // Step 6: additive Raids Enhanced special raider reinforcements.
    // This patch does not block, move, or replace Raids Enhanced's own RaidMixin.
    // It only adds extra special raiders at selected waves, with per-raid/per-wave
    // locks in RaidExtraWaveController so repeated ticks cannot duplicate them.
    public static final int RAIDS_ENHANCED_SPECIAL_NORMAL_EXTRA_PER_RAID = 1;
    public static final int RAIDS_ENHANCED_SPECIAL_HARD_WAVE_7_COUNT = 2;
    public static final int RAIDS_ENHANCED_SPECIAL_HARD_WAVE_8_COUNT = 3;
    public static final int RAIDS_ENHANCED_SPECIAL_HARD_WAVE_9_COUNT = 4;
    public static final int RAIDS_ENHANCED_SPECIAL_HARD_WAVE_10_COUNT = 6;
    public static final int RAIDS_ENHANCED_SPECIAL_HARD_WAVE_11_COUNT = 8;
    public static final int RAIDS_ENHANCED_SPECIAL_BLIMP_CAP_WAVE_7_TO_8 = 1;
    public static final int RAIDS_ENHANCED_SPECIAL_BLIMP_CAP_WAVE_9_TO_11 = 2;
    public static final boolean RAIDS_ENHANCED_SPECIAL_BLIMP_NATIVE_INIT = true;
    public static final boolean RAIDER_GLOWING_LOCATOR_ENABLED = true;
    public static final int RAIDER_GLOWING_LOW_COUNT_THRESHOLD = 5;
    public static final int RAIDER_GLOWING_LOW_COUNT_DURATION_TICKS = 600;
    public static final int RAIDER_GLOWING_SPECIAL_DURATION_TICKS = 36000;
    public static final int RAIDER_GLOWING_SCAN_RADIUS = 128;
    public static final String RAIDER_GLOWING_LOW_COUNT_MESSAGE = "残兵已被锁定";


    public static void loadOrCreate() {
        Path configDir = Path.of("config", "raid_enhancement_patch");
        Path configFile = configDir.resolve("village_security.properties");
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(configFile)) {
                writeDefaultVillageSecurityConfig(configFile);
                System.out.println("[Raid Enhancement Patch] Created default village security config: " + configFile.toAbsolutePath());
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            VILLAGE_SECURITY_ENABLED = readBool(properties, "enabled", VILLAGE_SECURITY_ENABLED);
            VILLAGE_SECURITY_TICK_INTERVAL_TICKS = readInt(properties, "tickIntervalTicks", VILLAGE_SECURITY_TICK_INTERVAL_TICKS, 1, 200);
            VILLAGE_SECURITY_MESSAGES_ENABLED = readBool(properties, "messages.enabled", VILLAGE_SECURITY_MESSAGES_ENABLED);
            VILLAGE_SECURITY_INITIAL_MESSAGES_ENABLED = readBool(properties, "messages.initialEntry", VILLAGE_SECURITY_INITIAL_MESSAGES_ENABLED);
            VILLAGE_SECURITY_REINFORCEMENT_MESSAGES_ENABLED = readBool(properties, "messages.reinforcement", VILLAGE_SECURITY_REINFORCEMENT_MESSAGES_ENABLED);
            VILLAGE_SECURITY_REPAIR_MESSAGES_ENABLED = readBool(properties, "messages.repair", VILLAGE_SECURITY_REPAIR_MESSAGES_ENABLED);
            VILLAGE_SECURITY_WAVE_FAILURE_MESSAGES_ENABLED = readBool(properties, "messages.waveDefenseFailure", VILLAGE_SECURITY_WAVE_FAILURE_MESSAGES_ENABLED);
            VILLAGE_SECURITY_VICTORY_MESSAGES_ENABLED = readBool(properties, "messages.victory", VILLAGE_SECURITY_VICTORY_MESSAGES_ENABLED);
            VILLAGE_SECURITY_FAILURE_MESSAGES_ENABLED = readBool(properties, "messages.failure", VILLAGE_SECURITY_FAILURE_MESSAGES_ENABLED);
            VILLAGE_SECURITY_VICTORY_GRADE_MESSAGES_ENABLED = readBool(properties, "messages.victoryGrade", VILLAGE_SECURITY_VICTORY_GRADE_MESSAGES_ENABLED);
            VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED = readBool(properties, "messages.battleSummary", VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_MESSAGES_ENABLED = readBool(properties, "messages.timeoutPenalty", VILLAGE_SECURITY_TIMEOUT_PENALTY_MESSAGES_ENABLED);
            VILLAGE_SECURITY_REDEPLOYMENT_MESSAGES_ENABLED = readBool(properties, "messages.redeployment", VILLAGE_SECURITY_REDEPLOYMENT_MESSAGES_ENABLED);
            VILLAGE_SECURITY_COMBAT_BUFF_MESSAGES_ENABLED = readBool(properties, "messages.combatBuff", VILLAGE_SECURITY_COMBAT_BUFF_MESSAGES_ENABLED);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_ENABLED = readBool(properties, "timeoutPenalty.enabled", VILLAGE_SECURITY_TIMEOUT_PENALTY_ENABLED);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE = readFloat(properties, "timeoutPenalty.damage", VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE, 0.0F, 20.0F);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_MIN_VILLAGER_HEALTH = readFloat(properties, "timeoutPenalty.minVillagerHealth", VILLAGE_SECURITY_TIMEOUT_PENALTY_MIN_VILLAGER_HEALTH, 0.0F, 20.0F);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_ONCE_PER_WAVE = readBool(properties, "timeoutPenalty.oncePerWave", VILLAGE_SECURITY_TIMEOUT_PENALTY_ONCE_PER_WAVE);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_SKIP_IF_WAVE_DEFENSE_ALREADY_FAILED = readBool(properties, "timeoutPenalty.skipIfWaveDefenseAlreadyFailed", VILLAGE_SECURITY_TIMEOUT_PENALTY_SKIP_IF_WAVE_DEFENSE_ALREADY_FAILED);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_REQUIRE_KNOWN_LIVING_RAIDERS = readBool(properties, "timeoutPenalty.requireKnownLivingRaiders", VILLAGE_SECURITY_TIMEOUT_PENALTY_REQUIRE_KNOWN_LIVING_RAIDERS);
            VILLAGE_SECURITY_TIMEOUT_PENALTY_INCLUDE_IN_BATTLE_SUMMARY = readBool(properties, "timeoutPenalty.includeInBattleSummary", VILLAGE_SECURITY_TIMEOUT_PENALTY_INCLUDE_IN_BATTLE_SUMMARY);
            VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED = readBool(properties, "performanceOptimization.enabled", VILLAGE_SECURITY_PERFORMANCE_OPTIMIZATION_ENABLED);
            VILLAGE_SECURITY_VILLAGER_CACHE_REFRESH_INTERVAL_TICKS = readInt(properties, "performanceOptimization.villagerCacheRefreshIntervalTicks", VILLAGE_SECURITY_VILLAGER_CACHE_REFRESH_INTERVAL_TICKS, 20, 1200);
            VILLAGE_SECURITY_GOLEM_GLOW_REFRESH_INTERVAL_TICKS = readInt(properties, "performanceOptimization.golemGlowRefreshIntervalTicks", VILLAGE_SECURITY_GOLEM_GLOW_REFRESH_INTERVAL_TICKS, 20, 1200);
            PERFORMANCE_RAIDER_CACHE_SCAN_INTERVAL_TICKS = readInt(properties, "performanceOptimization.raiderCacheScanIntervalTicks", PERFORMANCE_RAIDER_CACHE_SCAN_INTERVAL_TICKS, 5, 1200);
            PERFORMANCE_STRAGGLER_GLOW_INTERVAL_TICKS = readInt(properties, "performanceOptimization.stragglerGlowIntervalTicks", PERFORMANCE_STRAGGLER_GLOW_INTERVAL_TICKS, 20, 1200);
            PERFORMANCE_RAIDER_AUDIT_INTERVAL_TICKS = readInt(properties, "performanceOptimization.raiderAuditIntervalTicks", PERFORMANCE_RAIDER_AUDIT_INTERVAL_TICKS, 20, 2400);
            PERFORMANCE_SPECIAL_RAIDER_SCAN_INTERVAL_TICKS = readInt(properties, "performanceOptimization.specialRaiderScanIntervalTicks", PERFORMANCE_SPECIAL_RAIDER_SCAN_INTERVAL_TICKS, 20, 2400);
            PERFORMANCE_TIMEOUT_PENALTY_CHECK_INTERVAL_TICKS = readInt(properties, "performanceOptimization.timeoutPenaltyCheckIntervalTicks", PERFORMANCE_TIMEOUT_PENALTY_CHECK_INTERVAL_TICKS, 10, 1200);
            PERFORMANCE_SECURITY_GOLEM_RETURN_GRACE_TICKS = readInt(properties, "performanceOptimization.securityGolemReturnGraceTicks", PERFORMANCE_SECURITY_GOLEM_RETURN_GRACE_TICKS, 0, 2400);
            PERFORMANCE_SECURITY_GOLEM_MISSING_PRUNE_GRACE_TICKS = readInt(properties, "performanceOptimization.securityGolemMissingPruneGraceTicks", PERFORMANCE_SECURITY_GOLEM_MISSING_PRUNE_GRACE_TICKS, 200, 72000);
            PERFORMANCE_ACTIVE_SESSION_FAST_CHECK_ENABLED = readBool(properties, "performanceOptimization.activeSessionFastCheck", PERFORMANCE_ACTIVE_SESSION_FAST_CHECK_ENABLED);
            PERFORMANCE_CLEANUP_ENDED_SESSIONS_AGGRESSIVELY = readBool(properties, "performanceOptimization.cleanupEndedSessionsAggressively", PERFORMANCE_CLEANUP_ENDED_SESSIONS_AGGRESSIVELY);
            VILLAGE_SECURITY_RADIUS = readInt(properties, "radius", VILLAGE_SECURITY_RADIUS, 16, 256);
            VILLAGE_SECURITY_VILLAGER_PENALTY_RADIUS = readInt(properties, "strictRules.villagerPenaltyRadius", VILLAGE_SECURITY_VILLAGER_PENALTY_RADIUS, 16, 384);
            VILLAGE_SECURITY_EASY_VILLAGERS_PER_GOLEM = readInt(properties, "securityGolems.villagersPerGolem.easy", VILLAGE_SECURITY_EASY_VILLAGERS_PER_GOLEM, 1, 128);
            VILLAGE_SECURITY_NORMAL_VILLAGERS_PER_GOLEM = readInt(properties, "securityGolems.villagersPerGolem.normal", VILLAGE_SECURITY_NORMAL_VILLAGERS_PER_GOLEM, 1, 128);
            VILLAGE_SECURITY_HARD_VILLAGERS_PER_GOLEM = readInt(properties, "securityGolems.villagersPerGolem.hard", VILLAGE_SECURITY_HARD_VILLAGERS_PER_GOLEM, 1, 128);
            VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE = readInt(properties, "securityGolems.minPerWave", VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE, 0, 32);
            VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE = readInt(properties, "securityGolems.maxPerWave", VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE, 0, 32);
            if (VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE < VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE) {
                VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE = VILLAGE_SECURITY_MIN_GOLEMS_PER_WAVE;
            }
            VILLAGE_SECURITY_BREACH_DAMAGE = readFloat(properties, "breachDamage", VILLAGE_SECURITY_BREACH_DAMAGE, 0.0F, 20.0F);
            VILLAGE_SECURITY_GOLEM_GLOW_DURATION_TICKS = readInt(properties, "securityGolems.glowDurationTicks", VILLAGE_SECURITY_GOLEM_GLOW_DURATION_TICKS, 20, 72000);
            VILLAGE_SECURITY_GOLEM_SPAWN_RING_RADIUS = readInt(properties, "securityGolems.spawnRingRadius", VILLAGE_SECURITY_GOLEM_SPAWN_RING_RADIUS, 1, 64);
            VILLAGE_SECURITY_GOLEM_SPAWN_POINT_COUNT = readInt(properties, "securityGolems.spawnPointCount", VILLAGE_SECURITY_GOLEM_SPAWN_POINT_COUNT, 1, 32);
            VILLAGE_SECURITY_REDEPLOY_EXISTING_GOLEMS_ON_WAVE_START = readBool(properties, "securityGolems.redeployExistingAtWaveStart", VILLAGE_SECURITY_REDEPLOY_EXISTING_GOLEMS_ON_WAVE_START);
            VILLAGE_SECURITY_GOLEM_REDEPLOY_RING_RADIUS = readInt(properties, "securityGolems.redeployRingRadius", VILLAGE_SECURITY_GOLEM_REDEPLOY_RING_RADIUS, 1, 96);
            VILLAGE_SECURITY_REPAIR_SURVIVORS_ON_WAVE_START = readBool(properties, "securityGolems.repairSurvivorsOnWaveStart", VILLAGE_SECURITY_REPAIR_SURVIVORS_ON_WAVE_START);
            VILLAGE_SECURITY_SURVIVOR_REPAIR_TO_FULL = readBool(properties, "securityGolems.survivorRepairToFull", VILLAGE_SECURITY_SURVIVOR_REPAIR_TO_FULL);
            VILLAGE_SECURITY_SURVIVOR_REPAIR_HEALTH = readFloat(properties, "securityGolems.survivorRepairHealth", VILLAGE_SECURITY_SURVIVOR_REPAIR_HEALTH, 0.0F, 200.0F);
            VILLAGE_SECURITY_GOLEM_RESISTANCE_ENABLED = readBool(properties, "securityGolems.resistance.enabled", VILLAGE_SECURITY_GOLEM_RESISTANCE_ENABLED);
            VILLAGE_SECURITY_GOLEM_RESISTANCE_LEVEL = readInt(properties, "securityGolems.resistance.level", VILLAGE_SECURITY_GOLEM_RESISTANCE_LEVEL, 1, 5);
            VILLAGE_SECURITY_GOLEM_RESISTANCE_DURATION_TICKS = readInt(properties, "securityGolems.resistance.durationTicks", VILLAGE_SECURITY_GOLEM_RESISTANCE_DURATION_TICKS, 20, 72000);
            VILLAGE_SECURITY_VICTORY_GOLEMS_EASY = readInt(properties, "victoryOrdinaryGolems.easy", VILLAGE_SECURITY_VICTORY_GOLEMS_EASY, 0, 16);
            VILLAGE_SECURITY_VICTORY_GOLEMS_NORMAL = readInt(properties, "victoryOrdinaryGolems.normal", VILLAGE_SECURITY_VICTORY_GOLEMS_NORMAL, 0, 16);
            VILLAGE_SECURITY_VICTORY_GOLEMS_HARD = readInt(properties, "victoryOrdinaryGolems.hard", VILLAGE_SECURITY_VICTORY_GOLEMS_HARD, 0, 16);
            VILLAGE_SECURITY_DEBUG_LOGS_ENABLED = readBool(properties, "debug.enabled", VILLAGE_SECURITY_DEBUG_LOGS_ENABLED);
            VILLAGE_SECURITY_DEBUG_WAVE_PREPARATION = readBool(properties, "debug.wavePreparation", VILLAGE_SECURITY_DEBUG_WAVE_PREPARATION);
            VILLAGE_SECURITY_DEBUG_BREACH = readBool(properties, "debug.breach", VILLAGE_SECURITY_DEBUG_BREACH);
            VILLAGE_SECURITY_DEBUG_COMPLETION = readBool(properties, "debug.completion", VILLAGE_SECURITY_DEBUG_COMPLETION);
            VILLAGE_SECURITY_DEBUG_GOLEM_TRIM = readBool(properties, "debug.golemTrim", VILLAGE_SECURITY_DEBUG_GOLEM_TRIM);
            VILLAGE_SECURITY_DEBUG_TIMEOUT_PENALTY = readBool(properties, "debug.timeoutPenalty", VILLAGE_SECURITY_DEBUG_TIMEOUT_PENALTY);
            VILLAGE_SECURITY_DEBUG_PERFORMANCE = readBool(properties, "debug.performance", VILLAGE_SECURITY_DEBUG_PERFORMANCE);
            VILLAGE_SECURITY_ALLOW_PLAYER_DAMAGE_TO_VILLAGE_GOLEMS_DURING_RAID = readBool(properties, "strictRules.allowPlayerDamageToVillageGolemsDuringRaid", VILLAGE_SECURITY_ALLOW_PLAYER_DAMAGE_TO_VILLAGE_GOLEMS_DURING_RAID);
            VILLAGE_SECURITY_DISABLE_OLD_REGEN_ABSORPTION = readBool(properties, "strictRules.disableOldRegenAbsorption", VILLAGE_SECURITY_DISABLE_OLD_REGEN_ABSORPTION);
            VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED = readBool(properties, "strictRules.villagerHealthClampEnabled", VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED);
            VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS = readInt(properties, "strictRules.healthClampDurationTicks", VILLAGE_SECURITY_HEALTH_CLAMP_DURATION_TICKS, 200, 240000);
            VILLAGE_SECURITY_TRIM_ORDINARY_GOLEMS_ON_RAID_START = readBool(properties, "strictRules.trimOrdinaryGolemsOnRaidStart", VILLAGE_SECURITY_TRIM_ORDINARY_GOLEMS_ON_RAID_START);
            VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_EASY = readInt(properties, "strictRules.keepOrdinaryUnnamedGolems.easy", VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_EASY, 0, 32);
            VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_NORMAL = readInt(properties, "strictRules.keepOrdinaryUnnamedGolems.normal", VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_NORMAL, 0, 32);
            VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_HARD = readInt(properties, "strictRules.keepOrdinaryUnnamedGolems.hard", VILLAGE_SECURITY_KEEP_ORDINARY_GOLEMS_HARD, 0, 32);

            writeDefaultVillageSecurityConfig(configDir.resolve("village_security.default.properties"));
            System.out.println("[Raid Enhancement Patch] Loaded village security config " + VILLAGE_SECURITY_CONFIG_STAGE
                    + ": enabled=" + VILLAGE_SECURITY_ENABLED
                    + ", messages=" + VILLAGE_SECURITY_MESSAGES_ENABLED
                    + ", clamp=" + VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED
                    + ", trimOrdinaryGolems=" + VILLAGE_SECURITY_TRIM_ORDINARY_GOLEMS_ON_RAID_START
                    + ", maxSecurityGolems=" + VILLAGE_SECURITY_MAX_GOLEMS_PER_WAVE
                    + ", victoryGradeMessages=" + VILLAGE_SECURITY_VICTORY_GRADE_MESSAGES_ENABLED
                    + ", battleSummaryMessages=" + VILLAGE_SECURITY_BATTLE_SUMMARY_MESSAGES_ENABLED
                    + ", timeoutPenalty=" + VILLAGE_SECURITY_TIMEOUT_PENALTY_ENABLED
                    + ", timeoutPenaltyDamage=" + VILLAGE_SECURITY_TIMEOUT_PENALTY_DAMAGE
                    + ", raiderCacheScanInterval=" + PERFORMANCE_RAIDER_CACHE_SCAN_INTERVAL_TICKS
                    + ", stragglerGlowInterval=" + PERFORMANCE_STRAGGLER_GLOW_INTERVAL_TICKS
                    + ", raiderAuditInterval=" + PERFORMANCE_RAIDER_AUDIT_INTERVAL_TICKS
                    + ", allowPlayerDamageToVillageGolemsDuringRaid=" + VILLAGE_SECURITY_ALLOW_PLAYER_DAMAGE_TO_VILLAGE_GOLEMS_DURING_RAID
                    + ", debug=" + VILLAGE_SECURITY_DEBUG_LOGS_ENABLED + ".");
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Village security config load failed; using built-in defaults: " + throwable);
        }
    }

    private static boolean readBool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized) || "on".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        System.out.println("[Raid Enhancement Patch] Invalid boolean config value for " + key + "=" + value + "; using " + fallback + ".");
        return fallback;
    }

    private static int readInt(Properties properties, String key, int fallback, int min, int max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            System.out.println("[Raid Enhancement Patch] Invalid integer config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
    }

    private static float readFloat(Properties properties, String key, float fallback, float min, float max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            System.out.println("[Raid Enhancement Patch] Invalid float config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
    }

    private static void writeDefaultVillageSecurityConfig(Path file) throws IOException {
        List<String> lines = List.of(
                "# Raid Enhancement Patch - Village Security Config",
                "# Stage: " + VILLAGE_SECURITY_CONFIG_STAGE,
                "# File created in config/raid_enhancement_patch/ . Restart the game/server after editing.",
                "# This config controls only the village-security/performance-safe/deployment-doctrine layers. HUD, bridge, extra-wave state machine, and Raids Enhanced spawn logic remain fixed in 0.8.9.6.",
                "",
                "enabled=true",
                "tickIntervalTicks=20",
                "radius=96",
                "",
                "# Chat-only Village Defense League / 村民防卫同盟 reports.",
                "messages.enabled=true",
                "messages.initialEntry=true",
                "messages.reinforcement=true",
                "messages.repair=true",
                "messages.waveDefenseFailure=true",
                "messages.victory=true",
                "messages.failure=true",
                "messages.victoryGrade=true",
                "messages.battleSummary=true",
                "messages.timeoutPenalty=true",
                "messages.redeployment=true",
                "messages.combatBuff=true",
                "",
                "# Timeout penalty. This converts clear-time overruns into village pressure without failing vanilla raids.",
                "timeoutPenalty.enabled=true",
                "timeoutPenalty.damage=2.0",
                "timeoutPenalty.minVillagerHealth=2.0",
                "timeoutPenalty.oncePerWave=true",
                "timeoutPenalty.skipIfWaveDefenseAlreadyFailed=false",
                "timeoutPenalty.requireKnownLivingRaiders=true",
                "timeoutPenalty.includeInBattleSummary=true",
                "",
                "# Conservative performance layer. Keep enabled for large modpacks.",
                "performanceOptimization.enabled=true",
                "performanceOptimization.villagerCacheRefreshIntervalTicks=100",
                "performanceOptimization.golemGlowRefreshIntervalTicks=80",
                "# Raider-side throttles from 0.8.9.5. Higher values reduce scan pressure but delay locator/audit feedback.",
                "performanceOptimization.raiderCacheScanIntervalTicks=20",
                "performanceOptimization.stragglerGlowIntervalTicks=40",
                "performanceOptimization.raiderAuditIntervalTicks=200",
                "performanceOptimization.specialRaiderScanIntervalTicks=100",
                "performanceOptimization.timeoutPenaltyCheckIntervalTicks=20",
                "# Return/reload reconciliation: prevents unloaded security golems from being judged dead immediately after players leave and come back.",
                "performanceOptimization.securityGolemReturnGraceTicks=200",
                "performanceOptimization.securityGolemMissingPruneGraceTicks=1200",
                "performanceOptimization.activeSessionFastCheck=true",
                "performanceOptimization.cleanupEndedSessionsAggressively=true",
                "",
                "# Fewer villagers per golem means stronger village defense.",
                "securityGolems.villagersPerGolem.easy=5",
                "securityGolems.villagersPerGolem.normal=8",
                "securityGolems.villagersPerGolem.hard=10",
                "securityGolems.minPerWave=1",
                "securityGolems.maxPerWave=5",
                "securityGolems.glowDurationTicks=160",
                "securityGolems.spawnRingRadius=12",
                "securityGolems.spawnPointCount=8",
                "securityGolems.redeployExistingAtWaveStart=true",
                "securityGolems.redeployRingRadius=14",
                "securityGolems.repairSurvivorsOnWaveStart=true",
                "securityGolems.survivorRepairToFull=true",
                "securityGolems.survivorRepairHealth=10.0",
                "securityGolems.resistance.enabled=true",
                "securityGolems.resistance.level=1",
                "securityGolems.resistance.durationTicks=1200",
                "",
                "# Damage applied to every nearby living villager when one wave's security golems are wiped out.",
                "breachDamage=4.0",
                "",
                "# Ordinary unnamed iron golems restored after a victorious raid.",
                "victoryOrdinaryGolems.easy=1",
                "victoryOrdinaryGolems.normal=2",
                "victoryOrdinaryGolems.hard=3",
                "",
                "# Debug logs. Keep debug.enabled=false for normal play; enable selected sub-switches when diagnosing behavior.",
                "debug.enabled=false",
                "debug.wavePreparation=false",
                "debug.breach=false",
                "debug.completion=false",
                "debug.golemTrim=false",
                "debug.timeoutPenalty=false",
                "debug.performance=false",
                "",
                "# Strict rules from 0.8.8.9 and 0.8.9.4.",
                "# Default false means players cannot damage any village iron golem inside the active raid security radius.",
                "# Set true only if you want players to be able to attack village iron golems during raids.",
                "strictRules.allowPlayerDamageToVillageGolemsDuringRaid=false",
                "strictRules.disableOldRegenAbsorption=true",
                "strictRules.villagerHealthClampEnabled=true",
                "strictRules.healthClampDurationTicks=24000",
                "# Villagers inside this penalty radius are included when breach/timeout pressure is applied. Increase for oversized villages.",
                "strictRules.villagerPenaltyRadius=128",
                "strictRules.trimOrdinaryGolemsOnRaidStart=true",
                "strictRules.keepOrdinaryUnnamedGolems.easy=3",
                "strictRules.keepOrdinaryUnnamedGolems.normal=2",
                "strictRules.keepOrdinaryUnnamedGolems.hard=1"
        );
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }

    // Legacy preview switch kept disabled. Step 8.2 uses WAVE_TIME_BUDGET_ENABLED above.
    public static final boolean WAVE_CLEAR_TIMER_ENABLED = false;
    public static final boolean ATTACK_PREVIEW_ENABLED = false;
    public static final boolean RAIDER_HIGHLIGHT_ENABLED = true;
}
