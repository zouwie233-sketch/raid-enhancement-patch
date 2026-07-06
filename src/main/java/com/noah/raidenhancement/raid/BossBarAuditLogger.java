package com.noah.raidenhancement.raid;

/**
 * 0.9.1.3 BossBar module boundary audit helper.
 *
 * This class is deliberately diagnostic-only. It names the desired BossBar
 * module boundaries without moving live state machines or changing the tested
 * 0.9.1.0 visual behavior. The current runtime path still lives in
 * {@link RaidIndependentBossbarManager}; this helper only adds consistent audit
 * fields so future extraction can be planned safely.
 */
public final class BossBarAuditLogger {
    public static final String STAGE = "0.9.1.3-bossbar-module-boundary-alpha";
    public static final String AUDIT_MODE = "boundary-only-no-gameplay-behavior-change";

    private BossBarAuditLogger() {
    }

    public static String commonBoundaryFields(String sourceModule) {
        return " bossBarBoundaryStage=" + STAGE
                + " bossBarAuditMode=" + AUDIT_MODE
                + " sourceModule=" + safe(sourceModule)
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
                + " victoryAttachGuardBehaviorChanged=false";
    }

    public static String boundarySummary() {
        return "BossBarDisplayManager(display-only);"
                + "BossBarCleanupController(same-dimension-cleanup);"
                + "BossBarVanillaSuppressor(vanilla-hide-and-victory-guard);"
                + "BossBarAuditLogger(diagnostic-only);"
                + "VictoryBarAttachGuard(behavior-retained)";
    }

    private static String safe(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        return text.replace(' ', '_').replace(';', ',');
    }
}
