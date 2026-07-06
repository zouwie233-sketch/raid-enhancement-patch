package com.noah.raidenhancement.raid;

import java.util.UUID;

/**
 * 0.9.1.2 key audit polish service.
 *
 * This service is intentionally read-only and string-format focused in this stage.
 * It centralizes key naming, descriptions, and audit helpers without changing the
 * already-tested gameplay behavior: settlement remains raidInstance based,
 * VillageFavor still owns long-term village records, and no persistence migration
 * is attempted here. 0.9.1.2 only makes the audit output easier to interpret.
 */
public final class RaidKeyService {
    public static final String STAGE = "0.9.1.2-key-audit-polish-alpha";
    public static final String AUDIT_MODE = "readOnly-no-gameplay-behavior-change";

    private RaidKeyService() {
    }

    public static String villageKey(String dimensionId, int centerX, int centerY, int centerZ) {
        return sanitizeDimension(dimensionId) + "@village:center:" + centerX + "," + centerY + "," + centerZ;
    }

    public static String raidInstanceCandidate(String dimensionId,
                                              String stateOrRaidKey,
                                              int sessionRaidId,
                                              long firstSeenGameTime,
                                              Object nativeRaid) {
        return sanitizeDimension(dimensionId) + "@raidInstance:candidate:" + safe(stateOrRaidKey)
                + "#sessionRaidId=" + sessionRaidId
                + "#firstSeen=" + firstSeenGameTime
                + "#nativeIdentity=" + nativeIdentity(nativeRaid);
    }

    public static String favorRecordKey(String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid) {
        return villageKey(dimensionId, centerX, centerY, centerZ) + "@player:" + (playerUuid == null ? "null" : playerUuid);
    }

    public static String settlementKeyMode(String settlementKey) {
        return safe(settlementKey).contains("@raidInstance:") ? "raidInstance" : "legacyVillageCenter";
    }

    public static String keyPurpose(String keyType) {
        String type = safe(keyType);
        if ("raidInstanceKey".equalsIgnoreCase(type) || "settlementKey".equalsIgnoreCase(type)) {
            return "single-raid-settlement-identity";
        }
        if ("villageKey".equalsIgnoreCase(type)) {
            return "long-term-village-identity";
        }
        if ("favorRecordKey".equalsIgnoreCase(type)) {
            return "player-by-village-long-term-favor-record";
        }
        if ("bossBarKey".equalsIgnoreCase(type) || "raidSessionKey".equalsIgnoreCase(type)) {
            return "active-raid-runtime-display-session";
        }
        return "unknown-key-purpose";
    }

    public static boolean hasDuplicatedDimensionToken(String key, String dimensionId) {
        String keyText = safe(key);
        String rawDimension = safe(dimensionId);
        String sanitizedDimension = sanitizeDimension(dimensionId);
        return occurrences(keyText, rawDimension) > 1 || occurrences(keyText, sanitizedDimension) > 1;
    }

    public static String dimensionDuplicationAudit(String keyType, String key, String dimensionId) {
        if (!hasDuplicatedDimensionToken(key, dimensionId)) {
            return "dimensionDuplication=false";
        }
        return "dimensionDuplication=true keyType=" + safe(keyType)
                + " warning=dimension appears duplicated; audit only, behavior unchanged";
    }

    public static String commonAuditFields(String sourceModule,
                                           String dimensionId,
                                           int centerX,
                                           int centerY,
                                           int centerZ,
                                           String raidInstanceKey,
                                           String villageKey,
                                           String settlementKey,
                                           String favorRecordKey) {
        boolean raidInstanceDup = hasDuplicatedDimensionToken(raidInstanceKey, dimensionId);
        boolean villageDup = hasDuplicatedDimensionToken(villageKey, dimensionId);
        boolean settlementDup = hasDuplicatedDimensionToken(settlementKey, dimensionId);
        boolean favorDup = hasDuplicatedDimensionToken(favorRecordKey, dimensionId);
        boolean anyDup = raidInstanceDup || villageDup || settlementDup || favorDup;
        return " keyServiceStage=" + STAGE
                + " keyAuditMode=" + AUDIT_MODE
                + " keyAuditPolish=0.9.1.2"
                + " actualKeyFormatUnchanged=true"
                + " keyFormatChange=false"
                + " sourceModule=" + safe(sourceModule)
                + " keyBoundary=raidInstanceKey:single-raid,villageKey:long-term-village,settlementKey:raidInstance,favorRecordKey:player-plus-village"
                + " raidInstancePurpose=" + keyPurpose("raidInstanceKey")
                + " villagePurpose=" + keyPurpose("villageKey")
                + " settlementPurpose=" + keyPurpose("settlementKey")
                + " favorRecordPurpose=" + keyPurpose("favorRecordKey")
                + " dimensionDuplicationAny=" + anyDup
                + " dimensionDuplicationSummary=" + dimensionDuplicationSummary(raidInstanceDup, villageDup, settlementDup, favorDup)
                + " dimensionDuplicationRecommendation=" + dimensionDuplicationRecommendation(anyDup)
                + " " + dimensionDuplicationAudit("raidInstanceKey", raidInstanceKey, dimensionId)
                + " " + dimensionDuplicationAudit("villageKey", villageKey, dimensionId)
                + " " + dimensionDuplicationAudit("settlementKey", settlementKey, dimensionId)
                + " " + dimensionDuplicationAudit("favorRecordKey", favorRecordKey, dimensionId)
                + " center=" + center(centerX, centerY, centerZ);
    }

    public static String dimensionDuplicationSummary(boolean raidInstanceDup,
                                                     boolean villageDup,
                                                     boolean settlementDup,
                                                     boolean favorDup) {
        return "raidInstanceKey:" + raidInstanceDup
                + ",villageKey:" + villageDup
                + ",settlementKey:" + settlementDup
                + ",favorRecordKey:" + favorDup;
    }

    public static String dimensionDuplicationRecommendation(boolean anyDup) {
        return anyDup
                ? "audit-only-defer-normalization-to-future-key-format-version"
                : "none";
    }

    static String sanitizeDimension(String input) {
        return safe(input).replace(':', '_').replace('/', '_').replace(' ', '_').replace('#', '_');
    }

    static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String center(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static int occurrences(String text, String needle) {
        if (text == null || needle == null || text.isBlank() || needle.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += Math.max(1, needle.length());
        }
        return count;
    }

    private static String nativeIdentity(Object nativeRaid) {
        if (nativeRaid == null) {
            return "null";
        }
        Integer id = nativeRaidNumericId(nativeRaid);
        return id == null
                ? "identity:" + System.identityHashCode(nativeRaid)
                : "id:" + id + "/identity:" + System.identityHashCode(nativeRaid);
    }

    private static Integer nativeRaidNumericId(Object nativeRaid) {
        for (String methodName : java.util.List.of("getId", "getRaidId", "id")) {
            try {
                java.lang.reflect.Method method = nativeRaid.getClass().getMethod(methodName);
                Object result = method.invoke(nativeRaid);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
