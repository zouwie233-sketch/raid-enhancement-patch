package com.noah.raidenhancement.config;

/**
 * Read-only configuration audit marker for 0.9.1.6.
 *
 * <p>This service does not read new gameplay values, mutate existing config
 * fields, rewrite user files, delete deprecated keys, or become a runtime
 * authority. The complete source-level table is shipped as
 * CONFIG_AUDIT_0.9.1.6.csv.</p>
 */
public final class ConfigAuditService {
    public static final String STAGE = "0.9.1.6-config-audit-alpha";
    public static final int AUDITED_FIELD_COUNT = 417;
    public static final int RUNTIME_LOADED_ENTRY_COUNT = 232;
    public static final int LOADED_WITHOUT_RUNTIME_CONSUMER_COUNT = 9;
    public static final int CODE_CONSTANT_COUNT = 185;

    private ConfigAuditService() {
    }

    public static String startupMarker() {
        return "configAuditStage=" + STAGE
                + " configAuditMode=read-only-source-and-runtime-boundary-audit"
                + " configAuditReadOnly=true"
                + " auditedFieldCount=" + AUDITED_FIELD_COUNT
                + " runtimeLoadedEntryCount=" + RUNTIME_LOADED_ENTRY_COUNT
                + " loadedWithoutRuntimeConsumerCount=" + LOADED_WITHOUT_RUNTIME_CONSUMER_COUNT
                + " codeConstantCount=" + CODE_CONSTANT_COUNT
                + " configValuesChanged=false"
                + " configDefaultsChanged=false"
                + " configFilesDeleted=false"
                + " configKeysRenamed=false"
                + " configMigrationPerformed=false"
                + " runtimeConsumersChanged=false"
                + " rareGiftChanceMultiplierEffective=false"
                + " equalXpPerEligiblePlayerEffective=false"
                + " professionGiftConfigEffective=true"
                + " emeraldCapsEffective=true"
                + " giftCooldownConfigEffective=true"
                + " raidsEnhancedCompatConfigEffective=true"
                + " bossBarDiagnosticConfigEffective=true"
                + " logModeEnumImplemented=false"
                + " recommendation=defer-removal-and-behavior-fixes-to-dedicated-versions";
    }

    public static void logStartup() {
        System.out.println("[Raid Enhancement Patch][ConfigAudit] " + startupMarker());
    }
}
