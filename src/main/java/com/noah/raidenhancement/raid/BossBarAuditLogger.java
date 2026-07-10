package com.noah.raidenhancement.raid;

/**
 * 0.9.1.3.1 BossBar audit throttle helper.
 *
 * Diagnostic-only hotfix. It preserves the tested 0.9.1.3 BossBar module
 * boundaries and runtime behavior while reducing repeated audit output.
 */
public final class BossBarAuditLogger {
    public static final String BOUNDARY_STAGE = "0.9.1.3-bossbar-module-boundary-alpha";
    public static final String STAGE = "0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha";
    public static final String AUDIT_MODE = "diagnostic-throttle-no-gameplay-behavior-change";

    private BossBarAuditLogger() {
    }

    /** Full declaration. Emit only on BossBar creation / boundary declaration. */
    public static String commonBoundaryFields(String sourceModule) {
        return compactBoundaryFields(sourceModule)
                + " bossBarBoundaryStage=" + BOUNDARY_STAGE
                + " bossBarBehaviorUnchanged=true"
                + " bossBarProgressAlgorithmChanged=false"
                + " waveChangeChanged=false"
                + " baselineResetChanged=false"
                + " settlementKeyChanged=false"
                + " keyFormatChanged=false"
                + " villageFavorChanged=false"
                + " rewardsChanged=false"
                + " raidWavesChanged=false"
                + " serverBossEventRaidTitleMixinRestored=false"
                + " bossBarDisplayBoundary=rep-independent-create-title-visible-player-binding"
                + " bossBarCleanupBoundary=same-dimension-completed-stopped-cleanup-only"
                + " bossBarVanillaSuppressBoundary=hide-vanilla-and-victory-rebind-guard"
                + " bossBarAuditLoggerBoundary=diagnostic-only-no-state-mutation"
                + " victoryAttachGuardBehaviorChanged=false"
                + " boundaryDeclarationRepeated=false";
    }

    /** Compact fields for recurring runtime events. */
    public static String compactBoundaryFields(String sourceModule) {
        return " bossBarAuditThrottleStage=" + STAGE
                + " bossBarAuditMode=" + AUDIT_MODE
                + " sourceModule=" + safe(sourceModule)
                + " bossBarAuditThrottleActive=true"
                + " bossBarRuntimeBehaviorChanged=false"
                + " playerAuditPayload=name-and-uuid-only";
    }

    public static String boundarySummary() {
        return "BossBarDisplayManager(display-only);"
                + "BossBarCleanupController(same-dimension-cleanup);"
                + "BossBarVanillaSuppressor(vanilla-hide-and-victory-guard);"
                + "BossBarAuditLogger(throttled-diagnostic-only);"
                + "VictoryBarAttachGuard(behavior-retained)";
    }

    public static String throttlePolicySummary() {
        return "hide-vanilla:first-visible-failure-debounced-state-change-or-200tick-summary;"
                + "boundary:full-on-create-compact-after;"
                + "player:name-and-uuid-only";
    }

    private static String safe(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        return text.replace(' ', '_').replace(';', ',');
    }
}
