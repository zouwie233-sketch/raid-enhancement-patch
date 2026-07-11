# Step 9 — 0.9.1.6 Configuration Audit

Version: `0.9.1.6-config-audit-alpha`

## Goal

Produce an authoritative inventory of current configuration surfaces without changing runtime behavior.

## Audited configuration classes

- `RaidEnhancementConfig`
- `BattleSupportConfig`
- `VictorySettlementConfig`
- `VillageFavorConfig`
- `KeyDiagnosticsConfig`
- `RaidsEnhancedCompatConfig`

## Audit columns

The CSV records:

- configuration file;
- configuration class and field;
- user-facing key;
- default value;
- whether it is loaded at runtime;
- whether a runtime consumer exists;
- consuming modules;
- suspected legacy status;
- retain/deprecate recommendation.

## Guardrails

This version must not:

- delete or rename a key;
- change a default or loaded value;
- rewrite user config files beyond existing behavior;
- make an ineffective option effective;
- disable an existing option;
- introduce the future diagnostics mode switch;
- change rewards, VillageFavor, gifts, cooldowns, persistence, keys, BossBar, waves or Mixins.
