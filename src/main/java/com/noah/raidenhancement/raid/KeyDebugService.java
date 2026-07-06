package com.noah.raidenhancement.raid;

/**
 * 0.9.1.1 key debug facade.
 *
 * This class does not generate authoritative gameplay keys and does not decide
 * whether a raid may settle. Its only job in this stage is to make diagnostic
 * lines clearly state which key belongs to which conceptual boundary.
 */
public final class KeyDebugService {
    private KeyDebugService() {
    }

    public static String startupMarker() {
        return "keyDebugService=KeyDebugService"
                + " keyService=RaidKeyService"
                + " keyServiceStage=" + RaidKeyService.STAGE
                + " keyAuditMode=" + RaidKeyService.AUDIT_MODE
                + " note=0.9.1.1 adds read-only key boundary diagnostics only.";
    }

    public static String boundarySummary() {
        return "keyBoundarySummary="
                + "RaidInstanceKey(single-raid-settlement);"
                + "VillageKey(long-term-village-favor);"
                + "SettlementKey(raidInstance-mode-duplicate-guard);"
                + "FavorRecordKey(player-plus-village).";
    }

    public static String auditFields(String sourceModule,
                                     String dimensionId,
                                     int centerX,
                                     int centerY,
                                     int centerZ,
                                     String raidInstanceKey,
                                     String villageKey,
                                     String settlementKey,
                                     String favorRecordKey) {
        return RaidKeyService.commonAuditFields(sourceModule, dimensionId, centerX, centerY, centerZ,
                raidInstanceKey, villageKey, settlementKey, favorRecordKey)
                + " " + boundarySummary();
    }
}
