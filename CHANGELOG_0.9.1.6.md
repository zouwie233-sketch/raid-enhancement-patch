# CHANGELOG 0.9.1.6

Version: `0.9.1.6-config-audit-alpha`

## Type

Read-only configuration audit version. No configuration or gameplay behavior is intentionally changed.

## Added

- `ConfigAuditService` startup marker.
- `CONFIG_AUDIT_0.9.1.6.csv` with 417 field-level audit rows.
- `CONFIG_AUDIT_SUMMARY_0.9.1.6.md` with configuration-class statistics and priority findings.
- Explicit audit flags for configuration mutation, deletion, key rename, migration and runtime-consumer changes.

## Confirmed findings

- `rareGiftChanceMultiplier` is loaded but currently has no runtime gift/reward consumer.
- `equalXpPerEligiblePlayer` is loaded but currently has no settlement consumer.
- Profession gift switches, emerald caps, gift cooldown/claim limits, Raids Enhanced compatibility settings and BossBar diagnostic controls do have runtime consumers.
- Several BattleSupport compatibility/default fields are loaded but have no external runtime consumer; they are documented as deprecation candidates only.
- `RaidEnhancementConfig` contains many hardcoded gameplay constants that are not user-editable file settings.

## Not changed

- No config key is deleted, renamed, migrated or rewritten.
- No default or loaded value is changed.
- No runtime consumer is rerouted.
- No log-mode enum is implemented yet.
- No favor, reward, gift, cooldown, persistence, key, BossBar, raid-wave or Mixin behavior is changed.
