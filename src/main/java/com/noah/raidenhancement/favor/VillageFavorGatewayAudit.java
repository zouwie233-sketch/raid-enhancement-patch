package com.noah.raidenhancement.favor;

/**
 * 0.9.1.5 boundary declaration for the settlement-to-VillageFavor gateway.
 *
 * <p>The gateway is a compatibility-preserving delegation layer in this
 * version. Favor rules, values, persistence and gift/cooldown behavior remain
 * owned by the existing VillageFavorSystem and VillageFavorState.</p>
 */
public final class VillageFavorGatewayAudit {
    public static final String STAGE = "0.9.1.5-village-favor-gateway-audit-alpha";
    public static final String MODE = "delegating-gateway-boundary-no-favor-rule-change";

    private VillageFavorGatewayAudit() {
    }

    public static String startupMarker() {
        return "villageFavorGatewayStage=" + STAGE
                + " villageFavorGatewayMode=" + MODE
                + " villageFavorGatewayRuntimeEntryActive=true"
                + " gatewayDelegatesToVillageFavorSystem=true"
                + " raidCompletionResultOverloadAvailable=true"
                + " raidCompletionResultConsumerMigration=false"
                + " legacyArgumentsBridgeActive=true"
                + " favorRulesChanged=false"
                + " favorValuesChanged=false"
                + " favorPersistenceChanged=false"
                + " favorRecordFormatChanged=false"
                + " giftRulesChanged=false"
                + " cooldownRulesChanged=false"
                + " villageKeyChanged=false"
                + " settlementKeyChanged=false";
    }

    public static String auditFields(String phase,
                                     String gatewayEntry,
                                     int eligiblePlayerCount,
                                     boolean completionResultInput) {
        return startupMarker()
                + " gatewayPhase=" + safe(phase)
                + " gatewayPhaseRole=" + phaseRole(phase)
                + " gatewayEntry=" + safe(gatewayEntry)
                + " gatewayEligiblePlayerCount=" + Math.max(0, eligiblePlayerCount)
                + " raidCompletionResultUsed=" + completionResultInput
                + " gatewayRuntimeBehaviorChanged=false"
                + " gatewayWriteAuthority=VillageFavorSystem"
                + " gatewayReadAuthority=VillageFavorSystem";
    }

    private static String phaseRole(String phase) {
        if ("read-favor-level-before-reward".equals(phase)) {
            return "pre-reward-favor-level-read";
        }
        if ("before-delegate-legacy".equals(phase)) {
            return "pre-village-favor-write-legacy-bridge";
        }
        if ("after-delegate-legacy".equals(phase)) {
            return "post-village-favor-write-legacy-bridge";
        }
        if ("before-delegate-completion-result".equals(phase)) {
            return "pre-village-favor-write-completion-result-overload";
        }
        if ("after-delegate-completion-result".equals(phase)) {
            return "post-village-favor-write-completion-result-overload";
        }
        if ("rejected-null-completion-result".equals(phase)) {
            return "gateway-input-rejected";
        }
        return "other";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
